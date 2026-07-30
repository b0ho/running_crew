import { createContext, useContext } from 'react';
import type { LoginRequest, SignupRequest, UserDto } from '../api/types';

export type AuthStatus = 'loading' | 'authed' | 'anon';

export interface AuthContextValue {
  currentUser: UserDto | null;
  status: AuthStatus;
  signup: (req: SignupRequest) => Promise<UserDto>;
  login: (req: LoginRequest) => Promise<UserDto>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

/** 인증 컨텍스트 훅 — AuthProvider 내부에서만 사용. */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth 는 AuthProvider 내부에서만 사용할 수 있습니다');
  }
  return ctx;
}
