import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LoginForm } from './LoginForm';
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
        <LoginForm />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('LoginForm', () => {
  beforeEach(() => jest.clearAllMocks());

  it('잘못된 이메일 형식이면 클라이언트 검증 오류', async () => {
    renderForm();
    await userEvent.type(screen.getByTestId('login-email'), 'not-email');
    await userEvent.type(screen.getByTestId('login-password'), 'password123');
    await userEvent.click(screen.getByTestId('login-submit'));

    expect(screen.getByTestId('login-error')).toHaveTextContent('이메일 형식');
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('로그인 성공 시 대시보드로 이동', async () => {
    (authApi.login as jest.Mock).mockResolvedValue({
      id: 1,
      email: 'a@learnkk.local',
      name: '앨리스',
      nickname: 'al',
      isAdmin: false,
    });
    renderForm();
    await userEvent.type(screen.getByTestId('login-email'), 'a@learnkk.local');
    await userEvent.type(screen.getByTestId('login-password'), 'password123');
    await userEvent.click(screen.getByTestId('login-submit'));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/dashboard'));
  });

  it('401 이면 사용자 열거 방지 동일 문구', async () => {
    (authApi.login as jest.Mock).mockRejectedValue(
      new ApiError('INVALID_CREDENTIALS', '이메일 또는 비밀번호가 올바르지 않습니다', 401),
    );
    renderForm();
    await userEvent.type(screen.getByTestId('login-email'), 'a@learnkk.local');
    await userEvent.type(screen.getByTestId('login-password'), 'wrongpass');
    await userEvent.click(screen.getByTestId('login-submit'));

    await waitFor(() =>
      expect(screen.getByTestId('login-error')).toHaveTextContent(
        '이메일 또는 비밀번호가 올바르지 않습니다',
      ),
    );
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
