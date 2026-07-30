import { apiClient, apiUrl } from './ApiClient';
import type { CohortAttendanceDto, EvidenceDto } from './types';

/**
 * 출석·증빙 API 호출 (frontend-components §3). 모든 호출은 ApiClient(세션 쿠키·에러 정규화) 경유.
 *
 * 업로드는 멀티파트(FormData), 다운로드는 증빙 id 경유 스트리밍이다. 사용자 id 는 서버가 세션에서 해석한다(바디 미전송).
 */
export const attendanceApi = {
  /** 회차 증빙 업로드(멘토 전용) — 성공 시 회차가 인증된다. */
  uploadEvidence: (sessionId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return apiClient.postForm<EvidenceDto>(`/api/sessions/${sessionId}/evidence`, form);
  },

  /** 코호트 진도·출석 조회(참여자·관리자). */
  getAttendance: (cohortId: number) =>
    apiClient.get<CohortAttendanceDto>(`/api/cohorts/${cohortId}/attendance`),

  /** 증빙 다운로드 URL(앵커 href 용). 브라우저가 세션 쿠키를 자동 전송한다. */
  evidenceDownloadUrl: (sessionId: number, evidenceId: number) =>
    apiUrl(`/api/sessions/${sessionId}/evidence/${evidenceId}`),

  /** 증빙 blob 로드(프로그램적 다운로드/미리보기). */
  loadEvidence: (sessionId: number, evidenceId: number) =>
    apiClient.getBlob(`/api/sessions/${sessionId}/evidence/${evidenceId}`),
};
