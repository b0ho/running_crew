package com.learnkk.metrics;

import com.learnkk.metrics.dto.EvidenceHistoryItemDto;
import com.learnkk.metrics.dto.ReportHistoryItemDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 이력 조회 API (frontend-components §3, security-design.md §1, R-U6-08~11).
 *
 * <p>증빙 이력과 보고서 이력을 각각 별도 엔드포인트로 노출한다(R-U6-08, FR-10 분리 조회). 관리자 전용 — 클래스 레벨
 * {@code @PreAuthorize("hasRole('ADMIN')")}(U1 R-U1-16a 상속). {@code cohortId} 는 선택 필터, {@code
 * page}/{@code size}(기본 0/20) 로 페이지네이션한다. 이력은 조직 전체 데이터와 업로더·작성자 성명(PII)을 포함하므로 관리자에게만
 * 제공한다(security-design.md §3). U6 은 읽기 전용이다(INV-U6-1).
 */
@RestController
@RequestMapping("/api/admin/history")
@PreAuthorize("hasRole('ADMIN')")
public class HistoryController {

  private final HistoryService historyService;

  public HistoryController(HistoryService historyService) {
    this.historyService = historyService;
  }

  @Operation(
      summary = "증빙 이력 조회",
      description =
          "관리자에게 회차 증빙 업로드 이력(코호트·회차·업로더·형식·크기·업로드일)을 최신순으로 반환합니다."
              + " cohortId 로 코호트를 좁힐 수 있으며, 파일 다운로드는 증빙/회차 id 로 기존 스트리밍 엔드포인트를 경유합니다.")
  @GetMapping("/evidence")
  public ResponseEntity<Page<EvidenceHistoryItemDto>> evidenceHistory(
      @RequestParam(required = false) Long cohortId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(historyService.evidenceHistory(cohortId, page, size));
  }

  @Operation(
      summary = "보고서 이력 조회",
      description =
          "관리자에게 최종 보고서 제출 이력(코호트·작성자·첨부유무·제출일)을 최신순으로 반환합니다."
              + " 증빙 이력과 별도 뷰이며, cohortId 로 코호트를 좁힐 수 있습니다.")
  @GetMapping("/reports")
  public ResponseEntity<Page<ReportHistoryItemDto>> reportHistory(
      @RequestParam(required = false) Long cohortId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(historyService.reportHistory(cohortId, page, size));
  }
}
