package com.learnkk.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.common.exception.AlreadyEnrolledException;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 선착순 참여 동시성 정확성 통합 테스트 — U3 신뢰성의 게이팅 조건 (reliability-design.md §1, R-U3-03/10, INV-U3-1).
 *
 * <p>Testcontainers 실 PostgreSQL + ExecutorService + CountDownLatch 로 capacity=N 코호트에 N+k 동시 join 을
 * 실행하고, CONFIRMED==N·WAITING==k·중복 0·정원 초과 0 을 단언한다. 비관적 락(SELECT ... FOR UPDATE)이 "정원 확인→확정" 구간을
 * 코호트 단위로 직렬화하므로 정원 초과 확정이 발생하지 않는다.
 */
class EnrollmentConcurrencyIntegrationTest extends IntegrationTestBase {

  @Autowired private EnrollmentService enrollmentService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private NotificationRepository notificationRepository;

  private Long mentorId;

  @BeforeEach
  void setUp() {
    // FK: enrollment.mentee_id → users RESTRICT. 참여를 먼저 지운 뒤 사용자/코호트 정리.
    enrollmentRepository.deleteAll();
    notificationRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    User mentor =
        userRepository.save(
            User.newMember("mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummyhashvalue000000"));
    mentorId = mentor.getId();
  }

  private Long openCohort(int capacity) {
    Cohort cohort =
        cohortRepository.save(
            Cohort.open(
                mentorId,
                "동시성 코호트",
                "설명",
                capacity,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 1),
                6));
    return cohort.getId();
  }

  private List<Long> createMentees(int count) {
    List<Long> ids = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      User u =
          userRepository.save(
              User.newMember(
                  "mentee" + i + "@learnkk.local",
                  "멘티" + i,
                  "m" + i,
                  "$2a$10$dummyhashvalue000000"));
      ids.add(u.getId());
    }
    return ids;
  }

  /** 동시에 join 을 실행하고 완료를 대기한다. */
  private void joinConcurrently(List<Long> menteeIds, Long cohortId) throws InterruptedException {
    int n = menteeIds.size();
    ExecutorService pool = Executors.newFixedThreadPool(Math.min(n, 32));
    CountDownLatch ready = new CountDownLatch(n);
    CountDownLatch go = new CountDownLatch(1);

    for (Long menteeId : menteeIds) {
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
              enrollmentService.join(menteeId, cohortId);
            } catch (RuntimeException ignored) {
              // 대기 등록/충돌은 정상 경로 — 상태 단언으로 검증한다.
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
    }

    ready.await();
    go.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void 정원_초과_다수_동시_join_시_정확히_capacity명만_확정() throws InterruptedException {
    int capacity = 5;
    int requests = 20;
    Long cohortId = openCohort(capacity);
    List<Long> mentees = createMentees(requests);

    joinConcurrently(mentees, cohortId);

    int confirmed =
        enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED);
    int waiting = enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.WAITING);

    assertThat(confirmed).isEqualTo(capacity); // 정원 초과 확정 0 (INV-U3-1)
    assertThat(waiting).isEqualTo(requests - capacity);
    assertThat(enrollmentRepository.count()).isEqualTo(requests); // 중복 0
  }

  @Test
  void 정원1_코호트에_다수_동시_join_시_정확히_1명만_확정() throws InterruptedException {
    int capacity = 1;
    int requests = 10;
    Long cohortId = openCohort(capacity);
    List<Long> mentees = createMentees(requests);

    joinConcurrently(mentees, cohortId);

    int confirmed =
        enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED);
    int waiting = enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.WAITING);

    assertThat(confirmed).isEqualTo(1);
    assertThat(waiting).isEqualTo(requests - 1);
    assertThat(enrollmentRepository.count()).isEqualTo(requests);
  }

  @Test
  void 동일_멘티_동시_이중제출은_UNIQUE로_1건만_성공() throws InterruptedException {
    Long cohortId = openCohort(10);
    Long menteeId = createMentees(1).get(0);

    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch go = new CountDownLatch(1);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger alreadyEnrolled = new AtomicInteger();

    for (int i = 0; i < threads; i++) {
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
              enrollmentService.join(menteeId, cohortId);
              success.incrementAndGet();
            } catch (AlreadyEnrolledException expected) {
              alreadyEnrolled.incrementAndGet();
            } catch (RuntimeException ignored) {
              // 락 경합 등 그 외 — 최종 상태 단언으로 검증한다.
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
    }

    ready.await();
    go.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

    // 정확히 1건만 저장(UNIQUE(cohort_id, mentee_id) 최종 방어선, R-U3-08).
    assertThat(enrollmentRepository.findByCohortIdAndMenteeId(cohortId, menteeId)).isPresent();
    assertThat(enrollmentRepository.count()).isEqualTo(1);
    assertThat(success.get()).isEqualTo(1);
  }
}
