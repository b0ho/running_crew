package com.learnkk.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.attendance.AttendanceEvidence;
import com.learnkk.attendance.AttendanceEvidenceRepository;
import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.completion.FinalReport;
import com.learnkk.completion.FinalReportRepository;
import com.learnkk.metrics.dto.EvidenceHistoryItemDto;
import com.learnkk.metrics.dto.ReportHistoryItemDto;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 이력 조회 통합 테스트 (Testcontainers) — 증빙/보고서 이력의 조인·최신순·페이지네이션·cohortId 필터·첨부 유무를 실 PostgreSQL 로
 * 검증한다(R-U6-08~10).
 *
 * <p>두 코호트에 걸쳐 증빙·보고서를 시드하고, 조인 컬럼(코호트 제목·회차 순번·업로더/작성자 성명)이 채워지는지, createdAt/submittedAt 최신순 정렬과
 * 20건 페이지네이션, cohortId 필터 on/off, 보고서 첨부 유무 계산을 확인한다.
 */
class HistoryIntegrationTest extends IntegrationTestBase {

  @Autowired private HistoryService historyService;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private AttendanceEvidenceRepository evidenceRepository;
  @Autowired private FinalReportRepository finalReportRepository;
  @Autowired private UserRepository userRepository;

  private Long cohortA;
  private Long cohortB;
  private Long uploaderId;
  private Long authorId;

  @BeforeEach
  void setUp() {
    evidenceRepository.deleteAll();
    finalReportRepository.deleteAll();
    sessionRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();

    uploaderId =
        userRepository
            .save(
                User.newMember(
                    "uploader@learnkk.local", "업로더", "uploader", "$2a$10$dummyhash00000"))
            .getId();
    authorId =
        userRepository
            .save(User.newMember("author@learnkk.local", "작성자", "author", "$2a$10$dummyhash00000"))
            .getId();

    cohortA = createCohort("코호트A");
    cohortB = createCohort("코호트B");
  }

  @Test
  void evidenceHistory_조인컬럼이_채워지고_최신순으로_정렬된다() {
    Long s1 = createSession(cohortA, 1);
    Long s2 = createSession(cohortA, 2);
    // 오래된 것 먼저, 최신 것 나중에 저장 → 최신순이면 두 번째가 앞.
    saveEvidence(s1, "image/png", 100L, "2026-01-01T00:00:00Z");
    saveEvidence(s2, "application/pdf", 200L, "2026-02-01T00:00:00Z");

    Page<EvidenceHistoryItemDto> page = historyService.evidenceHistory(null, 0, 20);

    assertThat(page.getTotalElements()).isEqualTo(2);
    EvidenceHistoryItemDto first = page.getContent().get(0);
    // 최신순(2026-02-01 이 먼저).
    assertThat(first.mimeType()).isEqualTo("application/pdf");
    assertThat(first.sessionSeq()).isEqualTo(2);
    assertThat(first.cohortTitle()).isEqualTo("코호트A");
    assertThat(first.uploadedBy()).isEqualTo("업로더");
    assertThat(first.size()).isEqualTo(200L);
    assertThat(first.sessionId()).isEqualTo(s2);
  }

  @Test
  void evidenceHistory_cohortId필터가_해당_코호트만_반환한다() {
    Long sa = createSession(cohortA, 1);
    Long sb = createSession(cohortB, 1);
    saveEvidence(sa, "image/png", 100L, "2026-01-01T00:00:00Z");
    saveEvidence(sb, "image/png", 100L, "2026-01-02T00:00:00Z");

    Page<EvidenceHistoryItemDto> all = historyService.evidenceHistory(null, 0, 20);
    Page<EvidenceHistoryItemDto> onlyB = historyService.evidenceHistory(cohortB, 0, 20);

    assertThat(all.getTotalElements()).isEqualTo(2);
    assertThat(onlyB.getTotalElements()).isEqualTo(1);
    assertThat(onlyB.getContent().get(0).cohortTitle()).isEqualTo("코호트B");
  }

  @Test
  void evidenceHistory_페이지네이션_기본20건_상한() {
    Long s1 = createSession(cohortA, 1);
    for (int i = 0; i < 25; i++) {
      saveEvidence(s1, "image/png", 10L, String.format("2026-01-%02dT00:00:00Z", (i % 28) + 1));
    }

    Page<EvidenceHistoryItemDto> page0 = historyService.evidenceHistory(cohortA, 0, 20);

    assertThat(page0.getTotalElements()).isEqualTo(25);
    assertThat(page0.getContent()).hasSize(20);
    assertThat(page0.getTotalPages()).isEqualTo(2);
  }

  @Test
  void reportHistory_조인_최신순_첨부유무_계산() {
    // 첨부 없는 보고서(오래됨) + 첨부 있는 보고서(최신).
    saveReport(cohortA, null, "2026-01-01T00:00:00Z");
    saveReport(cohortA, "report.pdf", "2026-03-01T00:00:00Z");

    Page<ReportHistoryItemDto> page = historyService.reportHistory(null, 0, 20);

    assertThat(page.getTotalElements()).isEqualTo(2);
    ReportHistoryItemDto first = page.getContent().get(0);
    // 최신순 → 첨부 있는 것이 먼저.
    assertThat(first.hasAttachment()).isTrue();
    assertThat(first.cohortTitle()).isEqualTo("코호트A");
    assertThat(first.authorName()).isEqualTo("작성자");
    assertThat(page.getContent().get(1).hasAttachment()).isFalse();
  }

  @Test
  void reportHistory_cohortId필터가_해당_코호트만_반환한다() {
    saveReport(cohortA, "a.pdf", "2026-01-01T00:00:00Z");
    saveReport(cohortB, null, "2026-01-02T00:00:00Z");

    Page<ReportHistoryItemDto> onlyA = historyService.reportHistory(cohortA, 0, 20);

    assertThat(onlyA.getTotalElements()).isEqualTo(1);
    assertThat(onlyA.getContent().get(0).cohortId()).isEqualTo(cohortA);
    assertThat(onlyA.getContent().get(0).hasAttachment()).isTrue();
  }

  // ---- 시드 헬퍼 ----

  private Long createCohort(String title) {
    Cohort c =
        Cohort.open(
            uploaderId, title, "설명", 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 5);
    ReflectionTestUtils.setField(c, "status", CohortStatus.CLOSED);
    return cohortRepository.save(c).getId();
  }

  private Long createSession(Long cohortId, int seq) {
    return sessionRepository.save(Session.scheduled(cohortId, seq)).getId();
  }

  private void saveEvidence(Long sessionId, String mimeType, long size, String createdAt) {
    AttendanceEvidence e =
        AttendanceEvidence.of(
            sessionId, "path/" + sessionId + "-" + size, mimeType, size, uploaderId);
    ReflectionTestUtils.setField(e, "createdAt", java.time.Instant.parse(createdAt));
    evidenceRepository.save(e);
  }

  private void saveReport(Long cohortId, String filePath, String submittedAt) {
    FinalReport r = FinalReport.of(cohortId, authorId, "본문", filePath);
    ReflectionTestUtils.setField(r, "submittedAt", java.time.Instant.parse(submittedAt));
    finalReportRepository.save(r);
  }
}
