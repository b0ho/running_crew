import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { LoginForm } from './LoginForm';
import { SignupForm } from './SignupForm';

type Tab = 'login' | 'signup';

/** 인증 화면 — 로그인/회원가입 탭 전환 (frontend-components §2.3/2.4). */
export function AuthPage() {
  const [searchParams] = useSearchParams();
  const [tab, setTab] = useState<Tab>('login');
  const signupSuccess = searchParams.get('signup') === 'success';

  return (
    <div className="mx-auto mt-16 max-w-sm rounded-lg border border-gray-200 p-6">
      <h1 className="mb-4 text-center text-xl font-semibold">LearnKK</h1>

      <div className="mb-6 flex" role="tablist">
        <button
          role="tab"
          data-testid="tab-login"
          aria-selected={tab === 'login'}
          onClick={() => setTab('login')}
          className={`flex-1 border-b-2 py-2 ${
            tab === 'login' ? 'border-accent font-medium' : 'border-gray-200 text-gray-500'
          }`}
        >
          로그인
        </button>
        <button
          role="tab"
          data-testid="tab-signup"
          aria-selected={tab === 'signup'}
          onClick={() => setTab('signup')}
          className={`flex-1 border-b-2 py-2 ${
            tab === 'signup' ? 'border-accent font-medium' : 'border-gray-200 text-gray-500'
          }`}
        >
          회원가입
        </button>
      </div>

      {signupSuccess && tab === 'login' && (
        <p data-testid="signup-success" className="mb-4 text-sm text-green-600">
          가입이 완료되었습니다. 로그인하세요.
        </p>
      )}

      {tab === 'login' ? <LoginForm /> : <SignupForm />}
    </div>
  );
}
