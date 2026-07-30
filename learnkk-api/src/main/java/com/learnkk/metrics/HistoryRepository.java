package com.learnkk.metrics;

import com.learnkk.attendance.AttendanceEvidence;
import com.learnkk.metrics.dto.EvidenceHistoryItemDto;
import com.learnkk.metrics.dto.ReportHistoryItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 이력 조회 리포지토리 — 읽기 전용 조인 프로젝션 (business-logic-model.md §3/§4, R-U6-09/10).
 *
 * <p>U4 증빙·U5 보고서 이력을 코호트·회차·업로더/작성자와 조인해 페이지네이션 목록으로 반환한다. 세션·참여·보고서는 JPA 연관 없이 스칼라 FK 만 보유하므로
 * theta 조인(FROM 다중 엔티티 + WHERE 조인 조건)으로 결합하며, <b>생성자 표현식</b>으로 성명 등 조인 컬럼을 한 번에 로딩해 N+1 을
 * 회피한다(performance-design.md §2). 쓰기 메서드가 노출되지 않는 좁은 {@link Repository} 베이스를 상속한다(INV-U6-1). {@code
 * cohortId} 가 null 이면 전체, 아니면 해당 코호트로 필터한다(선택 필터).
 */
public interface HistoryRepository extends Repository<AttendanceEvidence, Long> {

  /** 증빙 이력(코호트·회차·업로더 성명 조인, 최신순) (R-U6-09). {@code cohortId} 선택 필터, createdAt desc 페이지네이션. */
  @Query(
      value =
          "SELECT new com.learnkk.metrics.dto.EvidenceHistoryItemDto("
              + " e.id, s.id, c.title, s.seq, e.mimeType, e.size, u.name, e.createdAt)"
              + " FROM AttendanceEvidence e, com.learnkk.cohort.Session s,"
              + " com.learnkk.cohort.Cohort c, com.learnkk.user.User u"
              + " WHERE e.sessionId = s.id AND s.cohortId = c.id AND e.uploadedBy = u.id"
              + " AND (:cohortId IS NULL OR c.id = :cohortId)"
              + " ORDER BY e.createdAt DESC",
      countQuery =
          "SELECT COUNT(e) FROM AttendanceEvidence e, com.learnkk.cohort.Session s"
              + " WHERE e.sessionId = s.id"
              + " AND (:cohortId IS NULL OR s.cohortId = :cohortId)")
  Page<EvidenceHistoryItemDto> findEvidenceHistory(
      @Param("cohortId") Long cohortId, Pageable pageable);

  /**
   * 보고서 이력(코호트·작성자 성명·첨부 유무, 최신순) (R-U6-10). {@code cohortId} 선택 필터, submittedAt desc 페이지네이션. 첨부
   * 유무는 {@code filePath} 존재 여부로 계산한다(원경로 미노출).
   */
  @Query(
      value =
          "SELECT new com.learnkk.metrics.dto.ReportHistoryItemDto("
              + " r.id, c.id, c.title, u.name,"
              + " CASE WHEN r.filePath IS NOT NULL AND r.filePath <> '' THEN TRUE ELSE FALSE END,"
              + " r.submittedAt)"
              + " FROM com.learnkk.completion.FinalReport r,"
              + " com.learnkk.cohort.Cohort c, com.learnkk.user.User u"
              + " WHERE r.cohortId = c.id AND r.authorId = u.id"
              + " AND (:cohortId IS NULL OR c.id = :cohortId)"
              + " ORDER BY r.submittedAt DESC",
      countQuery =
          "SELECT COUNT(r) FROM com.learnkk.completion.FinalReport r"
              + " WHERE (:cohortId IS NULL OR r.cohortId = :cohortId)")
  Page<ReportHistoryItemDto> findReportHistory(@Param("cohortId") Long cohortId, Pageable pageable);
}
