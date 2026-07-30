package com.learnkk.attendance;

import com.learnkk.attendance.dto.CohortAttendanceDto;
import com.learnkk.attendance.dto.EvidenceDto;
import com.learnkk.attendance.dto.SessionAttendanceDto;
import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.common.exception.CohortClosedException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.enrollment.EnrollmentStatus;
import com.learnkk.file.FileStorageService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 증빙 업로드·진도 조회·다운로드 오케스트레이션 서비스 (business-logic-model.md §2~4, R-U4-01~11).
 *
 * <p>{@link #uploadEvidence} 는 <b>의도적으로 @Transactional 이 아니다</b>: 사전검증·비트랜잭션 파일 I/O·보상 로직을 포함한
 * 오케스트레이션이며, 원자적 DB 작업(증빙 저장 + 회차 인증)은 별도 트랜잭션 빈 {@link AttendanceEvidenceWriter} 에 위임한다(INV-U4-1).
 * 트랜잭션 롤백 시 저장된 파일을 U1 {@code FileStorageService.delete} 로 보상 삭제하고, 삭제까지 실패하면 {@code
 * ORPHAN_FILE_COMPENSATION_FAILED} 토큰으로 ERROR 로그를 남긴다(R-U4-13).
 *
 * <p>Session·Cohort 는 U2 리포지토리를 읽기 전용으로 주입해 권한·소속을 판정하고, 회차 인증 전이는 U2 {@code
 * SessionService.markVerified} 계약으로만 수행한다(INV-U4-4). 참여자 인가는 U3 {@link EnrollmentRepository} 로 확정
 * 멘티를 확인한다(R-U4-09/11).
 */
@Service
public class AttendanceService {

  private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

  private final FileSignatureValidator fileSignatureValidator;
  private final FileStorageService fileStorageService;
  private final AttendanceEvidenceWriter evidenceWriter;
  private final AttendanceEvidenceRepository evidenceRepository;
  private final SessionRepository sessionRepository;
  private final CohortRepository cohortRepository;
  private final EnrollmentRepository enrollmentRepository;

  public AttendanceService(
      FileSignatureValidator fileSignatureValidator,
      FileStorageService fileStorageService,
      AttendanceEvidenceWriter evidenceWriter,
      AttendanceEvidenceRepository evidenceRepository,
      SessionRepository sessionRepository,
      CohortRepository cohortRepository,
      EnrollmentRepository enrollmentRepository) {
    this.fileSignatureValidator = fileSignatureValidator;
    this.fileStorageService = fileStorageService;
    this.evidenceWriter = evidenceWriter;
    this.evidenceRepository = evidenceRepository;
    this.sessionRepository = sessionRepository;
    this.cohortRepository = cohortRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  /**
   * 회차 증빙 업로드 + 회차 인증 (W-U4-1, R-U4-01~08, INV-U4-1).
   *
   * <p>사전검증(404·403·409·400) → 파일 저장 → 원자적 [증빙 저장 + markVerified] → 실패 시 파일 보상 삭제 순으로 수행한다.
   */
  public EvidenceDto uploadEvidence(Long mentorId, Long sessionId, MultipartFile file) {
    // 1. 사전 검증(트랜잭션 밖) — 존재/소유/종료/파일 제약
    Session session = requireSession(sessionId);
    Cohort cohort = requireCohort(session.getCohortId());
    if (!cohort.isOwnedBy(mentorId)) {
      throw new AccessDeniedException("코호트 소유 멘토만 증빙을 업로드할 수 있습니다");
    }
    if (cohort.getStatus() == CohortStatus.CLOSED) {
      throw new CohortClosedException("종료된 코호트에는 증빙을 업로드할 수 없습니다");
    }
    // 매직바이트 + 크기 + 선언 MIME 교차 검증(store 호출 전, security-design.md §2)
    fileSignatureValidator.validate(file);

    // 2. 파일 저장(비트랜잭션 외부 I/O). U1 store 가 확장자+선언 MIME+크기 기본 검증 재확인 후 저장.
    String storedPath = fileStorageService.store(file);

    // 3. 원자적 [증빙 이력 저장 + 회차 인증] + 4. 실패 시 파일 보상 삭제(R-U4-13)
    try {
      AttendanceEvidence saved =
          evidenceWriter.persistAndVerify(
              sessionId, storedPath, file.getContentType(), file.getSize(), mentorId);
      return EvidenceDto.from(saved);
    } catch (RuntimeException txError) {
      compensate(storedPath);
      throw new IllegalStateException("증빙 저장에 실패했습니다", txError);
    }
  }

  /** 트랜잭션 롤백 시 고아 파일 보상 삭제(멱등). 삭제까지 실패하면 경로를 ERROR 로그로 남긴다(수동 정리 대상). */
  private void compensate(String storedPath) {
    try {
      fileStorageService.delete(storedPath);
    } catch (RuntimeException deleteError) {
      log.error("ORPHAN_FILE_COMPENSATION_FAILED path={}", storedPath, deleteError);
    }
  }

  /**
   * 코호트 진도·출석 조회 (W-U4-2, R-U4-09/10).
   *
   * <p>참여자(소유 멘토·확정 멘티)·관리자만 조회 가능. 회차 status 집계 + 회차별 증빙 존재 여부/최근 증빙 id 를 한 응답으로 제공하며 진도율(인증/전체)을
   * 계산한다.
   */
  @Transactional(readOnly = true)
  public CohortAttendanceDto sessionsOf(Long cohortId, Long requesterId, boolean isAdmin) {
    Cohort cohort = requireCohort(cohortId);
    assertParticipantOrAdmin(cohort, requesterId, isAdmin);

    List<Session> sessions = sessionRepository.findByCohortIdOrderBySeqAsc(cohortId);
    List<Long> sessionIds = sessions.stream().map(Session::getId).toList();

    // 회차별 최근 증빙(id)을 한 번의 벌크 조회로 집계(N+1 회피, performance-design.md §3).
    Map<Long, Long> latestEvidenceBySession =
        sessionIds.isEmpty()
            ? Map.of()
            : evidenceRepository.findBySessionIdInOrderByCreatedAtDesc(sessionIds).stream()
                .collect(
                    Collectors.toMap(
                        AttendanceEvidence::getSessionId,
                        AttendanceEvidence::getId,
                        (first, later) -> first)); // 최신순 정렬이므로 첫 값이 최근 증빙

    List<SessionAttendanceDto> rows =
        sessions.stream()
            .map(
                s -> {
                  Long latestId = latestEvidenceBySession.get(s.getId());
                  return SessionAttendanceDto.of(s, latestId != null, latestId);
                })
            .toList();

    return CohortAttendanceDto.of(cohortId, rows);
  }

  /**
   * 증빙 파일 다운로드 (W-U4-3, R-U4-11).
   *
   * <p>참여자·관리자만 다운로드 가능. 증빙이 경로의 회차에 속하는지 확인 후 U1 {@code FileStorageService.load} 로 스트리밍 리소스를 얻는다.
   * 다운로드 파일명은 서버가 evidenceId·MIME 기준으로 생성해 헤더 인젝션을 방지한다(security-design.md §2).
   */
  @Transactional(readOnly = true)
  public EvidenceDownload downloadEvidence(
      Long sessionId, Long evidenceId, Long requesterId, boolean isAdmin) {
    AttendanceEvidence evidence =
        evidenceRepository
            .findById(evidenceId)
            .orElseThrow(() -> new EntityNotFoundException("증빙을 찾을 수 없습니다"));
    if (!evidence.getSessionId().equals(sessionId)) {
      throw new EntityNotFoundException("증빙을 찾을 수 없습니다");
    }
    Session session = requireSession(sessionId);
    Cohort cohort = requireCohort(session.getCohortId());
    assertParticipantOrAdmin(cohort, requesterId, isAdmin);

    Resource resource = fileStorageService.load(evidence.getFilePath());
    String filename = "evidence-" + evidenceId + extensionFor(evidence.getMimeType());
    return new EvidenceDownload(resource, evidence.getMimeType(), filename, evidence.getSize());
  }

  // ---- 내부 헬퍼 ----

  private Session requireSession(Long sessionId) {
    return sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new EntityNotFoundException("회차를 찾을 수 없습니다"));
  }

  private Cohort requireCohort(Long cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
  }

  /** 참여자(소유 멘토·확정 멘티)·관리자 인가(R-U4-09/11). 아니면 403. */
  private void assertParticipantOrAdmin(Cohort cohort, Long requesterId, boolean isAdmin) {
    if (isAdmin
        || cohort.isOwnedBy(requesterId)
        || isConfirmedMentee(cohort.getId(), requesterId)) {
      return;
    }
    throw new AccessDeniedException("코호트 참여자 또는 관리자만 조회할 수 있습니다");
  }

  /** U3 확정 멘티 여부(read-only). U3(enrollment)의 확정 참여 데이터로 판정한다. */
  private boolean isConfirmedMentee(Long cohortId, Long userId) {
    Optional<EnrollmentStatus> status =
        enrollmentRepository.findByCohortIdAndMenteeId(cohortId, userId).map(e -> e.getStatus());
    return status.map(s -> s == EnrollmentStatus.CONFIRMED).orElse(false);
  }

  private String extensionFor(String mimeType) {
    return switch (mimeType == null ? "" : mimeType.toLowerCase()) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "application/pdf" -> ".pdf";
      default -> "";
    };
  }
}
