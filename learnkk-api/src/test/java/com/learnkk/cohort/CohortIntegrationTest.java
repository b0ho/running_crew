package com.learnkk.cohort;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDetailDto;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.cohort.dto.CohortSummaryDto;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 코호트 통합 테스트 (Testcontainers 실 PostgreSQL).
 *
 * <p>마이그레이션 적용·개설·목록 페이지네이션·상세 N+1 회귀 방지(쿼리 카운트 단언)·상태 전이 가드 UPDATE 동시성을 검증한다.
 */
class CohortIntegrationTest extends IntegrationTestBase {

  @Autowired private CohortService cohortService;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private Long mentorId;

  @DynamicPropertySource
  static void statisticsProperty(DynamicPropertyRegistry registry) {
    // N+1 회귀 방지 테스트를 위해 Hibernate 통계 활성화
    registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
  }

  @BeforeEach
  void setUp() {
    cohortRepository.deleteAll();
    User mentor =
        userRepository.save(
            User.newMember("mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummleyhashvalue00"));
    mentorId = mentor.getId();
  }

  private CohortCreateRequest req(String title, int sessionCount) {
    return new CohortCreateRequest(
        title, "설명", 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), sessionCount);
  }

  @Test
  void 개설시_회차_N건이_생성된다() {
    CohortDto dto = cohortService.create(mentorId, req("자바 멘토링", 6));

    assertThat(dto.status()).isEqualTo(CohortStatus.RECRUITING);
    assertThat(sessionRepository.findByCohortIdOrderBySeqAsc(dto.id())).hasSize(6);
  }

  @Test
  void 목록은_페이지네이션으로_반환된다() {
    for (int i = 0; i < 25; i++) {
      cohortService.create(mentorId, req("코호트 " + i, 3));
    }
    Page<CohortSummaryDto> page =
        cohortService.list(
            null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

    assertThat(page.getTotalElements()).isEqualTo(25);
    assertThat(page.getContent()).hasSize(20);
  }

  @Test
  void 상세조회는_회차수와_무관하게_쿼리수가_상한된다() {
    // 회차 12건 코호트 — 상세 조회 시 회차별 개별 쿼리(N+1)가 발생하면 안 된다.
    CohortDto created = cohortService.create(mentorId, req("N+1 검증", 12));

    Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    stats.clear();

    CohortDetailDto detail = cohortService.get(created.id(), mentorId, false);

    assertThat(detail.sessions()).hasSize(12);
    // 코호트 1 + 회차 목록 1 + 최근 공지 1 = 3 쿼리. 여유를 두어 상한 4 로 단언(N 에 비례하지 않음).
    assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(4);
  }

  @Test
  void start_두번째_전이는_INVALID_STATE_TRANSITION() {
    CohortDto created = cohortService.create(mentorId, req("전이 검증", 3));
    cohortService.start(mentorId, created.id());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> cohortService.start(mentorId, created.id()))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void 동시_start_는_가드_UPDATE_로_한번만_성공한다() throws InterruptedException {
    CohortDto created = cohortService.create(mentorId, req("동시성 검증", 3));

    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch go = new CountDownLatch(1);
    AtomicInteger success = new AtomicInteger();

    for (int i = 0; i < threads; i++) {
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
              cohortService.start(mentorId, created.id());
              success.incrementAndGet();
            } catch (InvalidStateTransitionException expected) {
              // 경쟁에서 진 스레드 — 정상
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
    }

    ready.await();
    go.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(success.get()).isEqualTo(1);
    assertThat(cohortRepository.findById(created.id()).orElseThrow().getStatus())
        .isEqualTo(CohortStatus.ONGOING);
  }
}
