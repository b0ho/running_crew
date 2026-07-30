import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/authContext';
import { NotificationBell } from '../enrollment/NotificationBell';

/**
 * 반응형 탭 바 공통 셸 (frontend-components §2.6, cid:rough-mockups:c1).
 *
 * 데스크톱 상단(md+), 모바일 하단(bottom) 으로 전환. 좌측 사이드바 미사용. 주요 이동(대시보드/탐색/내 신청/관리자)과 알림 벨을 제공한다. 관리자 링크는
 * 관리자에게만 노출하되 서버 인가가 최종 방어선이다.
 */
export function ResponsiveTabBar() {
  const { currentUser, logout } = useAuth();

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm ${isActive ? 'font-semibold text-accent' : 'text-gray-500 hover:text-gray-800'}`;

  return (
    <nav
      data-testid="tab-bar"
      className="fixed inset-x-0 bottom-0 flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 md:static md:border-b md:border-t-0"
    >
      <div className="flex items-center gap-4">
        <span className="font-semibold text-accent">LearnKK</span>
        <NavLink to="/dashboard" data-testid="nav-dashboard" className={linkClass}>
          대시보드
        </NavLink>
        <NavLink to="/explore" data-testid="nav-explore" className={linkClass}>
          탐색
        </NavLink>
        <NavLink to="/my/applications" data-testid="nav-my-applications" className={linkClass}>
          내 신청
        </NavLink>
        {currentUser?.isAdmin && (
          <NavLink to="/admin" data-testid="nav-admin" className={linkClass}>
            관리자
          </NavLink>
        )}
      </div>
      <div className="flex items-center gap-4 text-sm">
        <NotificationBell />
        <span data-testid="current-nickname">{currentUser?.nickname}</span>
        <button
          data-testid="logout-button"
          onClick={() => logout()}
          className="text-gray-500 hover:text-gray-800"
        >
          로그아웃
        </button>
      </div>
    </nav>
  );
}
