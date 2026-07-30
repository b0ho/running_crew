package com.learnkk.metrics;

import com.learnkk.metrics.dto.MetricsOverviewDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 지표 집계 서비스 — 읽기 전용 (business-logic-model.md §2, R-U6-04~07, INV-U6-2/3/4).
 *
 * <p>조회 시점의 실제 데이터에서 4개 지표를 실시간 계산한다(캐시 없음, INV-U6-2 → FR-11 데이터 일치). 출석률·수료율·완주 수는 종료됨(CLOSED)
 * 코호트만 대상으로 하며(INV-U6-4), 발급 증서 수는 전체 증서 count 다(R-U6-07). 분모가 0이면 0% 로 안전 처리한다(INV-U6-3). 백분율은 정수
 * 집계 후 소수 1자리로 반올림하되 이는 표시 전용이며 내부 비교에는 사용하지 않는다(R-U6-04/05). 인가(ROLE_ADMIN)는 컨트롤러
 * {@code @PreAuthorize} 로 강제하고, 본 서비스는 순수 조회만 수행한다.
 */
@Service
public class MetricsService {

  private final MetricsRepository metricsRepository;

  public MetricsService(MetricsRepository metricsRepository) {
    this.metricsRepository = metricsRepository;
  }

  /** 운영 지표 개요 — 4개 집계 + 집계 범위 라벨 (W-U6-1). */
  @Transactional(readOnly = true)
  public MetricsOverviewDto overview() {
    int completedCohortCount = (int) metricsRepository.countClosedCohorts();
    long verifiedSessions = metricsRepository.countVerifiedSessionsOfClosed();
    long totalSessions = metricsRepository.countTotalSessionsOfClosed();
    long confirmedMentees = metricsRepository.countConfirmedMenteesOfClosed();
    long certificateCount = metricsRepository.countCertificates();

    double attendanceRate = percentage(verifiedSessions, totalSessions);
    double completionRate = percentage(certificateCount, confirmedMentees);
    String scopeLabel = "종료된 코호트 " + completedCohortCount + "건 기준";

    return new MetricsOverviewDto(
        completedCohortCount, attendanceRate, completionRate, certificateCount, scopeLabel);
  }

  /**
   * 백분율 계산 — 분모 0 → 0% 안전 처리(INV-U6-3), 정수 집계 후 소수 1자리 반올림(표시 전용, R-U6-04/05).
   *
   * <p>예: 3/4 → 75.0, 1/3 → 33.3. 분모가 0 이하면 나눗셈 없이 0.0 반환.
   */
  private static double percentage(long numerator, long denominator) {
    if (denominator <= 0L) {
      return 0.0;
    }
    double raw = (numerator * 100.0) / denominator;
    return Math.round(raw * 10.0) / 10.0;
  }
}
