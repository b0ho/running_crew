import { useAuth } from '../auth/authContext';

/**
 * 반응형 탭 바 공통 셸 (frontend-components §2.6, cid:rough-mockups:c1).
 *
 * 데스크톱 상단(md+), 모바일 하단(bottom) 으로 전환. 좌측 사이드바 미사용. U1 에서는 셸 골격만 제공.
 */
export function ResponsiveTabBar() {
  const { currentUser, logout } = useAuth();

  return (
    <nav
      data-testid="tab-bar"
      className="fixed inset-x-0 bottom-0 flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 md:static md:border-b md:border-t-0"
    >
      <span className="font-semibold text-accent">LearnKK</span>
      <div className="flex items-center gap-4 text-sm">
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
