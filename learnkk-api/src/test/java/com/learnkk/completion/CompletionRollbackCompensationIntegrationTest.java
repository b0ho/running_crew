package com.learnkk.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortService;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.cohort.SessionService;
import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.enrollment.EnrollmentStatus;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 종료 트랜잭션 롤백 시 N건 수료증 이미지 보상 통합 테스트 (Testcontainers, R-U5-08a, INV-U5-3).
 *
 * <p>{@link CompletionWriter} 를 예외를 던지도록 목킹해 종료 트랜잭션 실패를 재현한다. {@code CompletionService.endCohort}
 * 는 이미 저장(비트랜잭션)한 N개 수료증 이미지를 모두 보상 삭제하고, 상태 전이·수료증·정산은 커밋되지 않아야 한다(원자성).
 */
class CompletionRollbackCompensationIntegrationTest extends IntegrationTestBase {

  private static final Path UPLOAD_DIR =
      Path.of(System.getProperty("java.io.tmpdir"), "learnkk-u5-rollback-test");

  @Autowired private CompletionService completionService;
  @Autowired private CohortService cohortService;
  @Autowired private SessionService sessionService;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CertificateRepository certificateRepository;
  @Autowired private SettlementStatusRepository settlementStatusRepository;
  @Autowired private UserRepository userRepository;

  // 종료 트랜잭션 실패를 재현하기 위해 원자적 writer 를 목킹한다.
  @MockBean private CompletionWriter completionWriter;

  private Long mentorId;
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
    enrollmentRepository.deleteAll();
    sessionRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    User mentor =
        userRepository.save(
            User.newMember("mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummyhashvalue000000"));
    mentorId = mentor.getId();

    CohortDto cohort =
        cohortService.create(
            mentorId,
            new CohortCreateRequest(
                "롤백 코호트",
                "설명",
                20,
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 3, 1),
                5));
    cohortId = cohort.id();
    cohortService.start(mentorId, cohortId);

    // 확정 멘티 3명.
    for (int i = 1; i <= 3; i++) {
      User mentee =
          userRepository.save(
              User.newMember(
                  "mentee" + i + "@learnkk.local",
                  "멘티" + i,
                  "mentee" + i,
                  "$2a$10$dummyhashvalue000000"));
      Enrollment e = Enrollment.confirmed(cohortId, mentee.getId());
      ReflectionTestUtils.setField(e, "status", EnrollmentStatus.CONFIRMED);
      enrollmentRepository.save(e);
    }

    // 전 회차 인증 → 수료 판정 통과 → 이미지 3장 생성/저장.
    sessionRepository
        .findByCohortIdOrderBySeqAsc(cohortId)
        .forEach(s -> sessionService.markVerified(s.getId()));
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
  void 종료_트랜잭션_롤백시_N개_수료증_이미지를_모두_보상삭제하고_상태는_진행중으로_남는다() throws IOException {
    doThrow(new RuntimeException("DB 롤백 재현"))
        .when(completionWriter)
        .finalizeEnd(
            anyLong(), anyLong(), anyString(), anyBoolean(), anyList(), anyBoolean(), anyList());

    assertThatThrownBy(() -> completionService.endCohort(mentorId, cohortId))
        .isInstanceOf(IllegalStateException.class);

    // 저장했던 3개 이미지가 모두 보상 삭제되어 고아 파일이 없다(R-U5-08a).
    assertThat(storedFileCount()).isZero();
    // 상태 전이/수료증/정산은 커밋되지 않았다(원자성).
    assertThat(cohortRepository.findById(cohortId).orElseThrow().getStatus())
        .isEqualTo(CohortStatus.ONGOING);
    assertThat(certificateRepository.countByCohortId(cohortId)).isZero();
    assertThat(settlementStatusRepository.findByCohortId(cohortId)).isEmpty();
  }
}
