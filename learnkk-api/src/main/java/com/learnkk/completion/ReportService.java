package com.learnkk.completion;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.completion.dto.ReportDto;
import com.learnkk.completion.dto.ReportSubmitRequest;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.enrollment.EnrollmentStatus;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 최종 보고서 서비스 (business-logic-model.md §3, R-U5-15~19).
 *
 * <p>{@link #submit} 은 <b>의도적으로 @Transactional 이 아니다</b>: 사전검증·비트랜잭션 파일 I/O·보상 로직을 포함한 오케스트레이션이며,
 * 순수 DB insert 는 별도 트랜잭션 빈 {@link ReportWriter} 에 위임한다(U4 업로드와 동일 패턴). 첨부 저장 후 트랜잭션이 실패하면 U1 {@code
 * FileStorageService.delete} 로 보상 삭제하고, 삭제까지 실패하면 {@code ORPHAN_FILE_COMPENSATION_FAILED} 토큰으로
 * ERROR 로그를 남긴다(R-U5-17).
 *
 * <p>참여자 인가(멘토·확정 멘티)는 U2 Cohort·U3 Enrollment 를 읽어 판정한다(R-U5-15/19). {@link #mentorReportExists} 는
 * 정산 판정(R-U5-11)에서 U5 CompletionService 가 호출하는 계약이다.
 */
@Service
public class ReportService {

  private static final Logger log = LoggerFactory.getLogger(ReportService.class);

  private final com.learnkk.file.FileStorageService fileStorageService;
  private final ReportWriter reportWriter;
  private final FinalReportRepository finalReportRepository;
  private final CohortRepository cohortRepository;
  private final EnrollmentRepository enrollmentRepository;

  public ReportService(
      com.learnkk.file.FileStorageService fileStorageService,
      ReportWriter reportWriter,
      FinalReportRepository finalReportRepository,
      CohortRepository cohortRepository,
      EnrollmentRepository enrollmentRepository) {
    this.fileStorageService = fileStorageService;
    this.reportWriter = reportWriter;
    this.finalReportRepository = finalReportRepository;
    this.cohortRepository = cohortRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  /**
   * 최종 보고서 제출 (W-U5-2, R-U5-15~17).
   *
   * <p>참여자(멘토·확정 멘티)만 제출 가능하며 body 는 필수(DTO @NotBlank + 서비스 방어). 첨부가 있으면 U1 store → [TX insert] →
   * 실패 시 delete 보상. 첨부가 없으면 순수 DB 트랜잭션.
   */
  public ReportDto submit(
      Long userId, Long cohortId, ReportSubmitRequest request, MultipartFile file) {
    Cohort cohort = requireCohort(cohortId);
    assertParticipant(cohort, userId);
    if (request == null || !StringUtils.hasText(request.body())) {
      // DTO 검증이 1차 방어하나 서비스 직접 호출 경로도 보호한다(R-U5-15).
      throw new com.learnkk.common.exception.ValidationException("보고서 본문은 필수입니다");
    }
    String body = request.body().trim();

    boolean hasFile = file != null && !file.isEmpty();
    if (!hasFile) {
      // 첨부 없음 — 순수 DB 트랜잭션.
      return ReportDto.from(reportWriter.persist(cohortId, userId, body, null));
    }

    // 첨부 있음 — 비트랜잭션 저장 후 원자적 insert, 실패 시 보상 삭제(R-U5-16/17).
    String storedPath = fileStorageService.store(file);
    try {
      return ReportDto.from(reportWriter.persist(cohortId, userId, body, storedPath));
    } catch (RuntimeException txError) {
      compensate(storedPath);
      throw new IllegalStateException("보고서 저장에 실패했습니다", txError);
    }
  }

  /** 보고서 이력 조회 (W-U5-3, R-U5-19). 참여자·관리자만. */
  @Transactional(readOnly = true)
  public Page<ReportDto> historyOf(
      Long cohortId, Long requesterId, boolean isAdmin, Pageable pageable) {
    Cohort cohort = requireCohort(cohortId);
    if (!isAdmin) {
      assertParticipant(cohort, requesterId);
    }
    return finalReportRepository
        .findByCohortIdOrderBySubmittedAtDesc(cohortId, pageable)
        .map(ReportDto::from);
  }

  /**
   * 멘토 최종 보고서 존재 여부 (정산 판정 R-U5-11). authorId == 코호트 멘토 id 인 보고서 존재 여부. U5 CompletionService 가 호출하는
   * U5 내부 계약이다.
   */
  @Transactional(readOnly = true)
  public boolean mentorReportExists(Long cohortId, Long mentorId) {
    return finalReportRepository.existsByCohortIdAndAuthorId(cohortId, mentorId);
  }

  // ---- 내부 헬퍼 ----

  private void compensate(String storedPath) {
    try {
      fileStorageService.delete(storedPath);
    } catch (RuntimeException deleteError) {
      log.error("ORPHAN_FILE_COMPENSATION_FAILED path={}", storedPath, deleteError);
    }
  }

  private Cohort requireCohort(Long cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
  }

  /** 참여자(소유 멘토·확정 멘티) 인가(R-U5-15/19). 아니면 403. */
  private void assertParticipant(Cohort cohort, Long userId) {
    if (cohort.isOwnedBy(userId) || isConfirmedMentee(cohort.getId(), userId)) {
      return;
    }
    throw new AccessDeniedException("코호트 참여자만 수행할 수 있습니다");
  }

  private boolean isConfirmedMentee(Long cohortId, Long userId) {
    Optional<EnrollmentStatus> status =
        enrollmentRepository.findByCohortIdAndMenteeId(cohortId, userId).map(e -> e.getStatus());
    return status.map(s -> s == EnrollmentStatus.CONFIRMED).orElse(false);
  }
}
