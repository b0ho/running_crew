import { useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { attendanceApi } from '../api/attendanceApi';
import type { ToastMessage } from '../common/Toast';
import { FileDropzone } from './FileDropzone';
import { validateEvidenceFile } from './fileConstraints';

/** 업로드 실패 코드를 사용자용 한글 메시지로 매핑(frontend-components §2.3). */
function messageForError(err: ApiError): string {
  switch (err.code) {
    case 'FILE_CONSTRAINT_VIOLATION':
      return '허용되지 않는 파일 형식/크기입니다';
    case 'FORBIDDEN':
      return '증빙 업로드 권한이 없습니다';
    case 'COHORT_CLOSED':
      return '종료된 코호트에는 업로드할 수 없습니다';
    case 'NOT_FOUND':
      return '대상 회차를 찾을 수 없습니다';
    default:
      return err.message || '증빙 업로드에 실패했습니다';
  }
}

/**
 * 회차 증빙 업로드 (멘토 전용, US-9 / FR-5).
 *
 * 클라이언트 사전 검증(jpg/png/pdf ≤10MB) 후 멀티파트 업로드한다. 성공 시 상위에 회차 인증 갱신을 알리고(onUploaded) 결과 토스트를 전달한다.
 * 서버 권한(R-U4-01)·매직바이트 검증이 최종 방어선이다.
 */
export function SessionEvidenceUpload({
  sessionId,
  onUploaded,
  onToast,
}: {
  sessionId: number;
  onUploaded: () => void;
  onToast: (message: ToastMessage) => void;
}) {
  const [uploading, setUploading] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);

  async function handleFile(file: File) {
    const preError = validateEvidenceFile(file);
    if (preError) {
      setFieldError(preError);
      onToast({ text: preError, variant: 'error' });
      return;
    }
    setFieldError(null);
    setUploading(true);
    try {
      await attendanceApi.uploadEvidence(sessionId, file);
      onToast({ text: '회차 출석이 인증되었습니다', variant: 'success' });
      onUploaded();
    } catch (err) {
      const message = err instanceof ApiError ? messageForError(err) : '증빙 업로드에 실패했습니다';
      setFieldError(message);
      onToast({ text: message, variant: 'error' });
    } finally {
      setUploading(false);
    }
  }

  return (
    <div data-testid={`evidence-upload-${sessionId}`} className="mt-2">
      <FileDropzone
        onFile={handleFile}
        disabled={uploading}
        testId={`evidence-dropzone-${sessionId}`}
      />
      {uploading && (
        <p data-testid={`evidence-uploading-${sessionId}`} className="mt-1 text-xs text-gray-500">
          업로드 중...
        </p>
      )}
      {fieldError && (
        <p
          role="alert"
          data-testid={`evidence-error-${sessionId}`}
          className="mt-1 text-xs text-red-600"
        >
          {fieldError}
        </p>
      )}
    </div>
  );
}
