import { useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { enrollmentApi } from '../api/enrollmentApi';
import type { ToastMessage } from '../common/Toast';

/** 참여 신청 결과 코드 → 사용자 메시지 매핑 (frontend-components §2.1). */
function errorToToast(err: ApiError): ToastMessage {
  switch (err.code) {
    case 'ALREADY_ENROLLED':
      return { text: '이미 신청한 코호트입니다', variant: 'error' };
    case 'SELF_ENROLLMENT':
      return { text: '본인이 개설한 코호트에는 참여할 수 없습니다', variant: 'error' };
    case 'COHORT_NOT_OPEN':
      return { text: '참여할 수 없는 코호트입니다', variant: 'error' };
    case 'ENROLLMENT_BUSY':
      return { text: '신청이 몰려 잠시 후 다시 시도해 주세요', variant: 'error' };
    default:
      return { text: err.message, variant: 'error' };
  }
}

/**
 * 선착순 참여 버튼 (frontend-components §2.1).
 *
 * 제출 중에는 버튼을 비활성화해 이중 클릭을 방지한다(서버 UNIQUE 제약이 최종 방어이나 UX 차원 방지). 결과는 onResult(토스트 메시지)로 전달하고, 확정/대기
 * 성공 시 onSuccess 로 상위 목록 갱신을 알린다.
 */
export function JoinButton({
  cohortId,
  disabled = false,
  onResult,
  onSuccess,
}: {
  cohortId: number;
  disabled?: boolean;
  onResult: (message: ToastMessage) => void;
  onSuccess?: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);

  async function handleClick() {
    if (submitting) {
      return;
    }
    setSubmitting(true);
    try {
      const result = await enrollmentApi.join(cohortId);
      if (result.status === 'CONFIRMED') {
        onResult({ text: '참여가 확정되었습니다', variant: 'success' });
      } else {
        onResult({
          text: '정원이 마감되어 대기 신청되었습니다',
          variant: 'info',
        });
      }
      onSuccess?.();
    } catch (err) {
      onResult(
        err instanceof ApiError
          ? errorToToast(err)
          : { text: '참여 신청 중 오류가 발생했습니다', variant: 'error' },
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <button
      type="button"
      data-testid={`join-button-${cohortId}`}
      disabled={disabled || submitting}
      onClick={handleClick}
      className="rounded bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-50"
    >
      {submitting ? '신청 중...' : '참여 신청'}
    </button>
  );
}
