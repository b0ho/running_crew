import { apiClient, toQueryString } from './ApiClient';
import type { EvidenceHistoryItem, MetricsOverviewDto, Page, ReportHistoryItem } from './types';

/**
 * 관리자 운영 지표·이력 조회 API 호출 (frontend-components §3, U6). 모든 호출은 ApiClient(세션 쿠키·에러 정규화) 경유하며, 서버가
 * @PreAuthorize(ADMIN)로 최종 인가한다. U6 은 읽기 전용이므로 조회만 노출한다.
 */
export const adminMetricsApi = {
  /** 운영 지표 개요 — 종료된 코호트 기준 4개 지표 + 집계 범위 라벨. */
  getMetrics: () => apiClient.get<MetricsOverviewDto>('/api/admin/metrics'),

  /** 증빙 이력 목록(최신순, 20건). cohortId 로 코호트를 좁힐 수 있다. */
  listEvidenceHistory: (params: { cohortId?: number; page?: number; size?: number } = {}) =>
    apiClient.get<Page<EvidenceHistoryItem>>(`/api/admin/history/evidence${toQueryString(params)}`),

  /** 보고서 이력 목록(최신순, 20건). 증빙 이력과 별도 뷰. */
  listReportHistory: (params: { cohortId?: number; page?: number; size?: number } = {}) =>
    apiClient.get<Page<ReportHistoryItem>>(`/api/admin/history/reports${toQueryString(params)}`),
};
