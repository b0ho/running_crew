package com.learnkk.completion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** FinalReport 리포지토리 (performance-design.md §3, R-U5-11/19). */
public interface FinalReportRepository extends JpaRepository<FinalReport, Long> {

  /** 보고서 이력(코호트 스코프, 최신순 + 페이지네이션 기본 20) (R-U5-19, historyOf). */
  Page<FinalReport> findByCohortIdOrderBySubmittedAtDesc(Long cohortId, Pageable pageable);

  /**
   * 멘토 최종 보고서 존재 여부(정산 판정 mentorReportExists, R-U5-11).
   *
   * <p>{@code authorId == cohort.mentorId} 인 보고서 존재 여부로 멘토 보고서를 멘티 보고서와 구분한다.
   */
  boolean existsByCohortIdAndAuthorId(Long cohortId, Long authorId);
}
