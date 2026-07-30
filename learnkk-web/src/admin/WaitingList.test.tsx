import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { adminApi } from '../api/adminApi';
import { WaitingList } from './WaitingList';

jest.mock('../api/adminApi', () => ({
  adminApi: {
    listWaiting: jest.fn(),
    approve: jest.fn(),
    reject: jest.fn(),
  },
}));

const mockListWaiting = adminApi.listWaiting as jest.Mock;
const mockApprove = adminApi.approve as jest.Mock;
const mockReject = adminApi.reject as jest.Mock;

function page(content: unknown[]) {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20, first: true, last: true };
}

function waitingRow(enrollmentId: number) {
  return {
    enrollmentId,
    cohortId: 5,
    cohortTitle: '자바 멘토링',
    menteeId: 7,
    menteeName: '홍길동',
    menteeNickname: 'gildong',
    createdAt: '2026-01-01T00:00:00Z',
  };
}

describe('WaitingList', () => {
  afterEach(() => jest.clearAllMocks());

  it('대기 목록을 렌더한다', async () => {
    mockListWaiting.mockResolvedValue(page([waitingRow(100)]));
    render(<WaitingList />);

    await waitFor(() => expect(screen.getByTestId('waiting-row-100')).toBeInTheDocument());
    expect(screen.getByText('자바 멘토링')).toBeInTheDocument();
  });

  it('승인은 확인 다이얼로그를 거쳐 approve 를 호출하고 목록에서 제거한다', async () => {
    mockListWaiting.mockResolvedValue(page([waitingRow(100)]));
    mockApprove.mockResolvedValue(undefined);
    render(<WaitingList />);

    await waitFor(() => expect(screen.getByTestId('waiting-approve-100')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('waiting-approve-100'));

    // 정원 초과 승인 확인 다이얼로그
    expect(screen.getByTestId('approve-confirm-dialog')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('approve-confirm-ok'));

    await waitFor(() => expect(mockApprove).toHaveBeenCalledWith(100));
    await waitFor(() => expect(screen.queryByTestId('waiting-row-100')).not.toBeInTheDocument());
  });

  it('확인 다이얼로그 취소 시 approve 를 호출하지 않는다', async () => {
    mockListWaiting.mockResolvedValue(page([waitingRow(100)]));
    render(<WaitingList />);

    await waitFor(() => expect(screen.getByTestId('waiting-approve-100')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('waiting-approve-100'));
    fireEvent.click(screen.getByTestId('approve-confirm-cancel'));

    expect(screen.queryByTestId('approve-confirm-dialog')).not.toBeInTheDocument();
    expect(mockApprove).not.toHaveBeenCalled();
  });

  it('거절은 reject 를 호출하고 목록에서 제거한다', async () => {
    mockListWaiting.mockResolvedValue(page([waitingRow(100)]));
    mockReject.mockResolvedValue(undefined);
    render(<WaitingList />);

    await waitFor(() => expect(screen.getByTestId('waiting-reject-100')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('waiting-reject-100'));

    await waitFor(() => expect(mockReject).toHaveBeenCalledWith(100));
    await waitFor(() => expect(screen.queryByTestId('waiting-row-100')).not.toBeInTheDocument());
  });
});
