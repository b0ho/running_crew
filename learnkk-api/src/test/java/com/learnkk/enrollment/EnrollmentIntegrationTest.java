package com.learnkk.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.port.ConfirmedEnrollmentQuery;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.enrollment.dto.EnrollmentDto;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.time.LocalDate;
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
 * 참여 통합 테스트 (Testcontainers 실 PostgreSQL).
 *
 * <p>마이그레이션 적용, confirmedCount/confirmedEnrollments 정확성, U2 포트 실빈 대체(EnrollmentQueryAdapter), 승인 경합
 * 조건부 UPDATE(2 관리자 동시 approve → 1건·알림 1건)를 검증한다.
 */
class EnrollmentIntegrationTest extends IntegrationTestBase {

  @Autowired private EnrollmentService enrollmentService;
  @Autowired private AdminApprovalService adminApprovalService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ConfirmedEnrollmentQuery confirmedEnrollmentQuery;

  private Long mentorId;

  @BeforeEach
  void setUp() {
    enrollmentRepository.deleteAll();
    notificationRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    mentorId =
        userRepository
            .save(
                User.newMember(
                    "mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummyhashvalue000000"))
            .getId();
  }

  private Long openCohort(int capacity) {
    return cohortRepository
        .save(
            Cohort.open(
                mentorId,
                "코호트",
                "설명",
                capacity,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 1),
                6))
        .getId();
  }

  private Long createUser(String tag) {
    return userRepository
        .save(User.newMember(tag + "@learnkk.local", tag, tag, "$2a$10$dummyhashvalue000000"))
        .getId();
  }

  @Test
  void confirmedCount와_confirmedEnrollments가_정확하다() {
    Long cohortId = openCohort(10);
    Long m1 = createUser("mc1");
    Long m2 = createUser("mc2");
    Long m3 = createUser("mc3");
    enrollmentRepository.save(Enrollment.confirmed(cohortId, m1));
    enrollmentRepository.save(Enrollment.confirmed(cohortId, m2));
    enrollmentRepository.save(Enrollment.waiting(cohortId, m3));

    assertThat(enrollmentService.confirmedCount(cohortId)).isEqualTo(2);

    List<EnrollmentDto> confirmed = enrollmentService.confirmedEnrollments(cohortId);
    assertThat(confirmed).hasSize(2);
    assertThat(confirmed).allMatch(e -> e.status() == EnrollmentStatus.CONFIRMED);
    assertThat(confirmed).allMatch(e -> "코호트".equals(e.cohortTitle()));
  }

  @Test
  void U2_포트가_실제_확정인원으로_동작한다() {
    // EnrollmentQueryAdapter(@Component)가 U2 기본 빈(0 반환)을 대체해야 한다.
    assertThat(confirmedEnrollmentQuery).isInstanceOf(EnrollmentQueryAdapter.class);

    Long cohortId = openCohort(10);
    enrollmentRepository.save(Enrollment.confirmed(cohortId, createUser("mp1")));
    enrollmentRepository.save(Enrollment.confirmed(cohortId, createUser("mp2")));

    assertThat(confirmedEnrollmentQuery.confirmedCount(cohortId)).isEqualTo(2);
  }

  @Test
  void join으로_확정되면_확정알림이_생성된다() {
    Long cohortId = openCohort(10);
    Long menteeId = createUser("mj1");

    enrollmentService.join(menteeId, cohortId);

    assertThat(notificationRepository.countByUserIdAndReadFalse(menteeId)).isEqualTo(1);
    assertThat(enrollmentRepository.findByCohortIdAndMenteeId(cohortId, menteeId))
        .get()
        .extracting(Enrollment::getStatus)
        .isEqualTo(EnrollmentStatus.CONFIRMED);
  }

  @Test
  void 두_관리자_동시_승인은_조건부UPDATE로_1건만_성공하고_알림도_1건() throws InterruptedException {
    Long cohortId = openCohort(1);
    Long menteeId = createUser("mw1");
    Long adminId = createUser("admin1");
    Long enrollmentId = enrollmentRepository.save(Enrollment.waiting(cohortId, menteeId)).getId();

    int threads = 6;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch go = new CountDownLatch(1);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger conflict = new AtomicInteger();

    for (int i = 0; i < threads; i++) {
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
              adminApprovalService.approve(adminId, enrollmentId);
              success.incrementAndGet();
            } catch (InvalidStateTransitionException expected) {
              conflict.incrementAndGet();
            } catch (RuntimeException ignored) {
              // 그 외 경합 예외 — 최종 상태 단언으로 검증한다.
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
    }

    ready.await();
    go.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

    assertThat(success.get()).isEqualTo(1); // 정확히 1건만 승인
    assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
        .isEqualTo(EnrollmentStatus.CONFIRMED);
    // 알림 중복 없음 — 확정 알림 1건만.
    assertThat(notificationRepository.countByUserIdAndReadFalse(menteeId)).isEqualTo(1);
  }
}
