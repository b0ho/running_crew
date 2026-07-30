package com.learnkk.completion;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.common.exception.DataIntegrityException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.completion.dto.CohortEndSummaryDto;
import com.learnkk.enrollment.EnrollmentService;
import com.learnkk.enrollment.dto.EnrollmentDto;
import com.learnkk.file.FileStorageService;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코호트 종료 오케스트레이션·판정 서비스 (business-logic-model.md §2/§4, R-U5-01~20, INV-U5-1~5).
 *
 * <p>{@link #endCohort} 는 <b>의도적으로 @Transactional 이 아니다</b>: 사전검증 → 회차 집계 → 수료 판정 → (충족 시)수료증 이미지
 * 생성·저장(비트랜잭션 I/O, imagePath 누적) → 정산 판정 → 원자적 반영({@link CompletionWriter#finalizeEnd}) 순의
 * 오케스트레이션이다. 원자적 DB 작업(수료증 insert + 정산 upsert + 상태 전이 + 알림)은 트랜잭션 빈 {@link CompletionWriter} 에
 * 위임하며, 트랜잭션이 롤백되면 누적한 모든 imagePath 를 U1 {@code FileStorageService.delete} 로 보상 삭제한다(R-U5-08a). 5명
 * 중 마지막에서 실패해도 앞선 이미지가 모두 정리된다.
 *
 * <p>판정은 전적으로 서버측 정수 비교로 수행되어 클라이언트가 조작할 수 없다(INV-U5-5, security-design.md §2). 수료: {@code
 * verified*100 >= total*80}. 정산: {@code verified == total AND 멘토 보고서 제출}.
 */
@Service
public class CompletionService {

  private static final Logger log = LoggerFactory.getLogger(CompletionService.class);

  private static final String CERTIFICATE_MIME = "image/png";

  private final CohortRepository cohortRepository;
  private final SessionRepository sessionRepository;
  private final EnrollmentService enrollmentService;
  private final UserRepository userRepository;
  private final CertificateRenderer certificateRenderer;
  private final FileStorageService fileStorageService;
  private final CertificateRepository certificateRepository;
  private final ReportService reportService;
  private final CompletionWriter completionWriter;

  public CompletionService(
      CohortRepository cohortRepository,
      SessionRepository sessionRepository,
      EnrollmentService enrollmentService,
      UserRepository userRepository,
      CertificateRenderer certificateRenderer,
      FileStorageService fileStorageService,
      CertificateRepository certificateRepository,
      ReportService reportService,
      CompletionWriter completionWriter) {
    this.cohortRepository = cohortRepository;
    this.sessionRepository = sessionRepository;
    this.enrollmentService = enrollmentService;
    this.userRepository = userRepository;
    this.certificateRenderer = certificateRenderer;
    this.fileStorageService = fileStorageService;
    this.certificateRepository = certificateRepository;
    this.reportService = reportService;
    this.completionWriter = completionWriter;
  }

  /**
   * 코호트 종료 (W-U5-1, R-U5-01~20). 소유 멘토만·진행중 상태만.
   *
   * @return 종료 요약(수료자 수·미수료 수·전체 확정 멘티·정산 충족·발급 증서 수)
   */
  public CohortEndSummaryDto endCohort(Long mentorId, Long cohortId) {
    // 1) 사전 검증 — 404 / 403 / 409 (트랜잭션 밖).
    Cohort cohort = requireCohort(cohortId);
    if (!cohort.isOwnedBy(mentorId)) {
      throw new AccessDeniedException("코호트 소유 멘토만 종료할 수 있습니다");
    }
    if (cohort.getStatus() != CohortStatus.ONGOING) {
      throw new InvalidStateTransitionException("진행중 상태의 코호트만 종료할 수 있습니다");
    }

    // 2) 회차 집계(U2 읽기). total==0 이면 데이터 정합 오류 500(R-U5-10).
    List<Session> sessions = sessionRepository.findByCohortIdOrderBySeqAsc(cohortId);
    int totalSessions = sessions.size();
    if (totalSessions == 0) {
      throw new DataIntegrityException("회차가 없는 코호트는 종료 판정할 수 없습니다");
    }
    int verifiedSessions = (int) sessions.stream().filter(Session::isVerified).count();

    // 3) 확정 멘티 목록(U3 읽기).
    List<EnrollmentDto> confirmed = enrollmentService.confirmedEnrollments(cohortId);
    List<Long> confirmedMenteeIds = confirmed.stream().map(EnrollmentDto::menteeId).toList();
    int totalConfirmed = confirmedMenteeIds.size();

    // 4) 수료 판정 — 정수 비교로 부동소수 경계 오차 제거(R-U5-06, INV-U5-5).
    boolean completionMet = (long) verifiedSessions * 100 >= (long) totalSessions * 80;

    // 5) 수료 시 확정 멘티별 수료증 이미지 생성·저장(imagePath 누적, R-U5-08/08a).
    List<String> storedImagePaths = new ArrayList<>();
    List<CertificateIssuance> issuances = new ArrayList<>();
    if (completionMet && totalConfirmed > 0) {
      Map<Long, String> menteeNames = menteeNames(confirmedMenteeIds);
      LocalDate issuedDate = LocalDate.now();
      for (Long menteeId : confirmedMenteeIds) {
        String name = menteeNames.getOrDefault(menteeId, "멘티");
        byte[] png = certificateRenderer.render(name, cohort.getTitle(), issuedDate);
        String storedPath =
            fileStorageService.store(
                new GeneratedImageMultipartFile(png, "certificate.png", CERTIFICATE_MIME));
        storedImagePaths.add(storedPath);
        issuances.add(new CertificateIssuance(menteeId, storedPath));
      }
    }

    // 6) 정산 판정(R-U5-11) — 전 회차 인증 완료 AND 멘토 보고서 제출.
    boolean settlementSatisfied =
        verifiedSessions == totalSessions
            && reportService.mentorReportExists(cohortId, cohort.getMentorId());

    // 7) 원자적 반영 + 실패 시 누적 이미지 보상 삭제(R-U5-08a, INV-U5-3).
    try {
      completionWriter.finalizeEnd(
          cohortId,
          cohort.getMentorId(),
          cohort.getTitle(),
          completionMet,
          issuances,
          settlementSatisfied,
          confirmedMenteeIds);
    } catch (RuntimeException txError) {
      compensateAll(storedImagePaths);
      throw new IllegalStateException("코호트 종료 처리에 실패했습니다", txError);
    }

    // 8) 종료 요약.
    int certifiedCount = completionMet ? totalConfirmed : 0;
    int notCertifiedCount = completionMet ? 0 : totalConfirmed;
    long issuedCertificateCount = certificateRepository.countByCohortId(cohortId);
    return CohortEndSummaryDto.of(
        certifiedCount,
        notCertifiedCount,
        totalConfirmed,
        settlementSatisfied,
        issuedCertificateCount);
  }

  /**
   * 수료증 조회/다운로드 (W-U5-4, security-design.md §3). 본인 멘티만 조회 가능하며, 조회는 요청자 세션 id 로 스코프한다(수평 권한 상승 방지
   * — 타인 수료증 접근은 404). 미발급이면 404.
   */
  @Transactional(readOnly = true)
  public CertificateDownload certificateOf(Long cohortId, Long menteeId) {
    Certificate certificate =
        certificateRepository
            .findByCohortIdAndMenteeId(cohortId, menteeId)
            .orElseThrow(() -> new EntityNotFoundException("수료증을 찾을 수 없습니다"));
    Resource resource = fileStorageService.load(certificate.getImagePath());
    String filename = "certificate-" + cohortId + ".png";
    return new CertificateDownload(resource, CERTIFICATE_MIME, filename);
  }

  // ---- 내부 헬퍼 ----

  /** 트랜잭션 롤백 시 누적한 모든 수료증 이미지 보상 삭제(멱등). 실패 경로는 ERROR 로그(수동 정리, R-U5-08a). */
  private void compensateAll(List<String> storedImagePaths) {
    for (String path : storedImagePaths) {
      try {
        fileStorageService.delete(path);
      } catch (RuntimeException deleteError) {
        log.error("ORPHAN_FILE_COMPENSATION_FAILED path={}", path, deleteError);
      }
    }
  }

  private Map<Long, String> menteeNames(List<Long> menteeIds) {
    if (menteeIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(menteeIds).stream()
        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
  }

  private Cohort requireCohort(Long cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
  }
}
