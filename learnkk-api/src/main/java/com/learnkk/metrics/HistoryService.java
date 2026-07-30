package com.learnkk.metrics;

import com.learnkk.metrics.dto.EvidenceHistoryItemDto;
import com.learnkk.metrics.dto.ReportHistoryItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이력 조회 서비스 — 읽기 전용 (business-logic-model.md §3/§4, R-U6-08~11).
 *
 * <p>증빙 이력(U4)과 보고서 이력(U5)을 각각 별도 뷰로 조회한다(R-U6-08, FR-10 분리 조회). {@code cohortId} 는 선택 필터(null 이면
 * 전체), 페이지네이션 기본 크기는 20건이며 최신순으로 정렬한다(R-U6-09/10). 정렬·조인은 리포지토리의 생성자 표현식 쿼리에서 수행하므로(N+1 회피) 본 서비스는
 * 페이지 파라미터 정규화와 위임만 담당한다. 인가(ROLE_ADMIN)는 컨트롤러 {@code @PreAuthorize} 로 강제한다.
 */
@Service
public class HistoryService {

  /** 페이지네이션 기본 크기 (R-U6-09/10). */
  static final int DEFAULT_PAGE_SIZE = 20;

  private final HistoryRepository historyRepository;

  public HistoryService(HistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  /** 증빙 이력 조회 — 최신순, 기본 20건 (W-U6-2, R-U6-09). */
  @Transactional(readOnly = true)
  public Page<EvidenceHistoryItemDto> evidenceHistory(Long cohortId, int page, int size) {
    return historyRepository.findEvidenceHistory(cohortId, pageable(page, size));
  }

  /** 보고서 이력 조회 — 최신순, 기본 20건 (W-U6-3, R-U6-10). */
  @Transactional(readOnly = true)
  public Page<ReportHistoryItemDto> reportHistory(Long cohortId, int page, int size) {
    return historyRepository.findReportHistory(cohortId, pageable(page, size));
  }

  /** 페이지 파라미터 정규화 — 음수 페이지는 0, size 미지정(≤0)은 기본 20 (정렬은 쿼리 ORDER BY 가 담당). */
  private static Pageable pageable(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
    return PageRequest.of(safePage, safeSize);
  }
}
