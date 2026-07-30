import { render, screen } from '@testing-library/react';
import type { CohortAttendanceDto } from '../api/types';
import { ProgressSummary } from './ProgressSummary';

const attendance: CohortAttendanceDto = {
  cohortId: 1,
  verifiedCount: 3,
  totalCount: 4,
  progressRate: 0.75,
  sessions: [],
};

describe('ProgressSummary', () => {
  it('인증/전체 회차와 진도율을 수치로 병기한다', () => {
    render(<ProgressSummary attendance={attendance} />);

    expect(screen.getByTestId('progress-rate')).toHaveTextContent('인증 3 / 전체 4 회차 (75%)');
  });

  it('진도율 바에 aria-valuenow 를 반영한다', () => {
    render(<ProgressSummary attendance={attendance} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '75');
  });

  it('전체 회차가 0이면 진도율 0%로 렌더한다', () => {
    render(
      <ProgressSummary
        attendance={{ cohortId: 1, verifiedCount: 0, totalCount: 0, progressRate: 0, sessions: [] }}
      />,
    );

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '0');
    expect(screen.getByTestId('progress-rate')).toHaveTextContent('(0%)');
  });
});
