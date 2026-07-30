package com.learnkk.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learnkk.attendance.dto.CohortAttendanceDto;
import com.learnkk.attendance.dto.EvidenceDto;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortService;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.cohort.SessionStatus;
import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 출석 증빙 통합 테스트 (Testcontainers 실 PostgreSQL).
 *
 * <p>업로드 → attendance_evidence 저장 + 회차 VERIFIED 원자성, 재업로드 이력 누적·인증 유지, 진도율, 스트리밍 다운로드, 저장소 고아 파일
 * 없음, 참여자 인가를 검증한다(INV-U4-1, R-U4-05/06/09/10/11).
 */
class AttendanceIntegrationTest extends IntegrationTestBase {

  private static final Path UPLOAD_DIR =
      Path.of(System.getProperty("java.io.tmpdir"), "learnkk-u4-upload-test");

  @Autowired private AttendanceService attendanceService;
  @Autowired private AttendanceEvidenceRepository evidenceRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private CohortService cohortService;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private UserRepository userRepository;

  private Long mentorId;
  private Long cohortId;
  private List<Long> sessionIds;

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

  private static MockMultipartFile pdf() {
    byte[] bytes = new byte[64];
    bytes[0] = 0x25;
    bytes[1] = 0x50;
    bytes[2] = 0x44;
    bytes[3] = 0x46;
    return new MockMultipartFile("file", "b.pdf", "application/pdf", bytes);
  }

  private long storedFileCount() throws IOException {
    if (!Files.exists(UPLOAD_DIR)) {
      return 0;
    }
    try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
      return files.filter(Files::isRegularFile).count();
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    // 저장소 정리(이전 테스트 잔여 파일 제거)
    if (Files.exists(UPLOAD_DIR)) {
      try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
        files.filter(Files::isRegularFile).forEach(this::deleteQuietly);
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
                "출석 통합", "설명", 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 4));
    cohortId = cohort.id();
    sessionIds =
        sessionRepository.findByCohortIdOrderBySeqAsc(cohortId).stream()
            .map(com.learnkk.cohort.Session::getId)
            .toList();
  }

  private void deleteQuietly(Path p) {
    try {
      Files.deleteIfExists(p);
    } catch (IOException ignored) {
      // 정리 실패는 테스트 결과에 영향 없음
    }
  }

  @Test
  void 업로드시_증빙_저장과_회차_인증이_원자적으로_커밋된다() {
    Long session1 = sessionIds.get(0);

    EvidenceDto dto = attendanceService.uploadEvidence(mentorId, session1, jpeg());

    assertThat(dto.id()).isNotNull();
    assertThat(dto.sessionId()).isEqualTo(session1);
    assertThat(evidenceRepository.countBySessionId(session1)).isEqualTo(1);
    assertThat(sessionRepository.findById(session1).orElseThrow().getStatus())
        .isEqualTo(SessionStatus.VERIFIED);
  }

  @Test
  void 재업로드시_이력이_누적되고_회차는_인증_상태를_유지한다() {
    Long session1 = sessionIds.get(0);
    attendanceService.uploadEvidence(mentorId, session1, jpeg());
    attendanceService.uploadEvidence(mentorId, session1, pdf());

    assertThat(evidenceRepository.countBySessionId(session1)).isEqualTo(2);
    assertThat(sessionRepository.findById(session1).orElseThrow().getStatus())
        .isEqualTo(SessionStatus.VERIFIED);
  }

  @Test
  void 진도율은_인증_회차수_나누기_전체_회차수다() {
    attendanceService.uploadEvidence(mentorId, sessionIds.get(0), jpeg());

    CohortAttendanceDto dto = attendanceService.sessionsOf(cohortId, mentorId, false);

    assertThat(dto.totalCount()).isEqualTo(4);
    assertThat(dto.verifiedCount()).isEqualTo(1);
    assertThat(dto.progressRate()).isEqualTo(0.25);
    assertThat(dto.sessions().get(0).hasEvidence()).isTrue();
    assertThat(dto.sessions().get(0).latestEvidenceId()).isNotNull();
  }

  @Test
  void 다운로드는_스트리밍_리소스를_반환한다() throws IOException {
    Long session1 = sessionIds.get(0);
    EvidenceDto dto = attendanceService.uploadEvidence(mentorId, session1, jpeg());

    EvidenceDownload download =
        attendanceService.downloadEvidence(session1, dto.id(), mentorId, false);

    assertThat(download.resource().exists()).isTrue();
    assertThat(download.mimeType()).isEqualTo("image/jpeg");
    assertThat(download.filename()).isEqualTo("evidence-" + dto.id() + ".jpg");
    assertThat(download.resource().contentLength()).isGreaterThan(0);
  }

  @Test
  void 업로드_성공시_저장소에_고아_파일이_남지_않는다() throws IOException {
    attendanceService.uploadEvidence(mentorId, sessionIds.get(0), jpeg());
    attendanceService.uploadEvidence(mentorId, sessionIds.get(1), pdf());

    // 성공 업로드 2건 → 저장 파일 정확히 2개(임시파일/고아 없음)
    assertThat(storedFileCount()).isEqualTo(2);
  }

  @Test
  void 확정_멘티는_진도를_조회할_수_있고_비참여자는_403이다() {
    User mentee =
        userRepository.save(
            User.newMember("mentee@learnkk.local", "멘티", "mentee", "$2a$10$dummleyhashvalue00"));
    enrollmentRepository.save(Enrollment.confirmed(cohortId, mentee.getId()));

    CohortAttendanceDto dto = attendanceService.sessionsOf(cohortId, mentee.getId(), false);
    assertThat(dto.totalCount()).isEqualTo(4);

    User outsider =
        userRepository.save(
            User.newMember("out@learnkk.local", "외부", "out", "$2a$10$dummleyhashvalue00"));
    assertThatThrownBy(() -> attendanceService.sessionsOf(cohortId, outsider.getId(), false))
        .isInstanceOf(AccessDeniedException.class);
  }
}
