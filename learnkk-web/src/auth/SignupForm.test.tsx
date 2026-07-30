import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { SignupForm } from './SignupForm';
import { AuthProvider } from './AuthProvider';
import { authApi } from '../api/authApi';
import { ApiError } from '../api/ApiClient';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../api/authApi', () => ({
  authApi: {
    me: jest.fn().mockRejectedValue(new Error('anon')),
    login: jest.fn(),
    signup: jest.fn(),
    logout: jest.fn(),
  },
}));

function renderForm() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <SignupForm />
      </AuthProvider>
    </MemoryRouter>,
  );
}

async function fillValid() {
  await userEvent.type(screen.getByTestId('signup-email'), 'new@learnkk.local');
  await userEvent.type(screen.getByTestId('signup-name'), '홍길동');
  await userEvent.type(screen.getByTestId('signup-nickname'), 'gil');
  await userEvent.type(screen.getByTestId('signup-password'), 'password123');
  await userEvent.type(screen.getByTestId('signup-password-confirm'), 'password123');
}

describe('SignupForm', () => {
  beforeEach(() => jest.clearAllMocks());

  it('비밀번호 8자 미만이면 검증 오류', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <SignupForm />
        </AuthProvider>
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByTestId('signup-email'), 'new@learnkk.local');
    await userEvent.type(screen.getByTestId('signup-name'), '홍길동');
    await userEvent.type(screen.getByTestId('signup-nickname'), 'gil');
    await userEvent.type(screen.getByTestId('signup-password'), 'short');
    await userEvent.type(screen.getByTestId('signup-password-confirm'), 'short');
    await userEvent.click(screen.getByTestId('signup-submit'));

    expect(screen.getByTestId('signup-error')).toHaveTextContent('최소 8자');
    expect(authApi.signup).not.toHaveBeenCalled();
  });

  it('비밀번호 확인 불일치면 검증 오류', async () => {
    renderForm();
    await userEvent.type(screen.getByTestId('signup-email'), 'new@learnkk.local');
    await userEvent.type(screen.getByTestId('signup-name'), '홍길동');
    await userEvent.type(screen.getByTestId('signup-nickname'), 'gil');
    await userEvent.type(screen.getByTestId('signup-password'), 'password123');
    await userEvent.type(screen.getByTestId('signup-password-confirm'), 'password999');
    await userEvent.click(screen.getByTestId('signup-submit'));

    expect(screen.getByTestId('signup-error')).toHaveTextContent('일치하지 않습니다');
  });

  it('409 이면 중복 이메일 문구', async () => {
    (authApi.signup as jest.Mock).mockRejectedValue(
      new ApiError('DUPLICATE_EMAIL', '이미 사용 중인 이메일입니다', 409),
    );
    renderForm();
    await fillValid();
    await userEvent.click(screen.getByTestId('signup-submit'));

    await waitFor(() =>
      expect(screen.getByTestId('signup-error')).toHaveTextContent('이미 사용 중인 이메일입니다'),
    );
  });

  it('가입 성공 시 로그인 화면으로 이동', async () => {
    (authApi.signup as jest.Mock).mockResolvedValue({
      id: 2,
      email: 'new@learnkk.local',
      name: '홍길동',
      nickname: 'gil',
      isAdmin: false,
    });
    renderForm();
    await fillValid();
    await userEvent.click(screen.getByTestId('signup-submit'));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/auth?signup=success'));
  });
});
