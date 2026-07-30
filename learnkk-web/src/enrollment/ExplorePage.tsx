import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { cohortApi } from '../api/cohortApi';
import type { CohortSummaryDto } from '../api/types';
import { CohortStatusBadge } from '../cohorts/StatusBadge';
import { Toast } from '../common/Toast';
import type { ToastMessage } from '../common/Toast';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';
import { JoinButton } from './JoinButton';

/**
 * 코호트 탐색·참여 페이지 (라우트 /explore, frontend-components §2.1).
 *
 * 모집중·진행중 코호트를 목록으로 보여주고 각 카드에서 선착순 참여를 신청한다. 참여 결과는 aria-live 토스트로 안내한다.
 */
export function ExplorePage() {
  const [cohorts, setCohorts] = useState<CohortSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<ToastMessage | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    cohortApi
      .list({})
      .then((page) => setCohorts(page.content))
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : '코호트를 불러오지 못했습니다'),
      )
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      <ResponsiveTabBar />
      <main className="mx-auto max-w-3xl p-6">
        <h1 className="text-2xl font-semibold" data-testid="explore-title">
          코호트 탐색
        </h1>

        {loading && (
          <p data-testid="explore-loading" className="mt-6 text-sm text-gray-400">
            불러오는 중...
          </p>
        )}

        {error && (
          <p role="alert" data-testid="explore-error" className="mt-6 text-sm text-red-600">
            {error}
          </p>
        )}

        {!loading && !error && cohorts.length === 0 && (
          <div
            data-testid="explore-empty"
            className="mt-6 rounded border border-dashed border-gray-300 p-8 text-center text-gray-500"
          >
            참여할 수 있는 코호트가 없습니다.
          </div>
        )}

        {!loading && cohorts.length > 0 && (
          <ul data-testid="explore-list" className="mt-6 grid gap-4">
            {cohorts.map((cohort) => (
              <li
                key={cohort.id}
                data-testid={`explore-row-${cohort.id}`}
                className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium">{cohort.title}</h3>
                    <CohortStatusBadge status={cohort.status} label={cohort.statusLabel} />
                  </div>
                  <p className="mt-1 text-sm text-gray-600">
                    정원 {cohort.capacity}명 · {cohort.sessionCount}회차
                  </p>
                </div>
                <JoinButton cohortId={cohort.id} onResult={setToast} onSuccess={load} />
              </li>
            ))}
          </ul>
        )}
      </main>
      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
