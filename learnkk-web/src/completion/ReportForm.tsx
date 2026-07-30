import { useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';
import type { ReportDto } from '../api/types';
import type { ToastMessage } from '../common/Toast';
import { FileDropzone } from '../attendance/FileDropzone';
import { validateEvidenceFile } from '../attendance/fileConstraints';

/** 제출 실패 코드를 사용자용 한글 메시지로 매핑(frontend-components §2.2). */
function messageForError(err: ApiError): string {
  switch (err.code) {
    case 'FILE_CONSTRAINT_VIOLATION':
      return '허용되지 않는 첨부 파일 형식/크기입니다';
    case 'VALIDATION_ERROR':
      return '보고서 본문은 필수입니다';
    case 'FORBIDDEN':
      return '보고서 제출 권한이 없습니다';
    case 'NOT_FOUND':
      return '코호트를 찾을 수 없습니다';
    default:
      return err.message || '보고서 제출에 실패했습니다';
  }
}

/**
 * 최종 보고서 제출 폼 (US-11 / FR-7).
 *
 * 본문(필수 자유 서식) + 선택 첨부(FileDropzone 재사용, U1 제약). 제출 중에는 버튼을 비활성화해 중복 제출을 방지한다(§4). 성공 시 상위에 갱신을
 * 알린다(onSubmitted). 서버 검증(R-U5-15/16)이 최종 방어선이다.
 */
export function ReportForm({
  cohortId,
  onSubmitted,
  onToast,
}: {
  cohortId: number;
  onSubmitted: (report: ReportDto) => void;
  onToast: (message: ToastMessage) => void;
}) {
  const [body, setBody] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);

  function handleFile(selected: File) {
    const preError = validateEvidenceFile(selected);
    if (preError) {
      setFieldError(preError);
      onToast({ text: preError, variant: 'error' });
      return;
    }
    setFieldError(null);
    setFile(selected);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!body.trim()) {
      setFieldError('보고서 본문은 필수입니다');
      return;
    }
    setFieldError(null);
    setSubmitting(true);
    try {
      const report = await completionApi.submitReport(cohortId, body.trim(), file);
      onToast({ text: '보고서를 제출했습니다', variant: 'success' });
      setBody('');
      setFile(null);
      onSubmitted(report);
    } catch (err) {
      const message = err instanceof ApiError ? messageForError(err) : '보고서 제출에 실패했습니다';
      setFieldError(message);
      onToast({ text: message, variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form data-testid="report-form" onSubmit={handleSubmit} className="space-y-3">
      <div>
        <label htmlFor="report-body" className="block text-sm font-medium text-gray-700">
          최종 보고서 본문
        </label>
        <textarea
          id="report-body"
          data-testid="report-body"
          value={body}
          onChange={(e) => setBody(e.target.value)}
          rows={5}
          disabled={submitting}
          className="mt-1 w-full rounded border border-gray-300 p-2 text-sm focus:border-accent focus:outline-none disabled:opacity-50"
          placeholder="코호트 운영/참여 소회를 자유롭게 작성하세요"
        />
      </div>

      <div>
        <span className="block text-sm font-medium text-gray-700">첨부 파일 (선택)</span>
        <div className="mt-1">
          <FileDropzone onFile={handleFile} disabled={submitting} testId="report-dropzone" />
        </div>
        {file && (
          <p data-testid="report-file-name" className="mt-1 text-xs text-gray-600">
            선택된 파일: {file.name}
          </p>
        )}
      </div>

      {fieldError && (
        <p role="alert" data-testid="report-error" className="text-xs text-red-600">
          {fieldError}
        </p>
      )}

      <button
        type="submit"
        data-testid="report-submit"
        disabled={submitting}
        className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent-hover disabled:opacity-50"
      >
        {submitting ? '제출 중...' : '보고서 제출'}
      </button>
    </form>
  );
}
