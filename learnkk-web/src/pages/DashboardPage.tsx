import { useAuth } from '../auth/authContext';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';

/**
 * 내 코호트 대시보드 플레이스홀더 (frontend-components §2.6).
 *
 * 워킹 스켈레톤의 로그인 후 목적지(R-U1-11). 실제 코호트 목록은 후속 유닛(U2~)에서 채운다.
 */
export function DashboardPage() {
  const { currentUser } = useAuth();

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      <ResponsiveTabBar />
      <main className="mx-auto max-w-3xl p-6">
        <h1 className="text-2xl font-semibold" data-testid="dashboard-title">
          내 코호트 대시보드
        </h1>
        <p className="mt-2 text-gray-600" data-testid="dashboard-welcome">
          {currentUser?.name}님, 환영합니다.
        </p>
        {currentUser?.isAdmin && (
          <p data-testid="admin-badge" className="mt-4 inline-block rounded bg-accent/10 px-3 py-1 text-sm text-accent">
            관리자 계정
          </p>
        )}
        <p className="mt-6 text-sm text-gray-400">
          코호트 목록은 다음 단계(U2)에서 제공됩니다.
        </p>
      </main>
    </div>
  );
}
