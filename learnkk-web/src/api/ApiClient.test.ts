import { apiClient, ApiError } from './ApiClient';

describe('ApiClient 에러 정규화', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  function mockFetch(status: number, body: unknown) {
    global.fetch = jest.fn().mockResolvedValue({
      ok: status >= 200 && status < 300,
      status,
      text: async () => (body === undefined ? '' : JSON.stringify(body)),
    }) as unknown as typeof fetch;
  }

  it('성공 응답 본문을 반환한다', async () => {
    mockFetch(200, { id: 1, email: 'a@b.com' });
    const res = await apiClient.get<{ id: number }>('/api/auth/me');
    expect(res.id).toBe(1);
  });

  it('에러 응답을 ApiError(code) 로 정규화한다', async () => {
    mockFetch(409, {
      code: 'DUPLICATE_EMAIL',
      message: '이미 사용 중인 이메일입니다',
      timestamp: '2026-01-01T00:00:00Z',
      path: '/api/auth/signup',
    });

    await expect(apiClient.post('/api/auth/signup', {})).rejects.toMatchObject({
      code: 'DUPLICATE_EMAIL',
      status: 409,
    });
  });

  it('credentials include 로 호출한다', async () => {
    mockFetch(204, undefined);
    await apiClient.post('/api/auth/logout');
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/logout'),
      expect.objectContaining({ credentials: 'include' }),
    );
  });

  it('네트워크 실패를 NETWORK_ERROR 로 정규화한다', async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error('down')) as unknown as typeof fetch;
    await expect(apiClient.get('/api/auth/me')).rejects.toBeInstanceOf(ApiError);
  });
});
