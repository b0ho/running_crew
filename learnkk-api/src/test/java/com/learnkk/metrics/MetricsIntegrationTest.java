package com.learnkk.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.completion.Certificate;
import com.learnkk.completion.CertificateRepository;
import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.metrics.dto.MetricsOverviewDto;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 운영 지표 통합 테스트 (Testcontainers) — 실 PostgreSQL 집계가 실제 데이터와 일치하고(FR-11), CLOSED 범위가 일관되며(INV-U6-4)
 * 진행중 코호트가 제외되는지 검증한다.
 *
 * <p>시나리오: 종료됨 코호트 2건(A: 4/5 인증·확정 3·증서 3, B: 5/5 인증·확정 2·증서 2)과 진행중 코호트 1건(C: 5/5 인증·확정 3, 제외 대상)을
 * 세팅한다. 기대값 — 완주 2건, 출석률 9/10=90.0%, 수료율 5/5=100.0%, 증서 5장. 진행중 코호트가 포함되면 값이 달라지므로 제외 여부가 검증된다.
 */
class MetricsIntegrationTest extends IntegrationTestBase {

  @Autowired private MetricsService metricsService;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CertificateRepository certificateRepository;
  @Autowired private UserRepository userRepository;

  private Long mentorId;

  @BeforeEach
  void setUp() {
    certificateRepository.deleteAll();
    enrollmentRepository.deleteAll();
    sessionRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    mentorId =
        userRepository
            .save(
                User.newMember(
                    "mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummyhash0000000000"))
            .getId();

    // 종료됨 코호트 A: 5회차 중 4회차 인증, 확정 멘티 3명, 증서 3장.
    Long cohortA = createCohort(CohortStatus.CLOSED, 5);
    verifySessions(cohortA, 5, 4);
    confirmMentees(cohortA, 3);
    issueCertificates(cohortA, 3);

    // 종료됨 코호트 B: 5회차 모두 인증, 확정 멘티 2명, 증서 2장.
    Long cohortB = createCohort(CohortStatus.CLOSED, 5);
    verifySessions(cohortB, 5, 5);
    confirmMentees(cohortB, 2);
    issueCertificates(cohortB, 2);

    // 진행중 코호트 C: 집계 제외 대상(INV-U6-4). 5/5 인증·확정 3명이지만 지표에 포함되면 안 된다.
    Long cohortC = createCohort(CohortStatus.ONGOING, 5);
    verifySessions(cohortC, 5, 5);
    confirmMentees(cohortC, 3);
  }

  @Test
  void overview_실데이터와_일치하고_진행중_코호트는_제외된다() {
    MetricsOverviewDto dto = metricsService.overview();

    // 완주 코스 수 = 종료됨 코호트 2건(진행중 C 제외).
    assertThat(dto.completedCohortCount()).isEqualTo(2);
    // 출석률 = (4 + 5) 인증 / (5 + 5) 전체 = 90.0% (진행중 C 의 5/5 제외).
    assertThat(dto.attendanceRate()).isEqualTo(90.0);
    // 수료율 = 증서 5 / 종료됨 확정 멘티 5 = 100.0% (진행중 C 의 확정 3명 제외).
    assertThat(dto.completionRate()).isEqualTo(100.0);
    // 발급 증서 수 = 전체 증서 5장.
    assertThat(dto.certificateCount()).isEqualTo(5L);
    assertThat(dto.scopeLabel()).isEqualTo("종료된 코호트 2건 기준");
  }

  @Test
  void overview_종료된_코호트가_없으면_모든_지표가_0이고_0으로_안전처리() {
    // 참조 무결성을 지키도록 자식→부모 순으로 전부 제거한다.
    certificateRepository.deleteAll();
    enrollmentRepository.deleteAll();
    sessionRepository.deleteAll();
    cohortRepository.deleteAll();

    MetricsOverviewDto dto = metricsService.overview();

    assertThat(dto.completedCohortCount()).isZero();
    assertThat(dto.attendanceRate()).isEqualTo(0.0);
    assertThat(dto.completionRate()).isEqualTo(0.0);
    assertThat(dto.certificateCount()).isZero();
    assertThat(dto.scopeLabel()).isEqualTo("종료된 코호트 0건 기준");
  }

  // ---- 시드 헬퍼 ----

  private Long createCohort(CohortStatus status, int sessionCount) {
    Cohort c =
        Cohort.open(
            mentorId,
            "코호트",
            "설명",
            20,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 1),
            sessionCount);
    ReflectionTestUtils.setField(c, "status", status);
    return cohortRepository.save(c).getId();
  }

  private void verifySessions(Long cohortId, int total, int verified) {
    for (int seq = 1; seq <= total; seq++) {
      Session s = Session.scheduled(cohortId, seq);
      if (seq <= verified) {
        s.markVerified();
      }
      sessionRepository.save(s);
    }
  }

  private void confirmMentees(Long cohortId, int count) {
    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      User mentee =
          userRepository.save(
              User.newMember(
                  "mentee-" + cohortId + "-" + i + "@learnkk.local",
                  "멘티" + i,
                  "mentee-" + cohortId + "-" + i,
                  "$2a$10$dummyhash0000000000"));
      ids.add(mentee.getId());
    }
    for (Long menteeId : ids) {
      enrollmentRepository.save(Enrollment.confirmed(cohortId, menteeId));
    }
  }

  private void issueCertificates(Long cohortId, int count) {
    for (int i = 0; i < count; i++) {
      User mentee =
          userRepository.save(
              User.newMember(
                  "cert-" + cohortId + "-" + i + "@learnkk.local",
                  "수료자" + i,
                  "cert-" + cohortId + "-" + i,
                  "$2a$10$dummyhash0000000000"));
      certificateRepository.save(
          Certificate.issue(cohortId, mentee.getId(), "cert-" + cohortId + "-" + i + ".png"));
    }
  }
}
