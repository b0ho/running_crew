import { useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { adminMetricsApi } from '../api/adminMetricsApi';
import type { MetricsOverviewDto } from '../api/types';
import { MetricCard } from './MetricCard';

/**
 * 운영 지표 개요 (frontend-components §2.1, US-14 / FR-11).
 *
 * 종료된 코호트 기준 4개 지표(완주 코스 수·출석률·수료율·발급 증서 수)를 카드로 노출하고, 집계 범위 라벨("종료된 코호트 N건 기준")을 함께 보여준다. 출석률·수료율은
 * 서버가 분모 0 → 0% 로 안전 처리한 백분율을 그대로 표시한다(INV-U6-3). 로딩·오류 상태를 명시한다.
 */
export function MetricsOverview() {
  const [metrics, setMetrics] = useState<MetricsOverviewDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    adminMetricsApi
      .getMetrics()
      .then((data) => {
        if (active) {
          setMetrics(data);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err instanceof ApiError ? err.message : '지표를 불러오지 못했습니다');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <p data-testid="metrics-loading" className="mt-4 text-sm text-gray-400">
        불러오는 중...
      </p>
    );
  }

  if (error) {
    return (
      <p role="alert" data-testid="metrics-error" className="mt-4 text-sm text-red-600">
        {error}
      </p>
    );
  }

  if (!metrics) {
    return (
      <p data-testid="metrics-empty" className="mt-4 text-sm text-gray-500">
        표시할 지표가 없습니다.
      </p>
    );
  }

  return (
    <section data-testid="metrics-overview" aria-label="운영 지표">
      <p data-testid="metrics-scope-label" className="text-sm text-gray-500">
        {metrics.scopeLabel}
      </p>
      <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          testId="metric-completed-cohorts"
          label="완주 코스 수"
          value={`${metrics.completedCohortCount}개`}
          description={`완주(종료된) 코호트 수 ${metrics.completedCohortCount}개`}
        />
        <MetricCard
          testId="metric-attendance-rate"
          label="출석률"
          value={`${metrics.attendanceRate}%`}
          description={`전체 출석률 ${metrics.attendanceRate}퍼센트`}
        />
        <MetricCard
          testId="metric-completion-rate"
          label="수료율"
          value={`${metrics.completionRate}%`}
          description={`수료율 ${metrics.completionRate}퍼센트`}
        />
        <MetricCard
          testId="metric-certificate-count"
          label="발급 증서 수"
          value={`${metrics.certificateCount}장`}
          description={`발급된 수료증 ${metrics.certificateCount}장`}
        />
      </div>
    </section>
  );
}
