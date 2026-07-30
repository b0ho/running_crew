import { useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { enrollmentApi } from '../api/enrollmentApi';
import type { EnrollmentDto } from '../api/types';
import { EnrollmentStatusBadge } from '../cohorts/StatusBadge';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';

/**
 * 내 신청 상태 페이지 (라우트 /my/applications, frontend-components §2.2).
 *
 * 세션 사용자 스코프로 본인 신청(대기중/확정/거절)을 최신순으로 보여준다. 승인/거절 결과는 알림으로 통지되며 재조회 시 반영된다.
 */
export function MyApplicationsPage() {
  const [applications, setApplications] = useState<EnrollmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    enrollmentApi
      .myApplications({})
      .then((page) => {
        if (active) setApplications(page.content);
      })
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.message : '신청 목록을 불러오지 못했습니다');
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
        <h1 className="text-2xl font-semibold" data-testid="my-applications-title">
          내 신청 상태
        </h1>

        {loading && (
          <p data-testid="my-applications-loading" className="mt-6 text-sm text-gray-400">
            불러오는 중...
          </p>
        )}

        {error && (
          <p role="alert" data-testid="my-applications-error" className="mt-6 text-sm text-red-600">
            {error}
          </p>
        )}

        {!loading && !error && applications.length === 0 && (
          <div
            data-testid="my-applications-empty"
            className="mt-6 rounded border border-dashed border-gray-300 p-8 text-center text-gray-500"
          >
            아직 신청한 코호트가 없습니다.
          </div>
        )}

        {!loading && applications.length > 0 && (
          <ul data-testid="my-applications-list" className="mt-6 grid gap-3">
            {applications.map((app) => (
              <li
                key={app.id}
                data-testid={`application-row-${app.id}`}
                className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4"
              >
                <div>
                  <h3 className="font-medium">{app.cohortTitle ?? '코호트'}</h3>
                  <p className="mt-1 text-sm text-gray-500">신청일 {app.createdAt.slice(0, 10)}</p>
                </div>
                <EnrollmentStatusBadge status={app.status} label={app.statusLabel} />
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
