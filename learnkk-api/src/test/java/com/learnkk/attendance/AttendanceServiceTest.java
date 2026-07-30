package com.learnkk.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.attendance.dto.CohortAttendanceDto;
import com.learnkk.attendance.dto.EvidenceDto;
import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.cohort.SessionStatus;
import com.learnkk.common.exception.CohortClosedException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.FileConstraintViolationException;
import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.enrollment.EnrollmentStatus;
import com.learnkk.file.FileStorageService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * AttendanceService 단위 테스트 (Mockito) — 사전검증·원자성 보상·진도/권한(business-logic-model §2~4).
 *
 * <p>업로드 사전검증(404·403·409·400), 성공 시 writer 위임, 트랜잭션 롤백 시 파일 보상 삭제, 진도율 계산·참여자 인가를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

  @Mock private FileSignatureValidator fileSignatureValidator;
  @Mock private FileStorageService fileStorageService;
  @Mock private AttendanceEvidenceWriter evidenceWriter;
  @Mock private AttendanceEvidenceRepository evidenceRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private CohortRepository cohortRepository;
  @Mock private EnrollmentRepository enrollmentRepository;

  @InjectMocks private AttendanceService attendanceService;

  private static final Long MENTOR_ID = 10L;
  private static final Long SESSION_ID = 5L;
  private static final Long COHORT_ID = 1L;

  private Session session(Long id, Long cohortId, int seq, SessionStatus status) {
    Session s = Session.scheduled(cohortId, seq);
    ReflectionTestUtils.setField(s, "id", id);
    ReflectionTestUtils.setField(s, "status", status);
    return s;
  }

  private Cohort cohort(Long id, Long mentorId, CohortStatus status) {
    Cohort c =
        Cohort.open(
            mentorId, "코호트", null, 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 3);
    ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "status", status);
    return c;
  }

  private AttendanceEvidence evidence(Long id, Long sessionId) {
    AttendanceEvidence e =
        AttendanceEvidence.of(sessionId, "stored.jpg", "image/jpeg", 100L, MENTOR_ID);
    ReflectionTestUtils.setField(e, "id", id);
    ReflectionTestUtils.setField(e, "createdAt", Instant.now());
    return e;
  }

  private MockMultipartFile jpegFile() {
    return new MockMultipartFile(
        "file", "a.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
  }

  @Test
  void upload_회차_미존재면_404() {
    when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile()))
        .isInstanceOf(EntityNotFoundException.class);
    verify(fileStorageService, never()).store(any());
  }

  @Test
  void upload_코호트_미존재면_404() {
    when(sessionRepository.findById(SESSION_ID))
        .thenReturn(Optional.of(session(SESSION_ID, COHORT_ID, 1, SessionStatus.SCHEDULED)));
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile()))
        .isInstanceOf(EntityNotFoundException.class);
    verify(fileStorageService, never()).store(any());
  }

  @Test
  void upload_비소유_멘토면_403() {
    when(sessionRepository.findById(SESSION_ID))
        .thenReturn(Optional.of(session(SESSION_ID, COHORT_ID, 1, SessionStatus.SCHEDULED)));
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, 999L, CohortStatus.ONGOING)));

    assertThatThrownBy(() -> attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile()))
        .isInstanceOf(AccessDeniedException.class);
    verify(fileStorageService, never()).store(any());
  }

  @Test
  void upload_종료된_코호트면_409() {
    when(sessionRepository.findById(SESSION_ID))
        .thenReturn(Optional.of(session(SESSION_ID, COHORT_ID, 1, SessionStatus.SCHEDULED)));
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.CLOSED)));

    assertThatThrownBy(() -> attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile()))
        .isInstanceOf(CohortClosedException.class);
    verify(fileStorageService, never()).store(any());
  }

  @Test
  void upload_매직바이트_위반이면_400_이고_저장하지_않는다() {
    when(sessionRepository.findById(SESSION_ID))
        .thenReturn(Optional.of(session(SESSION_ID, COHORT_ID, 1, SessionStatus.SCHEDULED)));
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    doThrow(new FileConstraintViolationException("형식 불일치"))
        .when(fileSignatureValidator)
        .validate(any());

    assertThatThrownBy(() -> attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile()))
        .isInstanceOf(FileConstraintViolationException.class);
    verify(fileStorageService, never()).store(any());
  }

  @Test
  void upload_성공시_writer에_위임하고_EvidenceDto_반환() {
    when(sessionRepository.findById(SESSION_ID))
        .thenReturn(Optional.of(session(SESSION_ID, COHORT_ID, 1, SessionStatus.SCHEDULED)));
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    when(fileStorageService.store(any())).thenReturn("stored.jpg");
    when(evidenceWriter.persistAndVerify(
            eq(SESSION_ID), eq("stored.jpg"), anyString(), anyLong(), eq(MENTOR_ID)))
        .thenReturn(evidence(77L, SESSION_ID));

    EvidenceDto dto = attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile());

    assertThat(dto.id()).isEqualTo(77L);
    assertThat(dto.sessionId()).isEqualTo(SESSION_ID);
    verify(evidenceWriter)
        .persistAndVerify(eq(SESSION_ID), eq("stored.jpg"), anyString(), anyLong(), eq(MENTOR_ID));
  }

  @Test
  void upload_트랜잭션_실패시_저장한_파일을_보상_삭제한다() {
    when(sessionRepository.findById(SESSION_ID))
        .thenReturn(Optional.of(session(SESSION_ID, COHORT_ID, 1, SessionStatus.SCHEDULED)));
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    when(fileStorageService.store(any())).thenReturn("orphan.jpg");
    doThrow(new RuntimeException("DB 롤백"))
        .when(evidenceWriter)
        .persistAndVerify(anyLong(), anyString(), anyString(), anyLong(), anyLong());

    assertThatThrownBy(() -> attendanceService.uploadEvidence(MENTOR_ID, SESSION_ID, jpegFile()))
        .isInstanceOf(IllegalStateException.class);
    // 보상 삭제 호출 검증(R-U4-13)
    verify(fileStorageService).delete("orphan.jpg");
  }

  @Test
  void sessionsOf_소유멘토는_진도율을_계산한다() {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    Session s1 = session(101L, COHORT_ID, 1, SessionStatus.SCHEDULED);
    Session s2 = session(102L, COHORT_ID, 2, SessionStatus.VERIFIED);
    when(sessionRepository.findByCohortIdOrderBySeqAsc(COHORT_ID)).thenReturn(List.of(s1, s2));
    when(evidenceRepository.findBySessionIdInOrderByCreatedAtDesc(List.of(101L, 102L)))
        .thenReturn(List.of(evidence(9L, 102L)));

    CohortAttendanceDto dto = attendanceService.sessionsOf(COHORT_ID, MENTOR_ID, false);

    assertThat(dto.totalCount()).isEqualTo(2);
    assertThat(dto.verifiedCount()).isEqualTo(1);
    assertThat(dto.progressRate()).isEqualTo(0.5);
    assertThat(dto.sessions()).hasSize(2);
    assertThat(dto.sessions().get(1).hasEvidence()).isTrue();
    assertThat(dto.sessions().get(1).latestEvidenceId()).isEqualTo(9L);
  }

  @Test
  void sessionsOf_참여자도_관리자도_아니면_403() {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT_ID, 55L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> attendanceService.sessionsOf(COHORT_ID, 55L, false))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void sessionsOf_확정_멘티는_조회할_수_있다() {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    Enrollment confirmed = Enrollment.confirmed(COHORT_ID, 55L);
    ReflectionTestUtils.setField(confirmed, "status", EnrollmentStatus.CONFIRMED);
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT_ID, 55L))
        .thenReturn(Optional.of(confirmed));
    when(sessionRepository.findByCohortIdOrderBySeqAsc(COHORT_ID)).thenReturn(List.of());

    CohortAttendanceDto dto = attendanceService.sessionsOf(COHORT_ID, 55L, false);

    assertThat(dto.totalCount()).isZero();
    assertThat(dto.progressRate()).isZero();
  }
}
