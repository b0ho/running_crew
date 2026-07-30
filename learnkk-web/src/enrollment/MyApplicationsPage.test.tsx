import { render, screen, waitFor } from '@testing-library/react';
import { enrollmentApi } from '../api/enrollmentApi';
import { MyApplicationsPage } from './MyApplicationsPage';

jest.mock('../api/enrollmentApi', () => ({
  enrollmentApi: { myApplications: jest.fn() },
}));
// 셸은 인증/알림 의존이 있어 이 단위 테스트에서는 대체한다.
jest.mock('../shell/ResponsiveTabBar', () => ({ ResponsiveTabBar: () => null }));

const mockMyApplications = enrollmentApi.myApplications as jest.Mock;

function page(content: unknown[]) {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20, first: true, last: true };
}

describe('MyApplicationsPage', () => {
  afterEach(() => jest.clearAllMocks());

  it('신청 상태 배지를 렌더한다', async () => {
    mockMyApplications.mockResolvedValue(
      page([
        {
          id: 1,
          cohortId: 5,
          cohortTitle: '자바 멘토링',
          menteeId: 7,
          status: 'CONFIRMED',
          statusLabel: '확정',
          createdAt: '2026-01-01T00:00:00Z',
          decidedAt: null,
        },
        {
          id: 2,
          cohortId: 6,
          cohortTitle: '파이썬 멘토링',
          menteeId: 7,
          status: 'WAITING',
          statusLabel: '대기중',
          createdAt: '2026-01-02T00:00:00Z',
          decidedAt: null,
        },
      ]),
    );

    render(<MyApplicationsPage />);

    await waitFor(() => expect(screen.getByTestId('my-applications-list')).toBeInTheDocument());
    expect(screen.getByTestId('enrollment-status-CONFIRMED')).toHaveTextContent('확정');
    expect(screen.getByTestId('enrollment-status-WAITING')).toHaveTextContent('대기중');
    expect(screen.getByText('자바 멘토링')).toBeInTheDocument();
  });

  it('신청이 없으면 빈 상태를 보여준다', async () => {
    mockMyApplications.mockResolvedValue(page([]));

    render(<MyApplicationsPage />);

    await waitFor(() => expect(screen.getByTestId('my-applications-empty')).toBeInTheDocument());
  });
});
