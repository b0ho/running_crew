import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';
import { CompletionResult } from './CompletionResult';

jest.mock('../api/completionApi', () => ({
  completionApi: { getCertificate: jest.fn() },
}));

const mockGetCert = completionApi.getCertificate as jest.Mock;

describe('CompletionResult', () => {
  afterEach(() => jest.clearAllMocks());

  it('수료증 조회 성공 시 수료 배너와 다운로드 버튼을 보여준다', async () => {
    mockGetCert.mockResolvedValue(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }));
    render(<CompletionResult cohortId={7} />);

    expect(await screen.findByTestId('completion-result-certified')).toBeInTheDocument();
    expect(screen.getByText('수료를 축하합니다!')).toBeInTheDocument();
    expect(screen.getByTestId('certificate-download')).toBeInTheDocument();
  });

  it('404(NOT_FOUND)면 미수료 배너를 보여준다', async () => {
    mockGetCert.mockRejectedValue(new ApiError('NOT_FOUND', '수료증 없음', 404));
    render(<CompletionResult cohortId={7} />);

    expect(await screen.findByTestId('completion-result-not-certified')).toBeInTheDocument();
    expect(screen.getByText('미수료')).toBeInTheDocument();
  });

  it('그 외 오류면 에러 메시지를 보여준다', async () => {
    mockGetCert.mockRejectedValue(new ApiError('INTERNAL_ERROR', '서버 오류', 500));
    render(<CompletionResult cohortId={7} />);

    expect(await screen.findByTestId('completion-result-error')).toBeInTheDocument();
  });

  it('수료 시 다운로드 버튼을 누르면 object URL 로 다운로드를 트리거한다', async () => {
    mockGetCert.mockResolvedValue(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }));
    const createObjectURL = jest.fn().mockReturnValue('blob:mock');
    const revokeObjectURL = jest.fn();
    // jsdom 은 URL.createObjectURL 를 구현하지 않으므로 스텁한다.
    (URL as unknown as { createObjectURL: unknown }).createObjectURL = createObjectURL;
    (URL as unknown as { revokeObjectURL: unknown }).revokeObjectURL = revokeObjectURL;
    // jsdom 은 앵커 클릭 네비게이션을 구현하지 않으므로 click 을 스텁해 콘솔 노이즈를 제거한다.
    const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    render(<CompletionResult cohortId={7} />);
    await userEvent.click(await screen.findByTestId('certificate-download'));

    expect(createObjectURL).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock');
    clickSpy.mockRestore();
  });
});
