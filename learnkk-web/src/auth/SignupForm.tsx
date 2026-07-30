import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './authContext';
import { ApiError } from '../api/ApiClient';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * 회원가입 폼 (frontend-components §2.4).
 *
 * 필드: email·name·nickname·password(+ 확인). isAdmin 필드 없음(R-U1-06).
 */
export function SignupForm() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): string | null {
    if (!EMAIL_RE.test(email)) return '이메일 형식이 올바르지 않습니다';
    if (!name.trim() || name.length > 100) return '이름을 확인하세요 (최대 100자)';
    if (!nickname.trim() || nickname.length > 50) return '닉네임을 확인하세요 (최대 50자)';
    if (password.length < 8) return '비밀번호는 최소 8자입니다';
    if (password !== passwordConfirm) return '비밀번호가 일치하지 않습니다';
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);
    try {
      await signup({ email, name, nickname, password });
      navigate('/auth?signup=success');
    } catch (err) {
      if (err instanceof ApiError && err.code === 'DUPLICATE_EMAIL') {
        setError('이미 사용 중인 이메일입니다');
      } else if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('가입 중 오류가 발생했습니다');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} data-testid="signup-form" noValidate className="space-y-4">
      <Field
        id="signup-email"
        label="이메일"
        type="email"
        value={email}
        onChange={setEmail}
        error={error}
      />
      <Field id="signup-name" label="이름" value={name} onChange={setName} error={error} />
      <Field
        id="signup-nickname"
        label="닉네임"
        value={nickname}
        onChange={setNickname}
        error={error}
      />
      <Field
        id="signup-password"
        label="비밀번호"
        type="password"
        value={password}
        onChange={setPassword}
        error={error}
      />
      <Field
        id="signup-password-confirm"
        label="비밀번호 확인"
        type="password"
        value={passwordConfirm}
        onChange={setPasswordConfirm}
        error={error}
      />
      {error && (
        <p
          id="signup-error"
          role="alert"
          data-testid="signup-error"
          className="text-sm text-red-600"
        >
          {error}
        </p>
      )}
      <button
        type="submit"
        data-testid="signup-submit"
        disabled={submitting}
        className="w-full rounded bg-accent px-4 py-2 text-white hover:bg-accent-hover disabled:opacity-50"
      >
        {submitting ? '가입 중...' : '회원가입'}
      </button>
    </form>
  );
}

interface FieldProps {
  id: string;
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  error: string | null;
}

function Field({ id, label, type = 'text', value, onChange, error }: FieldProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        data-testid={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-describedby={error ? 'signup-error' : undefined}
        className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
      />
    </div>
  );
}
