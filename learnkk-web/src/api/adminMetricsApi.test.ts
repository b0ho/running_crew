import { adminMetricsApi } from './adminMetricsApi';

describe('adminMetricsApi', () => {
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

  it('getMetrics 는 GET /api/admin/metrics 로 호출한다', async () => {
    mockFetch(200, {
      completedCohortCount: 2,
      attendanceRate: 90,
      completionRate: 100,
      certificateCount: 5,
      scopeLabel: '종료된 코호트 2건 기준',
    });

    const dto = await adminMetricsApi.getMetrics();

    expect(dto.completedCohortCount).toBe(2);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/admin/metrics'),
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    );
  });

  it('listEvidenceHistory 는 cohortId·page 쿼리 파라미터를 붙인다', async () => {
    mockFetch(200, { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    await adminMetricsApi.listEvidenceHistory({ cohortId: 7, page: 1 });

    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain('/api/admin/history/evidence');
    expect(url).toContain('cohortId=7');
    expect(url).toContain('page=1');
  });

  it('listEvidenceHistory 는 파라미터 없이도 기본 경로로 호출한다', async () => {
    mockFetch(200, { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    await adminMetricsApi.listEvidenceHistory();

    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain('/api/admin/history/evidence');
    expect(url).not.toContain('cohortId=');
  });

  it('listReportHistory 는 GET /api/admin/history/reports 로 호출한다', async () => {
    mockFetch(200, { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    await adminMetricsApi.listReportHistory({ page: 2 });

    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain('/api/admin/history/reports');
    expect(url).toContain('page=2');
  });
});
