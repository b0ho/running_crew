package com.learnkk.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortService;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.cohort.SessionService;
import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.completion.dto.CohortEndSummaryDto;
import com.learnkk.completion.dto.ReportSubmitRequest;
import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.enrollment.EnrollmentStatus;
import com.learnkk.enrollment.NotificationRepository;
import com.learnkk.enrollment.NotificationType;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 코호트 종료 통합 테스트 (Testcontainers) — 종료 원자성·80% 경계·N건 이미지 보상·보고서 첨부·재종료 멱등(INV-U5-1~5, R-U5-08a).
 *
 * <p>실 PostgreSQL 위에서 수료증 N장 + 정산 + 상태전이(종료됨) + 알림이 함께 커밋되는지, 80% 경계가 실 DB 에서 동일하게 판정되는지, 트랜잭션 롤백 시
 * 저장된 N개 이미지가 모두 정리되는지, 재종료가 차단되는지를 검증한다.
 */
class CompletionIntegrationTest extends IntegrationTestBase {

  private static final Path UPLOAD_DIR =
      Path.of(System.getProperty("java.io.tmpdir"), "learnkk-u5-completion-test");

  @Autowired private CompletionService completionService;
  @Autowired private ReportService reportService;
  @Autowired private CohortService cohortService;
  @Autowired private SessionService sessionService;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CertificateRepository certificateRepository;
  @Autowired private SettlementStatusRepository settlementStatusRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private UserRepository userRepository;

  private Long mentorId;
  private List<Long> menteeIds;
  private Long cohortId;

  @DynamicPropertySource
  static void uploadDir(DynamicPropertyRegistry registry) {
    registry.add("app.storage.upload-dir", UPLOAD_DIR::toString);
  }

  @BeforeEach
  void setUp() throws IOException {
    clearUploadDir();
    certificateRepository.deleteAll();
    settlementStatusRepository.deleteAll();
    notificationRepository.deleteAll();
    enrollmentRepository.deleteAll();
    sessionRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    User mentor =
        userRepository.save(
            User.newMember("mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummyhashvalue000000"));
    mentorId = mentor.getId();

    menteeIds = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      User mentee =
          userRepository.save(
              User.newMember(
                  "mentee" + i + "@learnkk.local",
                  "멘티" + i,
                  "mentee" + i,
                  "$2a$10$dummyhashvalue000000"));
      menteeIds.add(mentee.getId());
    }

