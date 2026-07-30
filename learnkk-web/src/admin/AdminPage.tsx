import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/authContext';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';
import { WaitingList } from './WaitingList';

/**
 * 관리자 페이지 (라우트 /admin, frontend-components §2.4).
 *
 * 관리자(isAdmin)만 렌더한다. 비관리자는 대시보드로 리다이렉트하며, 서버 @PreAuthorize(ADMIN)가 최종 인가 방어선이다(R-U3-11). 현재는 대기
 * 승인 탭만 제공한다.
 */
export function AdminPage() {
  const { currentUser } = useAuth();

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
        <div className="mt-6">
          <WaitingList />
        </div>
      </main>
    </div>
  );
}
