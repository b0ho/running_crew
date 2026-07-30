import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../api/ApiClient';
import { cohortApi } from '../api/cohortApi';
import { enrollmentApi } from '../api/enrollmentApi';
import type { CohortSummaryDto, EnrollmentDto } from '../api/types';
import { useAuth } from '../auth/authContext';
import { CohortCard } from '../cohorts/CohortCard';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';

/**
 * 내 코호트 대시보드 (frontend-components §2.1). 로그인 후 첫 화면(R-U1-11).
 *
 * 내가 멘토인 코호트(/api/cohorts/mine)와 내가 멘티로 확정된 참여(/api/me/enrollments 중 CONFIRMED)를 함께 보여준다.
 */
export function DashboardPage() {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [cohorts, setCohorts] = useState<CohortSummaryDto[]>([]);
  const [confirmed, setConfirmed] = useState<EnrollmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([cohortApi.getMine(), enrollmentApi.myApplications({ size: 100 })])
      .then(([mine, applications]) => {
        if (!active) return;
        setCohorts(mine);
        setConfirmed(applications.content.filter((a) => a.status === 'CONFIRMED'));
      })
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.message : '코호트를 불러오지 못했습니다');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      <ResponsiveTabBar />
      <main className="mx-auto max-w-3xl p-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold" data-testid="dashboard-title">
            내 코호트 대시보드
          </h1>
          <button
            type="button"
            data-testid="create-cohort"
            onClick={() => navigate('/cohorts/new')}
            className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent-hover"
          >
            코호트 개설
          </button>
        </div>
        <p className="mt-2 text-gray-600" data-testid="dashboard-welcome">
          {currentUser?.name}님, 환영합니다.
        </p>

        {loading && (
          <p data-testid="dashboard-loading" className="mt-6 text-sm text-gray-400">
            불러오는 중...
          </p>
        )}

        {error && (
          <p role="alert" data-testid="dashboard-error" className="mt-6 text-sm text-red-600">
            {error}
          </p>
        )}

        {!loading && !error && cohorts.length === 0 && (
          <div data-testid="dashboard-empty" className="mt-6 rounded border border-dashed border-gray-300 p-8 text-center text-gray-500">
            아직 개설한 코호트가 없습니다. 코호트를 탐색해보세요.
          </div>
        )}

        {!loading && cohorts.length > 0 && (
          <div data-testid="cohort-card-list" className="mt-6 grid gap-4">
            {cohorts.map((c) => (
              <CohortCard key={c.id} cohort={c} />
            ))}
          </div>
        )}

        {!loading && !error && confirmed.length > 0 && (
          <section className="mt-10">
            <h2 className="text-lg font-semibold" data-testid="confirmed-title">
              내가 참여 확정된 코호트
            </h2>
            <ul data-testid="confirmed-list" className="mt-4 grid gap-3">
              {confirmed.map((app) => (
                <li
                  key={app.id}
                  data-testid={`confirmed-row-${app.id}`}
                  className="rounded-lg border border-gray-200 bg-white p-4"
                >
                  <h3 className="font-medium">{app.cohortTitle ?? '코호트'}</h3>
                  <p className="mt-1 text-sm text-gray-500">참여 확정</p>
                </li>
              ))}
            </ul>
          </section>
        )}
      </main>
    </div>
  );
}
