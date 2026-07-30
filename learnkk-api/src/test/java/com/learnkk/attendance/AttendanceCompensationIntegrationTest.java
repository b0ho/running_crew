package com.learnkk.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortService;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.cohort.SessionStatus;
import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 원자성 보상 통합 테스트 (Testcontainers) — 트랜잭션 롤백 시 파일 보상 삭제(R-U4-13, INV-U4-1).
 *
 * <p>{@link AttendanceEvidenceWriter} 를 예외를 던지도록 목킹해 DB 트랜잭션이 실패하는 상황을 재현한다. {@code
 * AttendanceService.uploadEvidence} 는 저장한 파일을 보상 삭제하고, 회차는 인증되지 않아야 한다(저장소에 고아 파일 없음).
 */
class AttendanceCompensationIntegrationTest extends IntegrationTestBase {

  private static final Path UPLOAD_DIR =
      Path.of(System.getProperty("java.io.tmpdir"), "learnkk-u4-compensation-test");

  @Autowired private AttendanceService attendanceService;
  @Autowired private AttendanceEvidenceRepository evidenceRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private CohortService cohortService;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private UserRepository userRepository;

  // 트랜잭션 실패를 재현하기 위해 원자적 writer 를 목킹한다.
  @MockBean private AttendanceEvidenceWriter evidenceWriter;

  private Long mentorId;
  private Long session1;

  @DynamicPropertySource
  static void uploadDir(DynamicPropertyRegistry registry) {
    registry.add("app.storage.upload-dir", UPLOAD_DIR::toString);
  }

  private static MockMultipartFile jpeg() {
    byte[] bytes = new byte[64];
    bytes[0] = (byte) 0xFF;
    bytes[1] = (byte) 0xD8;
    bytes[2] = (byte) 0xFF;
    return new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes);
  }

  @BeforeEach
  void setUp() throws IOException {
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
    evidenceRepository.deleteAll();
    enrollmentRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    User mentor =
        userRepository.save(
            User.newMember("mentor@learnkk.local", "멘토", "mentor", "$2a$10$dummleyhashvalue00"));
    mentorId = mentor.getId();
    CohortDto cohort =
        cohortService.create(
            mentorId,
            new CohortCreateRequest(
                "보상 통합", "설명", 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 2));
    session1 = sessionRepository.findByCohortIdOrderBySeqAsc(cohort.id()).get(0).getId();
  }

  private long storedFileCount() throws IOException {
    if (!Files.exists(UPLOAD_DIR)) {
      return 0;
    }
    try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
      return files.filter(Files::isRegularFile).count();
    }
  }

  @Test
  void 트랜잭션_롤백시_저장된_파일을_보상_삭제하고_회차는_인증되지_않는다() throws IOException {
    doThrow(new RuntimeException("DB 롤백 재현"))
        .when(evidenceWriter)
        .persistAndVerify(anyLong(), anyString(), anyString(), anyLong(), anyLong());

    assertThatThrownBy(() -> attendanceService.uploadEvidence(mentorId, session1, jpeg()))
        .isInstanceOf(IllegalStateException.class);

    // 저장했던 파일이 보상 삭제되어 저장소에 고아 파일이 없다.
    assertThat(storedFileCount()).isZero();
    // 회차는 인증되지 않았고 증빙 이력도 없다(INV-U4-1 일관).
    assertThat(sessionRepository.findById(session1).orElseThrow().getStatus())
        .isEqualTo(SessionStatus.SCHEDULED);
    assertThat(evidenceRepository.countBySessionId(session1)).isZero();
  }
}
