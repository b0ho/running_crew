import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { adminMetricsApi } from '../api/adminMetricsApi';
import { EvidenceHistoryTable } from './EvidenceHistoryTable';

jest.mock('../api/adminMetricsApi', () => ({
  adminMetricsApi: {
    listEvidenceHistory: jest.fn(),
  },
}));

const mockList = adminMetricsApi.listEvidenceHistory as jest.Mock;

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

function evidence(evidenceId: number) {
  return {
    evidenceId,
    sessionId: 5,
    cohortTitle: '자바 멘토링',
    sessionSeq: 2,
    mimeType: 'application/pdf',
    size: 2048,
    uploadedBy: '홍길동',
    createdAt: '2026-02-01T00:00:00Z',
  };
}

describe('EvidenceHistoryTable', () => {
  afterEach(() => jest.clearAllMocks());

  it('증빙 이력 행을 렌더하고 다운로드 링크를 제공한다', async () => {
    mockList.mockResolvedValue(page([evidence(100)], 0, 1));

    render(<EvidenceHistoryTable />);

    await waitFor(() => expect(screen.getByTestId('evidence-row-100')).toBeInTheDocument());
    expect(screen.getByText('자바 멘토링')).toBeInTheDocument();
    expect(screen.getByText('2회차')).toBeInTheDocument();
    expect(screen.getByText('홍길동')).toBeInTheDocument();
    const link = screen.getByTestId('evidence-download-100');
    expect(link).toHaveAttribute('href', expect.stringContaining('/api/sessions/5/evidence/100'));
  });

  it('빈 목록이면 빈 상태를 표시한다', async () => {
    mockList.mockResolvedValue(page([], 0, 0));

    render(<EvidenceHistoryTable />);

    await waitFor(() => expect(screen.getByTestId('evidence-empty')).toBeInTheDocument());
  });

  it('다음 페이지 버튼은 page 파라미터를 증가시켜 재조회한다', async () => {
    mockList
      .mockResolvedValueOnce(page([evidence(100)], 0, 2))
      .mockResolvedValueOnce(page([evidence(200)], 1, 2));

    render(<EvidenceHistoryTable />);

    await waitFor(() => expect(screen.getByTestId('evidence-row-100')).toBeInTheDocument());
    expect(mockList).toHaveBeenNthCalledWith(1, { page: 0 });

    fireEvent.click(screen.getByTestId('evidence-next'));

    await waitFor(() => expect(mockList).toHaveBeenCalledWith({ page: 1 }));
    await waitFor(() => expect(screen.getByTestId('evidence-row-200')).toBeInTheDocument());
    expect(screen.getByTestId('evidence-page-indicator')).toHaveTextContent('2 / 2');
  });
});
