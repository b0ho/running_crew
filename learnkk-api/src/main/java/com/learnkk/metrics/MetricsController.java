package com.learnkk.metrics;

import com.learnkk.metrics.dto.MetricsOverviewDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영 지표 API (frontend-components §3, security-design.md §1, R-U6-01/02).
 *
 * <p>관리자 전용 — 클래스 레벨 {@code @PreAuthorize("hasRole('ADMIN')")}(U1 R-U1-16a 상속). 비관리자는 403, 미인증은 401
 * 로 U1 공통 핸들러/SecurityConfig 가 매핑한다. U6 은 읽기 전용이므로 조회 엔드포인트만 노출한다(INV-U6-1).
 */
@RestController
@RequestMapping("/api/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
public class MetricsController {

  private final MetricsService metricsService;

  public MetricsController(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Operation(
      summary = "운영 지표 조회",
      description =
          "관리자에게 종료된 코호트 기준 운영 지표(완주 코스 수·출석률·수료율·발급 증서 수)를 실시간 집계해 반환합니다."
              + " 출석률·수료율의 분모가 0이면 0%로 안전 처리하며, 집계 범위 라벨을 함께 제공합니다.")
  @GetMapping
  public ResponseEntity<MetricsOverviewDto> overview() {
    return ResponseEntity.ok(metricsService.overview());
  }
}
