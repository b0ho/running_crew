import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '../api/authApi';
import type { LoginRequest, SignupRequest, UserDto } from '../api/types';
import { AuthContext } from './authContext';
import type { AuthStatus } from './authContext';

/**
 * 인증 컨텍스트 프로바이더 (frontend-components §2.2).
 *
 * 부팅 시 GET /api/auth/me 로 세션을 복원한다. 훅(useAuth)·타입은 authContext.ts 에 분리.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<UserDto | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');

  useEffect(() => {
    let active = true;
    authApi
      .me()
      .then((user) => {
        if (active) {
          setCurrentUser(user);
          setStatus('authed');
        }
      })
      .catch(() => {
        // 401 은 미인증 상태(정상). 그 외 오류도 anon 으로 취급해 앱이 계속 동작하게 한다.
        if (active) {
          setCurrentUser(null);
          setStatus('anon');
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const signup = useCallback(async (req: SignupRequest) => {
    return authApi.signup(req);
  }, []);

  const login = useCallback(async (req: LoginRequest) => {
    const user = await authApi.login(req);
    setCurrentUser(user);
    setStatus('authed');
    return user;
  }, []);

  const logout = useCallback(async () => {
    await authApi.logout();
    setCurrentUser(null);
    setStatus('anon');
  }, []);

  const value = useMemo(
    () => ({ currentUser, status, signup, login, logout }),
    [currentUser, status, signup, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
