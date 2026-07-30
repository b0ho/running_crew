import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';
import { ReportForm } from './ReportForm';

jest.mock('../api/completionApi', () => ({
  completionApi: { submitReport: jest.fn() },
}));

const mockSubmit = completionApi.submitReport as jest.Mock;

describe('ReportForm', () => {
  afterEach(() => jest.clearAllMocks());

  it('본문이 비면 제출하지 않고 에러를 표시한다', async () => {
    render(<ReportForm cohortId={7} onSubmitted={jest.fn()} onToast={jest.fn()} />);

    await userEvent.click(screen.getByTestId('report-submit'));

    expect(await screen.findByTestId('report-error')).toHaveTextContent('보고서 본문은 필수입니다');
    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('본문만 입력해 제출하면 첨부 없이 API 를 호출한다', async () => {
    mockSubmit.mockResolvedValue({
      id: 1,
      cohortId: 7,
      authorId: 10,
      body: '최종 소회',
      hasAttachment: false,
      submittedAt: '2026-03-01T00:00:00Z',
    });
    const onSubmitted = jest.fn();
    const onToast = jest.fn();
    render(<ReportForm cohortId={7} onSubmitted={onSubmitted} onToast={onToast} />);

    await userEvent.type(screen.getByTestId('report-body'), '최종 소회');
    await userEvent.click(screen.getByTestId('report-submit'));

    await waitFor(() => expect(mockSubmit).toHaveBeenCalledWith(7, '최종 소회', null));
    expect(onSubmitted).toHaveBeenCalled();
    expect(onToast).toHaveBeenCalledWith({ text: '보고서를 제출했습니다', variant: 'success' });
  });

  it('첨부 파일을 선택해 제출하면 파일과 함께 API 를 호출한다', async () => {
    mockSubmit.mockResolvedValue({
      id: 2,
      cohortId: 7,
      authorId: 10,
      body: '첨부 보고서',
      hasAttachment: true,
      submittedAt: '2026-03-01T00:00:00Z',
    });
    render(<ReportForm cohortId={7} onSubmitted={jest.fn()} onToast={jest.fn()} />);

    await userEvent.type(screen.getByTestId('report-body'), '첨부 보고서');
    const pdf = new File([new Uint8Array([1, 2, 3])], 'r.pdf', { type: 'application/pdf' });
    await userEvent.upload(screen.getByTestId('report-dropzone-input'), pdf);

    expect(await screen.findByTestId('report-file-name')).toHaveTextContent('r.pdf');
    await userEvent.click(screen.getByTestId('report-submit'));

    await waitFor(() =>
      expect(mockSubmit).toHaveBeenCalledWith(7, '첨부 보고서', expect.any(File)),
    );
  });

  it('400 FILE_CONSTRAINT_VIOLATION 이면 에러를 표시한다', async () => {
    mockSubmit.mockRejectedValue(new ApiError('FILE_CONSTRAINT_VIOLATION', '형식 위반', 400));
    const onToast = jest.fn();
    render(<ReportForm cohortId={7} onSubmitted={jest.fn()} onToast={onToast} />);

    await userEvent.type(screen.getByTestId('report-body'), '본문');
    await userEvent.click(screen.getByTestId('report-submit'));

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith({
        text: '허용되지 않는 첨부 파일 형식/크기입니다',
        variant: 'error',
      }),
    );
  });
});
