import { apiClient, toQueryString } from './ApiClient';
import type { NotificationDto, Page } from './types';

/** 알림 API 호출 (frontend-components §2.3). 모두 세션 사용자 스코프. */
export const notificationApi = {
  listFor: (params: { page?: number; size?: number } = {}) =>
    apiClient.get<Page<NotificationDto>>(`/api/me/notifications${toQueryString(params)}`),

  unreadCount: () => apiClient.get<{ count: number }>('/api/me/notifications/unread-count'),

  markRead: (id: number) => apiClient.post<void>(`/api/me/notifications/${id}/read`),
};
