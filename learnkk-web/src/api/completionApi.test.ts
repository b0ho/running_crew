import { completionApi } from './completionApi';
import { ApiError } from './ApiClient';

describe('completionApi', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  function mockFetch(status: number, body: unknown) {
    global.fetch = jest.fn().mockResolvedValue({
      ok: status >= 200 && status < 300,
      status,
      text: async () => (body === undefined ? '' : JSON.stringify(body)),
      blob: async () => new Blob([JSON.stringify(body)]),
    }) as unknown as typeof fetch;
  }

  it('endCohort 는 POST /api/cohorts/:id/end 로 호출하고 요약을 반환한다', async () => {
    mockFetch(200, {
      certifiedCount: 3,
      notCertifiedCount: 0,
      totalConfirmed: 3,
      settlementSatisfied: true,
      issuedCertificateCount: 3,
    });

    const summary = await completionApi.endCohort(7);

    expect(summary.certifiedCount).toBe(3);
    expect(summary.settlementSatisfied).toBe(true);
    const call = (global.fetch as jest.Mock).mock.calls[0];
    expect(call[0]).toContain('/api/cohorts/7/end');
    expect(call[1]).toMatchObject({ method: 'POST', credentials: 'include' });
  });

  it('submitReport 는 멀티파트(FormData)로 body(+선택 file)를 POST 한다', async () => {
    mockFetch(201, {
      id: 1,
      cohortId: 7,
      authorId: 10,
      body: '본문',
      hasAttachment: true,
      submittedAt: '2026-03-01T00:00:00Z',
    });
    const file = new File([new Uint8Array([1, 2, 3])], 'r.pdf', { type: 'application/pdf' });

    const dto = await completionApi.submitReport(7, '본문', file);

    expect(dto.id).toBe(1);
    expect(dto.hasAttachment).toBe(true);
    const call = (global.fetch as jest.Mock).mock.calls[0];
    expect(call[0]).toContain('/api/cohorts/7/reports');
    expect(call[1]).toMatchObject({ method: 'POST', credentials: 'include' });
    expect(call[1].body).toBeInstanceOf(FormData);
    const form = call[1].body as FormData;
    expect(form.get('body')).toBe('본문');
    expect(form.get('file')).toBeInstanceOf(File);
  });

  it('submitReport 는 첨부가 없으면 file 파트를 포함하지 않는다', async () => {
    mockFetch(201, {
      id: 2,
      cohortId: 7,
      authorId: 10,
      body: '본문',
      hasAttachment: false,
      submittedAt: '2026-03-01T00:00:00Z',
    });

    await completionApi.submitReport(7, '본문');

    const form = (global.fetch as jest.Mock).mock.calls[0][1].body as FormData;
    expect(form.get('body')).toBe('본문');
    expect(form.get('file')).toBeNull();
  });

  it('listReports 는 GET /api/cohorts/:id/reports 페이지 파라미터로 호출한다', async () => {
    mockFetch(200, { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    await completionApi.listReports(7);

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/cohorts/7/reports?page=0&size=20'),
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    );
  });

  it('getCertificate 는 blob 을 반환한다', async () => {
    mockFetch(200, 'binary');
    const blob = await completionApi.getCertificate(7);
    expect(blob).toBeInstanceOf(Blob);
  });

  it('getCertificate 는 404 를 ApiError(NOT_FOUND) 로 정규화한다(미수료)', async () => {
    mockFetch(404, {
      code: 'NOT_FOUND',
      message: '수료증을 찾을 수 없습니다',
      timestamp: '2026-03-01T00:00:00Z',
      path: '/api/cohorts/7/certificate',
    });
    await expect(completionApi.getCertificate(7)).rejects.toMatchObject({
      code: 'NOT_FOUND',
      status: 404,
    });
    expect(ApiError).toBeDefined();
  });
});
