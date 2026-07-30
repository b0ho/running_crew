import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './authContext';

/**
 * 보호 라우트 가드 (frontend-components §2.5).
 *
 * 미인증(status='anon')이면 /auth 로 리다이렉트한다. 로딩 중에는 플레이스홀더를 렌더한다.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth();

  if (status === 'loading') {
    return (
      <div data-testid="auth-loading" className="p-8 text-center text-gray-500">
        불러오는 중...
      </div>
    );
  }

  if (status === 'anon') {
    return <Navigate to="/auth" replace />;
  }

  return <>{children}</>;
}
