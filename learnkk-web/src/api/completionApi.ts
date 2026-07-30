import { apiClient, apiUrl } from './ApiClient';
import type { CohortEndSummaryDto, Page, ReportDto } from './types';

/**
 * 코호트 종료·최종 보고서·수료증 API 호출 (frontend-components §3, U5). 모든 호출은 ApiClient(세션 쿠키·에러 정규화) 경유.
 *
 * 종료 엔드포인트는 오케스트레이션 소유자인 U5 가 노출한다. 보고서 제출은 멀티파트(본문 + 선택 첨부), 수료증은 blob 스트리밍이다. 사용자 id 는 서버가 세션에서
 * 해석한다(바디 미전송).
 */
export const completionApi = {
  /** 코호트 종료(소유 멘토) — 수료 판정·수료증·정산·상태 전이·알림을 원자적으로 수행하고 요약을 반환한다. */
  endCohort: (cohortId: number) =>
    apiClient.post<CohortEndSummaryDto>(`/api/cohorts/${cohortId}/end`),

  /** 최종 보고서 제출(참여자) — 본문 필수, 파일 첨부 선택(멀티파트). */
  submitReport: (cohortId: number, body: string, file?: File | null) => {
    const form = new FormData();
    form.append('body', body);
    if (file) {
      form.append('file', file);
    }
    return apiClient.postForm<ReportDto>(`/api/cohorts/${cohortId}/reports`, form);
  },

  /** 보고서 이력 조회(참여자·관리자, 최신순). */
  listReports: (cohortId: number, page = 0, size = 20) =>
    apiClient.get<Page<ReportDto>>(`/api/cohorts/${cohortId}/reports?page=${page}&size=${size}`),

  /** 수료증 blob 로드(프로그램적 다운로드). 미수료·미발급이면 ApiError(NOT_FOUND). */
  getCertificate: (cohortId: number) => apiClient.getBlob(`/api/cohorts/${cohortId}/certificate`),

  /** 수료증 다운로드 URL(앵커 href 용). 브라우저가 세션 쿠키를 자동 전송한다. */
  certificateDownloadUrl: (cohortId: number) => apiUrl(`/api/cohorts/${cohortId}/certificate`),
};
