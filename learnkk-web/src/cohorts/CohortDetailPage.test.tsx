import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { CohortDetailPage } from './CohortDetailPage';
import { cohortApi } from '../api/cohortApi';
import { attendanceApi } from '../api/attendanceApi';
import type { CohortAttendanceDto, CohortDetailDto } from '../api/types';

const mockUseAuth = jest.fn();
jest.mock('../auth/authContext', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: () => ({ id: '1' }),
  useNavigate: () => jest.fn(),
}));

jest.mock('../api/cohortApi', () => ({
  cohortApi: {
    get: jest.fn(),
    createAnnouncement: jest.fn(),
    start: jest.fn(),
  },
}));

jest.mock('../api/attendanceApi', () => ({
  attendanceApi: {
    getAttendance: jest.fn(),
    uploadEvidence: jest.fn(),
    evidenceDownloadUrl: (sessionId: number, evidenceId: number) =>
      `/api/sessions/${sessionId}/evidence/${evidenceId}`,
    loadEvidence: jest.fn(),
  },
}));

const detail: CohortDetailDto = {
  id: 1,
  mentorId: 10,
  title: '자바 멘토링',
  description: null,
  capacity: 20,
  startDate: '2026-01-01',
  endDate: '2026-03-01',
  sessionCount: 2,
  status: 'ONGOING',
  statusLabel: '진행중',
  createdAt: '2026-01-01T00:00:00Z',
  sessions: [
    { id: 1, seq: 1, status: 'SCHEDULED', statusLabel: '예정' },
    { id: 2, seq: 2, status: 'VERIFIED', statusLabel: '인증' },
  ],
  recentAnnouncements: [
    { id: 1, cohortId: 1, body: '1회차 안내', externalLink: null, createdAt: '2026-01-02T00:00:00Z' },
  ],
};

function renderPage() {
  return render(
    <MemoryRouter>
      <CohortDetailPage />
    </MemoryRouter>,
  );
}

const attendance: CohortAttendanceDto = {
  cohortId: 1,
  verifiedCount: 1,
  totalCount: 2,
  progressRate: 0.5,
  sessions: [
    { sessionId: 1, seq: 1, status: 'SCHEDULED', statusLabel: '예정', hasEvidence: false, latestEvidenceId: null },
    { sessionId: 2, seq: 2, status: 'VERIFIED', statusLabel: '인증', hasEvidence: true, latestEvidenceId: 9 },
  ],
};

describe('CohortDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (cohortApi.get as jest.Mock).mockResolvedValue(detail);
    (attendanceApi.getAttendance as jest.Mock).mockResolvedValue(attendance);
  });

  it('멘토에게는 공지 작성 폼과 공지 목록이 보인다', async () => {
    mockUseAuth.mockReturnValue({ currentUser: { id: 10, name: '멘토', isAdmin: false } });
    renderPage();

    expect(await screen.findByTestId('cohort-detail-title')).toHaveTextContent('자바 멘토링');
    expect(screen.getByTestId('announcement-form')).toBeInTheDocument();
    expect(screen.getByTestId('announcement-item-1')).toHaveTextContent('1회차 안내');
  });

  it('비멘토에게는 공지 작성 폼이 보이지 않는다', async () => {
    mockUseAuth.mockReturnValue({ currentUser: { id: 99, name: '멘티', isAdmin: false } });
    renderPage();

    expect(await screen.findByTestId('cohort-detail-title')).toBeInTheDocument();
    expect(screen.queryByTestId('announcement-form')).not.toBeInTheDocument();
  });

  it('진도·출석 탭에서 회차 목록과 상태 배지를 렌더한다', async () => {
    mockUseAuth.mockReturnValue({ currentUser: { id: 99, name: '멘티', isAdmin: false } });
    renderPage();

    await screen.findByTestId('cohort-detail-title');
    await userEvent.click(screen.getByTestId('tab-progress'));

    expect(await screen.findByTestId('session-item-1')).toBeInTheDocument();
    expect(screen.getByTestId('session-item-2')).toBeInTheDocument();
    expect(screen.getByTestId('session-status-VERIFIED')).toHaveTextContent('인증');
    expect(screen.getByTestId('progress-rate')).toHaveTextContent('인증 1 / 전체 2');
  });
});
