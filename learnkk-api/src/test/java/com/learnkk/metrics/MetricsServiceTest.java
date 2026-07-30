package com.learnkk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.learnkk.metrics.dto.MetricsOverviewDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MetricsService 단위 테스트 (Mockito) — 지표 산식·분모 0 안전·CLOSED 범위·certificateCount (R-U6-04~07,
 * INV-U6-3/4).
 *
 * <p>집계 리포지토리를 mock 하여 출석률/수료율 정상 산식, 분모 0 → 0% 안전 처리(회차 0·확정 멘티 0), 소수 1자리 반올림(표시 전용), 완주 코스 수·발급
 * 증서 수·집계 범위 라벨을 검증한다. CLOSED 범위 일관은 리포지토리 쿼리가 담당하므로(진행중 제외) 통합 테스트에서 실 DB 로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

  @Mock private MetricsRepository metricsRepository;

  @InjectMocks private MetricsService metricsService;

  @Test
  void overview_정상_산식_출석률과_수료율을_소수1자리로_계산한다() {
    // 종료 코호트 3건, 인증 3/전체 4 회차 → 75.0%, 증서 1/확정 멘티 3 → 33.3%.
    when(metricsRepository.countClosedCohorts()).thenReturn(3L);
    when(metricsRepository.countVerifiedSessionsOfClosed()).thenReturn(3L);
    when(metricsRepository.countTotalSessionsOfClosed()).thenReturn(4L);
    when(metricsRepository.countConfirmedMenteesOfClosed()).thenReturn(3L);
    when(metricsRepository.countCertificates()).thenReturn(1L);

    MetricsOverviewDto dto = metricsService.overview();

    assertThat(dto.completedCohortCount()).isEqualTo(3);
    assertThat(dto.attendanceRate()).isEqualTo(75.0);
    assertThat(dto.completionRate()).isEqualTo(33.3);
    assertThat(dto.certificateCount()).isEqualTo(1L);
    assertThat(dto.scopeLabel()).isEqualTo("종료된 코호트 3건 기준");
  }

  @Test
  void overview_출석률_분모0이면_0퍼센트_안전처리() {
    // 종료 코호트 0건 → 회차 합 0 → 출석률 0%(예외 없이, INV-U6-3).
    when(metricsRepository.countClosedCohorts()).thenReturn(0L);
    when(metricsRepository.countVerifiedSessionsOfClosed()).thenReturn(0L);
    when(metricsRepository.countTotalSessionsOfClosed()).thenReturn(0L);
    when(metricsRepository.countConfirmedMenteesOfClosed()).thenReturn(0L);
    when(metricsRepository.countCertificates()).thenReturn(0L);

    MetricsOverviewDto dto = metricsService.overview();

    assertThat(dto.attendanceRate()).isEqualTo(0.0);
    assertThat(dto.completionRate()).isEqualTo(0.0);
    assertThat(dto.completedCohortCount()).isZero();
    assertThat(dto.certificateCount()).isZero();
    assertThat(dto.scopeLabel()).isEqualTo("종료된 코호트 0건 기준");
  }

  @Test
  void overview_수료율_확정멘티0이면_증서가_있어도_0퍼센트() {
    // 확정 멘티 0(분모 0) → 수료율 0%(0 나눗셈 방지, INV-U6-3). 증서 count 는 별도로 노출.
    when(metricsRepository.countClosedCohorts()).thenReturn(1L);
    when(metricsRepository.countVerifiedSessionsOfClosed()).thenReturn(2L);
    when(metricsRepository.countTotalSessionsOfClosed()).thenReturn(2L);
    when(metricsRepository.countConfirmedMenteesOfClosed()).thenReturn(0L);
    when(metricsRepository.countCertificates()).thenReturn(5L);

    MetricsOverviewDto dto = metricsService.overview();

    assertThat(dto.attendanceRate()).isEqualTo(100.0);
    assertThat(dto.completionRate()).isEqualTo(0.0);
    assertThat(dto.certificateCount()).isEqualTo(5L);
  }

  @Test
  void overview_반복소수는_소수1자리로_반올림된다() {
    // 인증 1/전체 3 = 33.333...% → 33.3, 증서 2/확정 3 = 66.666...% → 66.7.
    when(metricsRepository.countClosedCohorts()).thenReturn(1L);
    when(metricsRepository.countVerifiedSessionsOfClosed()).thenReturn(1L);
    when(metricsRepository.countTotalSessionsOfClosed()).thenReturn(3L);
    when(metricsRepository.countConfirmedMenteesOfClosed()).thenReturn(3L);
    when(metricsRepository.countCertificates()).thenReturn(2L);

    MetricsOverviewDto dto = metricsService.overview();

    assertThat(dto.attendanceRate()).isEqualTo(33.3);
    assertThat(dto.completionRate()).isEqualTo(66.7);
  }

  @Test
  void overview_certificateCount는_전체_증서수를_그대로_노출한다() {
    when(metricsRepository.countClosedCohorts()).thenReturn(2L);
    when(metricsRepository.countVerifiedSessionsOfClosed()).thenReturn(8L);
    when(metricsRepository.countTotalSessionsOfClosed()).thenReturn(10L);
    when(metricsRepository.countConfirmedMenteesOfClosed()).thenReturn(10L);
    when(metricsRepository.countCertificates()).thenReturn(7L);

    MetricsOverviewDto dto = metricsService.overview();

    assertThat(dto.certificateCount()).isEqualTo(7L);
    assertThat(dto.attendanceRate()).isEqualTo(80.0);
    assertThat(dto.completionRate()).isEqualTo(70.0);
  }
}
