import { useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';
import type { CohortEndSummaryDto } from '../api/types';
import type { ToastMessage } from '../common/Toast';
import { EndCohortDialog } from './EndCohortDialog';

/** 종료 실패 코드를 사용자용 한글 메시지로 매핑(frontend-components §2.1). */
function messageForError(err: ApiError): string {
  switch (err.code) {
    case 'FORBIDDEN':
      return '코호트 소유 멘토만 종료할 수 있습니다';
    case 'INVALID_STATE_TRANSITION':
      return '진행중 코호트만 종료할 수 있습니다';
    case 'NOT_FOUND':
      return '코호트를 찾을 수 없습니다';
    default:
      return err.message || '코호트 종료에 실패했습니다';
  }
}

/**
 * 코호트 종료 버튼 (US-4 종료, 멘토 전용).
 *
 * 진행중 코호트의 소유 멘토에게만 노출한다(서버 R-U5-01/02 최종 방어). 확인 다이얼로그에서 확정하면 종료 API 를 호출하고, 성공 시 종료 요약을 상위로
 * 전달한다(onEnded).
 */
export function EndCohortButton({
  cohortId,
  onEnded,
  onToast,
}: {
  cohortId: number;
  onEnded: (summary: CohortEndSummaryDto) => void;
  onToast: (message: ToastMessage) => void;
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleConfirm() {
    setSubmitting(true);
    try {
      const summary = await completionApi.endCohort(cohortId);
      onToast({ text: '코호트를 종료했습니다', variant: 'success' });
      setDialogOpen(false);
      onEnded(summary);
    } catch (err) {
      const message = err instanceof ApiError ? messageForError(err) : '코호트 종료에 실패했습니다';
      onToast({ text: message, variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <button
        type="button"
        data-testid="end-cohort-button"
        onClick={() => setDialogOpen(true)}
        className="rounded border border-red-300 px-3 py-1 text-sm text-red-700 hover:border-red-500"
      >
        코호트 종료
      </button>
      <EndCohortDialog
        open={dialogOpen}
        submitting={submitting}
        onConfirm={handleConfirm}
        onCancel={() => {
          if (!submitting) {
            setDialogOpen(false);
          }
        }}
      />
    </>
  );
}
