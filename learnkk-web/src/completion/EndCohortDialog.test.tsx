import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';
import { EndCohortButton } from './EndCohortButton';

jest.mock('../api/completionApi', () => ({
  completionApi: { endCohort: jest.fn() },
}));

const mockEnd = completionApi.endCohort as jest.Mock;

describe('EndCohortButton + EndCohortDialog', () => {
  afterEach(() => jest.clearAllMocks());

  it('버튼 클릭 시 되돌릴 수 없음을 고지하는 다이얼로그를 열고 확인 버튼에 포커스한다', async () => {
    render(<EndCohortButton cohortId={7} onEnded={jest.fn()} onToast={jest.fn()} />);

    await userEvent.click(screen.getByTestId('end-cohort-button'));

    const dialog = await screen.findByTestId('end-cohort-dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText(/되돌릴 수 없습니다/)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId('end-cohort-confirm')).toHaveFocus());
  });

  it('확인 시 종료 API 를 호출하고 요약을 상위로 전달한다', async () => {
    mockEnd.mockResolvedValue({
      certifiedCount: 2,
      notCertifiedCount: 1,
      totalConfirmed: 3,
      settlementSatisfied: false,
      issuedCertificateCount: 2,
    });
    const onEnded = jest.fn();
    const onToast = jest.fn();
    render(<EndCohortButton cohortId={7} onEnded={onEnded} onToast={onToast} />);

    await userEvent.click(screen.getByTestId('end-cohort-button'));
    await userEvent.click(screen.getByTestId('end-cohort-confirm'));

    await waitFor(() =>
      expect(onEnded).toHaveBeenCalledWith(expect.objectContaining({ certifiedCount: 2 })),
    );
    expect(mockEnd).toHaveBeenCalledWith(7);
    expect(onToast).toHaveBeenCalledWith({ text: '코호트를 종료했습니다', variant: 'success' });
  });

  it('409 INVALID_STATE_TRANSITION 이면 진행중 안내 토스트를 전달한다', async () => {
    mockEnd.mockRejectedValue(new ApiError('INVALID_STATE_TRANSITION', '전이 오류', 409));
    const onToast = jest.fn();
    render(<EndCohortButton cohortId={7} onEnded={jest.fn()} onToast={onToast} />);

    await userEvent.click(screen.getByTestId('end-cohort-button'));
    await userEvent.click(screen.getByTestId('end-cohort-confirm'));

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith({
        text: '진행중 코호트만 종료할 수 있습니다',
        variant: 'error',
      }),
    );
  });

  it('403 FORBIDDEN 이면 소유 멘토 안내 토스트를 전달한다', async () => {
    mockEnd.mockRejectedValue(new ApiError('FORBIDDEN', '권한 없음', 403));
    const onToast = jest.fn();
    render(<EndCohortButton cohortId={7} onEnded={jest.fn()} onToast={onToast} />);

    await userEvent.click(screen.getByTestId('end-cohort-button'));
    await userEvent.click(screen.getByTestId('end-cohort-confirm'));

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith({
        text: '코호트 소유 멘토만 종료할 수 있습니다',
        variant: 'error',
      }),
    );
  });

  it('취소 버튼은 다이얼로그를 닫고 API 를 호출하지 않는다', async () => {
    render(<EndCohortButton cohortId={7} onEnded={jest.fn()} onToast={jest.fn()} />);

    await userEvent.click(screen.getByTestId('end-cohort-button'));
    await userEvent.click(screen.getByTestId('end-cohort-cancel'));

    await waitFor(() => expect(screen.queryByTestId('end-cohort-dialog')).not.toBeInTheDocument());
    expect(mockEnd).not.toHaveBeenCalled();
  });
});
