import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './RequireAuth';
import { AuthProvider } from './AuthProvider';
import { authApi } from '../api/authApi';
import { ApiError } from '../api/ApiClient';

jest.mock('../api/authApi', () => ({
  authApi: {
    me: jest.fn(),
    login: jest.fn(),
    signup: jest.fn(),
    logout: jest.fn(),
  },
}));

function renderWithRoutes() {
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <AuthProvider>
        <Routes>
          <Route path="/auth" element={<div data-testid="auth-page">로그인 화면</div>} />
          <Route
            path="/protected"
            element={
              <RequireAuth>
                <div data-testid="protected">보호 콘텐츠</div>
              </RequireAuth>
            }
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('RequireAuth', () => {
  beforeEach(() => jest.clearAllMocks());

  it('미인증(401)이면 /auth 로 리다이렉트', async () => {
    (authApi.me as jest.Mock).mockRejectedValue(new ApiError('UNAUTHORIZED', '인증 필요', 401));
    renderWithRoutes();
    await waitFor(() => expect(screen.getByTestId('auth-page')).toBeInTheDocument());
    expect(screen.queryByTestId('protected')).not.toBeInTheDocument();
  });

  it('인증되면 보호 콘텐츠 렌더', async () => {
    (authApi.me as jest.Mock).mockResolvedValue({
      id: 1,
      email: 'a@learnkk.local',
      name: '앨리스',
      nickname: 'al',
      isAdmin: false,
    });
    renderWithRoutes();
    await waitFor(() => expect(screen.getByTestId('protected')).toBeInTheDocument());
  });
});
