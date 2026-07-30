import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { notificationApi } from '../api/notificationApi';
import { NotificationBell } from './NotificationBell';

jest.mock('../api/notificationApi', () => ({
  notificationApi: {
    unreadCount: jest.fn(),
    listFor: jest.fn(),
    markRead: jest.fn(),
  },
}));

const mockUnread = notificationApi.unreadCount as jest.Mock;
const mockList = notificationApi.listFor as jest.Mock;
const mockMarkRead = notificationApi.markRead as jest.Mock;

function page(content: unknown[]) {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20, first: true, last: true };
}

describe('NotificationBell', () => {
  afterEach(() => jest.clearAllMocks());

  it('안읽은 수 배지를 표시한다', async () => {
    mockUnread.mockResolvedValue({ count: 2 });
    render(<NotificationBell />);

    await waitFor(() => expect(screen.getByTestId('notification-badge')).toHaveTextContent('2'));
  });

  it('열면 목록을 조회하고 안읽은 항목 클릭 시 markRead 를 호출한다', async () => {
    mockUnread.mockResolvedValue({ count: 1 });
    mockList.mockResolvedValue(
      page([
        {
          id: 10,
          type: 'ENROLLMENT_CONFIRMED',
          message: '참여가 확정되었습니다',
          read: false,
          createdAt: '2026-01-01T00:00:00Z',
        },
      ]),
    );
    mockMarkRead.mockResolvedValue(undefined);

    render(<NotificationBell />);
    await waitFor(() => expect(screen.getByTestId('notification-badge')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('notification-bell'));

    await waitFor(() => expect(screen.getByTestId('notification-item-10')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('notification-item-10'));

    await waitFor(() => expect(mockMarkRead).toHaveBeenCalledWith(10));
  });

  it('알림이 없으면 빈 안내를 보여준다', async () => {
    mockUnread.mockResolvedValue({ count: 0 });
    mockList.mockResolvedValue(page([]));

    render(<NotificationBell />);
    fireEvent.click(screen.getByTestId('notification-bell'));

    await waitFor(() => expect(screen.getByTestId('notification-empty')).toBeInTheDocument());
  });
});
