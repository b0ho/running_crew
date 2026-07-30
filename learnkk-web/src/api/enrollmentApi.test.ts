import { enrollmentApi } from './enrollmentApi';
import { ApiError } from './ApiClient';

describe('enrollmentApi', () => {
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

  it('join 은 POST /api/cohorts/:id/enrollments 로 호출한다(바디 미전송)', async () => {
    mockFetch(201, {
      enrollmentId: 1,
      cohortId: 5,
      status: 'CONFIRMED',
      statusLabel: '확정',
      waitingPosition: null,
    });
    const result = await enrollmentApi.join(5);
    expect(result.status).toBe('CONFIRMED');
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/cohorts/5/enrollments'),
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    );
  });

  it('myApplications 는 GET /api/me/enrollments 로 페이지 파라미터를 직렬화한다', async () => {
    mockFetch(200, {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
      first: true,
      last: true,
    });
    await enrollmentApi.myApplications({ page: 1, size: 10 });
    const calledUrl = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(calledUrl).toContain('/api/me/enrollments');
    expect(calledUrl).toContain('page=1');
    expect(calledUrl).toContain('size=10');
  });

  it('409 응답을 ApiError(code) 로 정규화한다', async () => {
    mockFetch(409, {
      code: 'ENROLLMENT_BUSY',
      message: '신청이 몰려 잠시 후 다시 시도해 주세요',
      timestamp: '2026-01-01T00:00:00Z',
      path: '/api/cohorts/5/enrollments',
    });
    await expect(enrollmentApi.join(5)).rejects.toMatchObject({
      code: 'ENROLLMENT_BUSY',
      status: 409,
    });
    expect(ApiError).toBeDefined();
  });
});
