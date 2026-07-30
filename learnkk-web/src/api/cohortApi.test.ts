import { cohortApi } from './cohortApi';
import { ApiError } from './ApiClient';

describe('cohortApi', () => {
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

  it('create 는 POST /api/cohorts 로 호출한다', async () => {
    mockFetch(201, { id: 1, title: '자바 멘토링', status: 'RECRUITING', warnings: [] });
    await cohortApi.create({
      title: '자바 멘토링',
      capacity: 20,
      startDate: '2026-01-01',
      endDate: '2026-03-01',
      sessionCount: 6,
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/cohorts'),
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    );
  });

  it('update 는 PUT /api/cohorts/:id 로 호출한다', async () => {
    mockFetch(200, { id: 5, warnings: [] });
    await cohortApi.update(5, {
      title: '수정',
      capacity: 10,
      startDate: '2026-01-01',
      endDate: '2026-03-01',
      sessionCount: 5,
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/cohorts/5'),
      expect.objectContaining({ method: 'PUT' }),
    );
  });

  it('list 는 상태·키워드·페이지 쿼리스트링을 직렬화한다', async () => {
    mockFetch(200, { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true });
    await cohortApi.list({ status: 'ONGOING', keyword: '자바', page: 1, size: 10 });
    const calledUrl = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(calledUrl).toContain('status=ONGOING');
    expect(calledUrl).toContain('keyword=%EC%9E%90%EB%B0%94');
    expect(calledUrl).toContain('page=1');
    expect(calledUrl).toContain('size=10');
  });

  it('409 응답을 ApiError(code) 로 정규화한다', async () => {
    mockFetch(409, {
      code: 'CAPACITY_BELOW_CONFIRMED',
      message: '정원을 확정 인원 미만으로 축소할 수 없습니다',
      timestamp: '2026-01-01T00:00:00Z',
      path: '/api/cohorts/1',
    });
    await expect(
      cohortApi.update(1, {
        title: '수정',
        capacity: 1,
        startDate: '2026-01-01',
        endDate: '2026-03-01',
        sessionCount: 5,
      }),
    ).rejects.toMatchObject({ code: 'CAPACITY_BELOW_CONFIRMED', status: 409 });
  });

  it('createAnnouncement 는 하위 경로로 POST 한다', async () => {
    mockFetch(201, { id: 1, cohortId: 3, body: '공지', externalLink: null, createdAt: 'x' });
    await cohortApi.createAnnouncement(3, { body: '공지', externalLink: null });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/cohorts/3/announcements'),
      expect.objectContaining({ method: 'POST' }),
    );
    // ApiError 가 아닌 정상 응답
    expect(cohortApi.createAnnouncement).toBeDefined();
    expect(ApiError).toBeDefined();
  });
});
