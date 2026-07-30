import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/authContext';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';
import { EvidenceHistoryTable } from './EvidenceHistoryTable';
import { MetricsOverview } from './MetricsOverview';
import { ReportHistoryTable } from './ReportHistoryTable';
import { WaitingList } from './WaitingList';

type AdminTab = 'waiting' | 'metrics' | 'evidence' | 'reports';

const TABS: { id: AdminTab; label: string }[] = [
  { id: 'waiting', label: '대기승인' },
  { id: 'metrics', label: '지표' },
  { id: 'evidence', label: '증빙 이력' },
  { id: 'reports', label: '보고서 이력' },
];

/**
 * 관리자 페이지 (라우트 /admin, frontend-components §1/§2.4, U6 §1).
 *
 * 관리자(isAdmin)만 렌더한다. 비관리자는 대시보드로 리다이렉트하며, 서버 @PreAuthorize(ADMIN)가 최종 인가 방어선이다(R-U3-11, R-U6-01).
 * 대기 승인(U3) 탭에 더해 운영 지표·증빙 이력·보고서 이력(U6) 탭을 제공한다. 증빙/보고서 이력은 별도 탭으로 분리 조회한다(FR-10).
 */
export function AdminPage() {
  const { currentUser } = useAuth();
  const [tab, setTab] = useState<AdminTab>('waiting');

  if (currentUser && !currentUser.isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      <ResponsiveTabBar />
      <main className="mx-auto max-w-3xl p-6">
        <h1 className="text-2xl font-semibold" data-testid="admin-title">
          관리자
        </h1>

        <div
          role="tablist"
          aria-label="관리자 메뉴"
          data-testid="admin-tabs"
          className="mt-6 flex gap-2 border-b border-gray-200"
        >
          {TABS.map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={tab === t.id}
              data-testid={`admin-tab-${t.id}`}
              onClick={() => setTab(t.id)}
              className={
                tab === t.id
                  ? 'border-b-2 border-accent px-3 py-2 text-sm font-medium text-accent'
                  : 'px-3 py-2 text-sm text-gray-500 hover:text-gray-700'
              }
            >
              {t.label}
            </button>
          ))}
        </div>

        <div className="mt-6" data-testid={`admin-panel-${tab}`}>
          {tab === 'waiting' && <WaitingList />}
          {tab === 'metrics' && <MetricsOverview />}
          {tab === 'evidence' && <EvidenceHistoryTable />}
          {tab === 'reports' && <ReportHistoryTable />}
        </div>
      </main>
    </div>
  );
}
