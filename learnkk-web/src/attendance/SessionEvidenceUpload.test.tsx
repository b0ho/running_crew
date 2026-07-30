import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '../api/ApiClient';
import { attendanceApi } from '../api/attendanceApi';
import { SessionEvidenceUpload } from './SessionEvidenceUpload';

jest.mock('../api/attendanceApi', () => ({
  attendanceApi: { uploadEvidence: jest.fn() },
}));

const mockUpload = attendanceApi.uploadEvidence as jest.Mock;

function jpegFile() {
  return new File([new Uint8Array([0xff, 0xd8, 0xff])], 'a.jpg', { type: 'image/jpeg' });
}

function fileOfSize(bytes: number) {
  const file = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'big.jpg', { type: 'image/jpeg' });
  Object.defineProperty(file, 'size', { value: bytes });
  return file;
}

describe('SessionEvidenceUpload', () => {
  afterEach(() => jest.clearAllMocks());

  it('허용되지 않는 형식은 사전 검증에서 거부하고 업로드하지 않는다', async () => {
    const onToast = jest.fn();
    const onUploaded = jest.fn();
    render(<SessionEvidenceUpload sessionId={5} onUploaded={onUploaded} onToast={onToast} />);

    const bad = new File(['not an image'], 'note.txt', { type: 'text/plain' });
    await userEvent.upload(screen.getByTestId('evidence-dropzone-5-input'), bad, {
      applyAccept: false,
    });

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith(
        expect.objectContaining({ variant: 'error' }),
      ),
    );
    expect(mockUpload).not.toHaveBeenCalled();
  });

  it('10MB 초과 파일은 사전 검증에서 거부한다', async () => {
    const onToast = jest.fn();
    render(<SessionEvidenceUpload sessionId={5} onUploaded={jest.fn()} onToast={onToast} />);

    await userEvent.upload(screen.getByTestId('evidence-dropzone-5-input'), fileOfSize(11 * 1024 * 1024));

    await waitFor(() => expect(screen.getByTestId('evidence-error-5')).toBeInTheDocument());
    expect(mockUpload).not.toHaveBeenCalled();
  });

  it('업로드 성공 시 인증 성공 토스트와 갱신 콜백을 호출한다', async () => {
    mockUpload.mockResolvedValue({
      id: 1,
      sessionId: 5,
      mimeType: 'image/jpeg',
      size: 3,
      uploadedBy: 10,
      createdAt: '2026-01-01T00:00:00Z',
    });
    const onToast = jest.fn();
    const onUploaded = jest.fn();
    render(<SessionEvidenceUpload sessionId={5} onUploaded={onUploaded} onToast={onToast} />);

    await userEvent.upload(screen.getByTestId('evidence-dropzone-5-input'), jpegFile());

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith({ text: '회차 출석이 인증되었습니다', variant: 'success' }),
    );
    expect(onUploaded).toHaveBeenCalled();
    expect(mockUpload).toHaveBeenCalledWith(5, expect.any(File));
  });

  it('400 FILE_CONSTRAINT_VIOLATION 이면 에러 토스트를 전달한다', async () => {
    mockUpload.mockRejectedValue(new ApiError('FILE_CONSTRAINT_VIOLATION', '형식 위반', 400));
    const onToast = jest.fn();
    render(<SessionEvidenceUpload sessionId={5} onUploaded={jest.fn()} onToast={onToast} />);

    await userEvent.upload(screen.getByTestId('evidence-dropzone-5-input'), jpegFile());

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith({
        text: '허용되지 않는 파일 형식/크기입니다',
        variant: 'error',
      }),
    );
  });

  it('409 COHORT_CLOSED 이면 종료 안내 토스트를 전달한다', async () => {
    mockUpload.mockRejectedValue(new ApiError('COHORT_CLOSED', '종료됨', 409));
    const onToast = jest.fn();
    render(<SessionEvidenceUpload sessionId={5} onUploaded={jest.fn()} onToast={onToast} />);

    await userEvent.upload(screen.getByTestId('evidence-dropzone-5-input'), jpegFile());

    await waitFor(() =>
      expect(onToast).toHaveBeenCalledWith({
        text: '종료된 코호트에는 업로드할 수 없습니다',
        variant: 'error',
      }),
    );
  });
});
