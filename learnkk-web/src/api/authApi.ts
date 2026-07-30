import { apiClient } from './ApiClient';
import type { LoginRequest, SignupRequest, UserDto } from './types';

/** 인증 API 호출 (frontend-components §3). */
export const authApi = {
  signup: (req: SignupRequest) => apiClient.post<UserDto>('/api/auth/signup', req),
  login: (req: LoginRequest) => apiClient.post<UserDto>('/api/auth/login', req),
  me: () => apiClient.get<UserDto>('/api/auth/me'),
  logout: () => apiClient.post<void>('/api/auth/logout'),
};
