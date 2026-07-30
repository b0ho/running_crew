import { apiClient, toQueryString } from './ApiClient';
import type {
  AnnouncementCreateRequest,
  AnnouncementDto,
  CohortCreateRequest,
  CohortDetailDto,
  CohortDto,
  CohortStatus,
  CohortSummaryDto,
  CohortUpdateRequest,
  Page,
} from './types';

/** 코호트·공지 API 호출 (frontend-components §3). 모든 호출은 ApiClient(세션 쿠키·에러 정규화) 경유. */
export const cohortApi = {
  create: (req: CohortCreateRequest) => apiClient.post<CohortDto>('/api/cohorts', req),

  update: (id: number, req: CohortUpdateRequest) =>
    apiClient.put<CohortDto>(`/api/cohorts/${id}`, req),

  start: (id: number) => apiClient.post<CohortDto>(`/api/cohorts/${id}/start`),

  list: (params: { status?: CohortStatus; keyword?: string; page?: number; size?: number } = {}) =>
    apiClient.get<Page<CohortSummaryDto>>(`/api/cohorts${toQueryString(params)}`),

  get: (id: number) => apiClient.get<CohortDetailDto>(`/api/cohorts/${id}`),

  getMine: () => apiClient.get<CohortSummaryDto[]>('/api/cohorts/mine'),

  createAnnouncement: (cohortId: number, req: AnnouncementCreateRequest) =>
    apiClient.post<AnnouncementDto>(`/api/cohorts/${cohortId}/announcements`, req),

  listAnnouncements: (cohortId: number, params: { page?: number; size?: number } = {}) =>
    apiClient.get<Page<AnnouncementDto>>(
      `/api/cohorts/${cohortId}/announcements${toQueryString(params)}`,
    ),
};
