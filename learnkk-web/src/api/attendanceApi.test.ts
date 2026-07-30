import { attendanceApi } from './attendanceApi';
import { ApiError } from './ApiClient';

describe('attendanceApi', () => {
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

  it('uploadEvidence 는 멀티파트(FormData)로 POST 하고 Content-Type 을 수동 지정하지 않는다', async () => {
    mockFetch(201, {
      id: 1,
      sessionId: 5,
      mimeType: 'image/jpeg',
      size: 100,
      uploadedBy: 10,
      createdAt: '2026-01-01T00:00:00Z',
    });
    const file = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'a.jpg', { type: 'image/jpeg' });

    const dto = await attendanceApi.uploadEvidence(5, file);

    expect(dto.id).toBe(1);
    const call = (global.fetch as jest.Mock).mock.calls[0];
    expect(call[0]).toContain('/api/sessions/5/evidence');
    expect(call[1]).toMatchObject({ method: 'POST', credentials: 'include' });
    expect(call[1].body).toBeInstanceOf(FormData);
    // Content-Type 은 브라우저가 boundary 와 함께 자동 설정 — 수동 지정 금지
    expect(call[1].headers).toBeUndefined();
  });

  it('getAttendance 는 GET /api/cohorts/:id/attendance 로 호출한다', async () => {
    mockFetch(200, {
      cohortId: 5,
      verifiedCount: 1,
      totalCount: 4,
      progressRate: 0.25,
      sessions: [],
    });
    const dto = await attendanceApi.getAttendance(5);

    expect(dto.progressRate).toBe(0.25);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/cohorts/5/attendance'),
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    );
  });

  it('evidenceDownloadUrl 은 다운로드 경로 문자열을 만든다', () => {
    expect(attendanceApi.evidenceDownloadUrl(5, 9)).toContain('/api/sessions/5/evidence/9');
  });

  it('loadEvidence 는 blob 을 반환한다', async () => {
    mockFetch(200, 'binary');
    const blob = await attendanceApi.loadEvidence(5, 9);
    expect(blob).toBeInstanceOf(Blob);
  });

  it('업로드 400 응답을 ApiError(code) 로 정규화한다', async () => {
    mockFetch(400, {
      code: 'FILE_CONSTRAINT_VIOLATION',
      message: '허용되지 않는 파일 형식입니다',
      timestamp: '2026-01-01T00:00:00Z',
      path: '/api/sessions/5/evidence',
    });
    const file = new File(['x'], 'a.txt', { type: 'text/plain' });
    await expect(attendanceApi.uploadEvidence(5, file)).rejects.toMatchObject({
      code: 'FILE_CONSTRAINT_VIOLATION',
      status: 400,
    });
    expect(ApiError).toBeDefined();
  });
});