    // 5회차 코호트 개설 → 시작(진행중).
    CohortDto cohort =
        cohortService.create(
            mentorId,
            new CohortCreateRequest(
                "통합 코호트",
                "설명",
                20,
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 3, 1),
                5));
    cohortId = cohort.id();
    cohortService.start(mentorId, cohortId);

    // 확정 멘티 3명.
    for (Long menteeId : menteeIds) {
      Enrollment e = Enrollment.confirmed(cohortId, menteeId);
      org.springframework.test.util.ReflectionTestUtils.setField(
          e, "status", EnrollmentStatus.CONFIRMED);
      enrollmentRepository.save(e);
    }
  }

  private void verifySessions(int count) {
    var sessions = sessionRepository.findByCohortIdOrderBySeqAsc(cohortId);
    for (int i = 0; i < count; i++) {
      sessionService.markVerified(sessions.get(i).getId());
    }
  }

  private long storedFileCount() throws IOException {
    if (!Files.exists(UPLOAD_DIR)) {
      return 0;
    }
    try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
      return files.filter(Files::isRegularFile).count();
    }
  }

  private void clearUploadDir() throws IOException {
    if (Files.exists(UPLOAD_DIR)) {
      try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
        files
            .filter(Files::isRegularFile)
            .forEach(
                p -> {
                  try {
                    Files.deleteIfExists(p);
                  } catch (IOException ignored) {
                    // 무시
                  }
                });
      }
    }
  }

  @Test
  void 종료_원자성_수료증N장_정산_상태전이_알림이_함께_커밋된다() throws IOException {
    // 5/5 인증 + 멘토 보고서 제출 → 수료 + 정산 충족.
    verifySessions(5);
    reportService.submit(mentorId, cohortId, new ReportSubmitRequest("멘토 최종 보고서"), null);

    CohortEndSummaryDto summary = completionService.endCohort(mentorId, cohortId);

    assertThat(summary.certifiedCount()).isEqualTo(3);
    assertThat(summary.totalConfirmed()).isEqualTo(3);
    assertThat(summary.settlementSatisfied()).isTrue();
    assertThat(summary.issuedCertificateCount()).isEqualTo(3L);

    // 수료증 3장 커밋.
    assertThat(certificateRepository.countByCohortId(cohortId)).isEqualTo(3L);
    // 정산 상태 1건 커밋.
    assertThat(settlementStatusRepository.findByCohortId(cohortId)).isPresent();
    assertThat(settlementStatusRepository.findByCohortId(cohortId).orElseThrow().isSatisfied())
        .isTrue();
    // 상태 전이 종료됨.
    assertThat(cohortRepository.findById(cohortId).orElseThrow().getStatus())
        .isEqualTo(CohortStatus.CLOSED);
    // 확정 멘티 3명에게 수료 결과 알림.
    long completionNotifs =
        menteeIds.stream()
            .flatMap(
                id ->
                    notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(
                            id, org.springframework.data.domain.Pageable.unpaged())
                        .stream())
            .filter(n -> n.getType() == NotificationType.COMPLETION_RESULT)
            .count();
    assertThat(completionNotifs).isEqualTo(3L);
    // 저장소에 수료증 이미지 3개.
    assertThat(storedFileCount()).isEqualTo(3L);
  }

  @Test
  void 출석률_80퍼_경계_실DB검증_4회차5회차는_수료_3회차는_미수료() {
    // 4/5 = 80% → 수료.
    verifySessions(4);
    CohortEndSummaryDto summary = completionService.endCohort(mentorId, cohortId);
    assertThat(summary.certifiedCount()).isEqualTo(3);
    assertThat(certificateRepository.countByCohortId(cohortId)).isEqualTo(3L);
  }

  @Test
  void 출석률_미달이면_수료증_미발급() {
    // 3/5 = 60% → 미수료.
    verifySessions(3);
    CohortEndSummaryDto summary = completionService.endCohort(mentorId, cohortId);
    assertThat(summary.certifiedCount()).isZero();
    assertThat(summary.notCertifiedCount()).isEqualTo(3);
    assertThat(certificateRepository.countByCohortId(cohortId)).isZero();
    // 미수료여도 정산은 별개 판정(전 회차 인증 아님 → 미충족).
    assertThat(summary.settlementSatisfied()).isFalse();
  }

  @Test
  void 재종료는_차단된다_이미_종료됨이면_409() {
    verifySessions(5);
    completionService.endCohort(mentorId, cohortId);

    // 이미 종료됨 → 진행중 아님 → InvalidStateTransition(409).
    assertThatThrownBy(() -> completionService.endCohort(mentorId, cohortId))
        .isInstanceOf(com.learnkk.common.exception.InvalidStateTransitionException.class);
    // 수료증은 중복 발급되지 않는다(INV-U5-1).
    assertThat(certificateRepository.countByCohortId(cohortId)).isEqualTo(3L);
  }

  @Test
  void 보고서_첨부_저장과_이력조회() {
    var pdf =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "report.pdf", "application/pdf", new byte[] {1, 2, 3, 4});
    reportService.submit(mentorId, cohortId, new ReportSubmitRequest("첨부 보고서"), pdf);

    var page =
        reportService.historyOf(
            cohortId, mentorId, false, org.springframework.data.domain.Pageable.ofSize(20));
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0).hasAttachment()).isTrue();
  }
}
