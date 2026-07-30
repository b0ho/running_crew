import { apiClient, toQueryString } from './ApiClient';
import type { Page, WaitingEnrollmentDto } from './types';

/** 관리자 대기 승인 API 호출 (frontend-components §2.4). 서버가 @PreAuthorize(ADMIN)로 최종 인가한다. */
export const adminApi = {
  listWaiting: (params: { cohortId?: number; page?: number; size?: number } = {}) =>
    apiClient.get<Page<WaitingEnrollmentDto>>(
      `/api/admin/enrollments/waiting${toQueryString(params)}`,
    ),

  approve: (enrollmentId: number) =>
    apiClient.post<void>(`/api/admin/enrollments/${enrollmentId}/approve`),

  reject: (enrollmentId: number) =>
    apiClient.post<void>(`/api/admin/enrollments/${enrollmentId}/reject`),
};
