import { apiClient, toQueryString } from './ApiClient';
import type { EnrollmentDto, JoinResultDto, Page } from './types';

/** 참여 API 호출 (frontend-components §3). 모든 호출은 ApiClient(세션 쿠키·에러 정규화) 경유. */
export const enrollmentApi = {
  /** 선착순 참여 신청 — 확정/대기 결과를 반환. 사용자 id 는 서버가 세션에서 해석한다(바디 미전송). */
  join: (cohortId: number) =>
    apiClient.post<JoinResultDto>(`/api/cohorts/${cohortId}/enrollments`),

  /** 내 신청 목록 — 세션 사용자 스코프. */
  myApplications: (params: { page?: number; size?: number } = {}) =>
    apiClient.get<Page<EnrollmentDto>>(`/api/me/enrollments${toQueryString(params)}`),
};
