import type { ErrorResponse } from './types';

/**
 * API 에러 — 공통 에러 DTO(code·message)를 정규화해 담는다.
 * 컴포넌트는 err.code 로 분기(예: DUPLICATE_EMAIL, INVALID_CREDENTIALS)한다.
 */
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

// 동일 오리진 배포(nginx/Vite 가 /api 를 백엔드로 프록시)를 기본으로 한다.
// 필요 시 window.__LEARNKK_API_BASE__ 로 런타임 오버라이드(import.meta 미사용 → 테스트 안전).
declare global {
  interface Window {
    __LEARNKK_API_BASE__?: string;
  }
}

const BASE_URL =
  (typeof window !== 'undefined' && window.__LEARNKK_API_BASE__) || '';

/**
 * 중앙 API 클라이언트 (frontend-components §2.1).
 *
 * - credentials: 'include' — 세션 쿠키 전송(FE/BE 분리 + CORS)
 * - 응답 에러를 ApiError 로 정규화해 throw (컴포넌트 산발 try-catch 지양)
 */
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers ?? {}),
      },
    });
  } catch {
    throw new ApiError('NETWORK_ERROR', '서버에 연결할 수 없습니다', 0);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const body = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    const err = body as ErrorResponse | undefined;
    throw new ApiError(
      err?.code ?? 'INTERNAL_ERROR',
      err?.message ?? '요청을 처리하는 중 오류가 발생했습니다',
      response.status,
    );
  }

  return body as T;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, payload?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: payload === undefined ? undefined : JSON.stringify(payload),
    }),
};
