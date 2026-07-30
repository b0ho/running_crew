import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ApiError } from '../api/ApiClient';
import { enrollmentApi } from '../api/enrollmentApi';
import { JoinButton } from './JoinButton';

jest.mock('../api/enrollmentApi', () => ({
  enrollmentApi: { join: jest.fn() },
}));

const mockJoin = enrollmentApi.join as jest.Mock;

describe('JoinButton', () => {
  afterEach(() => jest.clearAllMocks());

  it('확정 응답이면 성공 토스트를 전달한다', async () => {
    mockJoin.mockResolvedValue({
      enrollmentId: 1,
      cohortId: 5,
      status: 'CONFIRMED',
      statusLabel: '확정',
      waitingPosition: null,
    });
    const onResult = jest.fn();
    render(<JoinButton cohortId={5} onResult={onResult} />);

    fireEvent.click(screen.getByTestId('join-button-5'));

    await waitFor(() =>
      expect(onResult).toHaveBeenCalledWith({ text: '참여가 확정되었습니다', variant: 'success' }),
    );
  });

  it('대기 응답이면 정보 토스트를 전달한다', async () => {
    mockJoin.mockResolvedValue({
      enrollmentId: 2,
      cohortId: 5,
      status: 'WAITING',
      statusLabel: '대기중',
      waitingPosition: 3,
    });
    const onResult = jest.fn();
    render(<JoinButton cohortId={5} onResult={onResult} />);

    fireEvent.click(screen.getByTestId('join-button-5'));

    await waitFor(() =>
      expect(onResult).toHaveBeenCalledWith({
        text: '정원이 마감되어 대기 신청되었습니다',
        variant: 'info',
      }),
    );
  });

  it('409 ALREADY_ENROLLED 이면 에러 토스트를 전달한다', async () => {
    mockJoin.mockRejectedValue(new ApiError('ALREADY_ENROLLED', '이미', 409));
    const onResult = jest.fn();
    render(<JoinButton cohortId={5} onResult={onResult} />);

    fireEvent.click(screen.getByTestId('join-button-5'));

    await waitFor(() =>
      expect(onResult).toHaveBeenCalledWith({ text: '이미 신청한 코호트입니다', variant: 'error' }),
    );
  });

  it('제출 중에는 버튼을 비활성화해 이중 클릭을 방지한다', async () => {
    mockJoin.mockReturnValue(new Promise(() => {})); // 미해결(pending)
    render(<JoinButton cohortId={5} onResult={jest.fn()} />);

    const btn = screen.getByTestId('join-button-5');
    fireEvent.click(btn);
    fireEvent.click(btn); // 두 번째 클릭 — 비활성화되어 무시되어야 한다

    await waitFor(() => expect(btn).toBeDisabled());
    expect(mockJoin).toHaveBeenCalledTimes(1);
  });
});
