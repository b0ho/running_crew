import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { adminMetricsApi } from '../api/adminMetricsApi';
import { ReportHistoryTable } from './ReportHistoryTable';

jest.mock('../api/adminMetricsApi', () => ({
  adminMetricsApi: {
    listReportHistory: jest.fn(),
  },
}));

const mockList = adminMetricsApi.listReportHistory as jest.Mock;

function page(content: unknown[], number: number, totalPages: number) {
  return {
    content,
    totalElements: content.length,
    totalPages,
    number,
    size: 20,
    first: number === 0,
    last: number === totalPages - 1,
  };
}

function report(reportId: number, hasAttachment: boolean) {
  return {
    reportId,
    cohortId: 5,
    cohortTitle: '자바 멘토링',
    authorName: '김멘토',
    hasAttachment,
    submittedAt: '2026-03-01T00:00:00Z',
  };
}

describe('ReportHistoryTable', () => {
  afterEach(() => jest.clearAllMocks());

  it('첨부 유무를 있음/없음으로 표기한다', async () => {
    mockList.mockResolvedValue(page([report(100, true), report(101, false)], 0, 1));

    render(<ReportHistoryTable />);

    await waitFor(() => expect(screen.getByTestId('report-row-100')).toBeInTheDocument());
    expect(screen.getByTestId('report-attachment-100')).toHaveTextContent('있음');
    expect(screen.getByTestId('report-attachment-101')).toHaveTextContent('없음');
    expect(screen.getAllByText('김멘토')).toHaveLength(2);
  });

  it('빈 목록이면 빈 상태를 표시한다', async () => {
    mockList.mockResolvedValue(page([], 0, 0));

    render(<ReportHistoryTable />);

    await waitFor(() => expect(screen.getByTestId('report-empty')).toBeInTheDocument());
  });

  it('다음 페이지 버튼은 page 파라미터를 증가시켜 재조회한다', async () => {
    mockList
      .mockResolvedValueOnce(page([report(100, true)], 0, 2))
      .mockResolvedValueOnce(page([report(200, false)], 1, 2));

    render(<ReportHistoryTable />);

    await waitFor(() => expect(screen.getByTestId('report-row-100')).toBeInTheDocument());
    expect(mockList).toHaveBeenNthCalledWith(1, { page: 0 });

    fireEvent.click(screen.getByTestId('report-next'));

    await waitFor(() => expect(mockList).toHaveBeenCalledWith({ page: 1 }));
    await waitFor(() => expect(screen.getByTestId('report-row-200')).toBeInTheDocument());
    expect(screen.getByTestId('report-page-indicator')).toHaveTextContent('2 / 2');
  });
});
