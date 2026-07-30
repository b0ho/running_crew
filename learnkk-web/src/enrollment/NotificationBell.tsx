import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { notificationApi } from '../api/notificationApi';
import type { NotificationDto } from '../api/types';

/**
 * 알림 벨 위젯 (frontend-components §2.3).
 *
 * 안읽은 알림 수 배지를 표시하고, 열면 최신 알림 목록을 조회한다. 항목 클릭 시 읽음 처리(markRead)하고 안읽은 수를 갱신한다. 파일럿은 실시간 푸시 없이 진입 시
 * 조회한다(폴링/푸시는 범위 외).
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<NotificationDto[]>([]);
  const [error, setError] = useState<string | null>(null);

  const refreshUnread = useCallback(() => {
    notificationApi
      .unreadCount()
      .then((res) => setUnread(res.count))
      .catch(() => {
        // 배지 조회 실패는 조용히 무시(핵심 플로우 비차단).
      });
  }, []);

  useEffect(() => {
    refreshUnread();
  }, [refreshUnread]);

  const toggle = useCallback(() => {
    const next = !open;
    setOpen(next);
    if (next) {
      setError(null);
      notificationApi
        .listFor({})
        .then((page) => setItems(page.content))
        .catch((err) =>
          setError(err instanceof ApiError ? err.message : '알림을 불러오지 못했습니다'),
        );
    }
  }, [open]);

  async function handleItemClick(item: NotificationDto) {
    if (item.read) {
      return;
    }
    try {
      await notificationApi.markRead(item.id);
      setItems((prev) => prev.map((n) => (n.id === item.id ? { ...n, read: true } : n)));
      refreshUnread();
    } catch {
      // 읽음 처리 실패는 무시(다음 조회 시 재시도 가능).
    }
  }

  return (
    <div className="relative">
      <button
        type="button"
        data-testid="notification-bell"
        aria-label={unread > 0 ? `안읽은 알림 ${unread}건` : '알림'}
        onClick={toggle}
        className="relative rounded p-1 text-gray-600 hover:text-gray-900"
      >
        <span aria-hidden="true">🔔</span>
        {unread > 0 && (
          <span
            data-testid="notification-badge"
            className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white"
          >
            {unread}
          </span>
        )}
      </button>

      {open && (
        <div
          data-testid="notification-dropdown"
          className="absolute right-0 z-40 mt-2 max-h-96 w-72 overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg"
        >
          {error && (
            <p role="alert" className="p-4 text-sm text-red-600">
              {error}
            </p>
          )}
          {!error && items.length === 0 && (
            <p data-testid="notification-empty" className="p-4 text-sm text-gray-500">
              알림이 없습니다.
            </p>
          )}
          {!error &&
            items.map((item) => (
              <button
                key={item.id}
                type="button"
                data-testid={`notification-item-${item.id}`}
                onClick={() => handleItemClick(item)}
                className={`block w-full border-b border-gray-100 p-3 text-left text-sm last:border-b-0 hover:bg-gray-50 ${
                  item.read ? 'text-gray-500' : 'font-medium text-gray-900'
                }`}
              >
                {item.message}
                {!item.read && (
                  <span
                    data-testid={`notification-unread-dot-${item.id}`}
                    className="ml-1 inline-block h-2 w-2 rounded-full bg-accent align-middle"
                    aria-label="안읽음"
                  />
                )}
              </button>
            ))}
        </div>
      )}
    </div>
  );
}
