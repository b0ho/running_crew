import { render, screen, waitFor } from '@testing-library/react';
import { adminMetricsApi } from '../api/adminMetricsApi';
import { MetricsOverview } from './MetricsOverview';

jest.mock('../api/adminMetricsApi', () => ({
  adminMetricsApi: {
    getMetrics: jest.fn(),
  },
}));

const mockGetMetrics = adminMetricsApi.getMetrics as jest.Mock;

describe('MetricsOverview', () => {
  afterEach(() => jest.clearAllMocks());

  it('4개 지표 카드와 집계 범위 라벨을 렌더한다', async () => {
    mockGetMetrics.mockResolvedValue({
      completedCohortCount: 3,
      attendanceRate: 90,
      completionRate: 66.7,
      certificateCount: 5,
      scopeLabel: '종료된 코호트 3건 기준',
    });

    render(<MetricsOverview />);

    await waitFor(() => expect(screen.getByTestId('metrics-overview')).toBeInTheDocument());
    expect(screen.getByTestId('metric-completed-cohorts-value')).toHaveTextContent('3개');
    expect(screen.getByTestId('metric-attendance-rate-value')).toHaveTextContent('90%');
    expect(screen.getByTestId('metric-completion-rate-value')).toHaveTextContent('66.7%');
    expect(screen.getByTestId('metric-certificate-count-value')).toHaveTextContent('5장');
    expect(screen.getByTestId('metrics-scope-label')).toHaveTextContent('종료된 코호트 3건 기준');
  });

  it('분모 0 상황(0%)을 안전하게 표시한다', async () => {
    mockGetMetrics.mockResolvedValue({
      completedCohortCount: 0,
      attendanceRate: 0,
      completionRate: 0,
      certificateCount: 0,
      scopeLabel: '종료된 코호트 0건 기준',
    });

    render(<MetricsOverview />);

    await waitFor(() => expect(screen.getByTestId('metrics-overview')).toBeInTheDocument());
    expect(screen.getByTestId('metric-attendance-rate-value')).toHaveTextContent('0%');
    expect(screen.getByTestId('metric-completion-rate-value')).toHaveTextContent('0%');
    expect(screen.getByTestId('metric-completed-cohorts-value')).toHaveTextContent('0개');
  });

  it('오류 시 오류 메시지를 표시한다', async () => {
    mockGetMetrics.mockRejectedValue(new Error('boom'));

    render(<MetricsOverview />);

    await waitFor(() => expect(screen.getByTestId('metrics-error')).toBeInTheDocument());
  });
});
