import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './authContext';
import { ApiError } from '../api/ApiClient';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// R-U1-09 — 미존재/불일치 동일 문구(사용자 열거 방지)
const INVALID_MESSAGE = '이메일 또는 비밀번호가 올바르지 않습니다';

/** 로그인 폼 (frontend-components §2.3). */
export function LoginForm() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!EMAIL_RE.test(email)) {
      setError('이메일 형식이 올바르지 않습니다');
      return;
    }
    if (!password) {
      setError('비밀번호를 입력하세요');
      return;
    }

    setSubmitting(true);
    try {
      await login({ email, password });
      navigate('/dashboard');
    } catch (err) {
      if (err instanceof ApiError && err.code === 'INVALID_CREDENTIALS') {
        setError(INVALID_MESSAGE);
      } else {
        setError(err instanceof ApiError ? err.message : INVALID_MESSAGE);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} data-testid="login-form" noValidate className="space-y-4">
      <div>
        <label htmlFor="login-email" className="block text-sm font-medium">
          이메일
        </label>
        <input
          id="login-email"
          data-testid="login-email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-describedby={error ? 'login-error' : undefined}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
        />
      </div>
      <div>
        <label htmlFor="login-password" className="block text-sm font-medium">
          비밀번호
        </label>
        <input
          id="login-password"
          data-testid="login-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-describedby={error ? 'login-error' : undefined}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
        />
      </div>
      {error && (
        <p id="login-error" role="alert" data-testid="login-error" className="text-sm text-red-600">
          {error}
        </p>
      )}
      <button
        type="submit"
        data-testid="login-submit"
        disabled={submitting}
        className="w-full rounded bg-accent px-4 py-2 text-white hover:bg-accent-hover disabled:opacity-50"
      >
        {submitting ? '로그인 중...' : '로그인'}
      </button>
    </form>
  );
}
