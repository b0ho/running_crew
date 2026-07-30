import { render, screen } from '@testing-library/react';
import type { SessionAttendanceDto } from '../api/types';
import { SessionAttendanceList } from './SessionAttendanceList';

const sessions: SessionAttendanceDto[] = [
  { sessionId: 1, seq: 1, status: 'SCHEDULED', statusLabel: '예정', hasEvidence: false, latestEvidenceId: null },
  { sessionId: 2, seq: 2, status: 'VERIFIED', statusLabel: '인증', hasEvidence: true, latestEvidenceId: 9 },
];

describe('SessionAttendanceList', () => {
  const noop = () => {};

  it('회차별 상태 배지를 렌더한다', () => {
    render(
      <SessionAttendanceList sessions={sessions} isMentor={false} onUploaded={noop} onToast={noop} />,
    );

    expect(screen.getByTestId('session-item-1')).toBeInTheDocument();
    expect(screen.getByTestId('session-item-2')).toBeInTheDocument();
    expect(screen.getByTestId('session-status-VERIFIED')).toHaveTextContent('인증');
  });

  it('증빙이 있는 회차만 다운로드 링크를 노출한다', () => {
    render(
      <SessionAttendanceList sessions={sessions} isMentor={false} onUploaded={noop} onToast={noop} />,
    );

    expect(screen.queryByTestId('evidence-download-1')).not.toBeInTheDocument();
    const link = screen.getByTestId('evidence-download-2');
    expect(link).toHaveAttribute('href', expect.stringContaining('/api/sessions/2/evidence/9'));
  });

  it('멘토가 아니면 업로드 컨트롤을 노출하지 않는다', () => {
    render(
      <SessionAttendanceList sessions={sessions} isMentor={false} onUploaded={noop} onToast={noop} />,
    );

    expect(screen.queryByTestId('evidence-upload-1')).not.toBeInTheDocument();
  });

  it('멘토면 회차별 업로드 컨트롤을 노출한다', () => {
    render(
      <SessionAttendanceList sessions={sessions} isMentor onUploaded={noop} onToast={noop} />,
    );

    expect(screen.getByTestId('evidence-upload-1')).toBeInTheDocument();
    expect(screen.getByTestId('evidence-upload-2')).toBeInTheDocument();
  });

  it('회차가 없으면 안내 문구를 렌더한다', () => {
    render(<SessionAttendanceList sessions={[]} isMentor={false} onUploaded={noop} onToast={noop} />);

    expect(screen.getByTestId('attendance-empty')).toBeInTheDocument();
  });
});
