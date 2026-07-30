import { useCallback, useEffect, useRef } from 'react';

/**
 * 코호트 종료 확인 다이얼로그 (frontend-components §2.1/§4, 접근성).
 *
 * 되돌릴 수 없음을 명확히 고지하고, 확인 버튼에 포커스 트랩·키보드 접근(Tab 순환, Escape 취소)을 제공한다. 종료 진행 중에는 버튼을 비활성화해 중복 제출을
 * 방지한다. 서버 인가(R-U5-01/02)가 최종 방어선이다.
 */
export function EndCohortDialog({
  open,
  submitting,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  submitting: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const confirmRef = useRef<HTMLButtonElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  // 열릴 때 확인 버튼으로 초기 포커스 이동.
  useEffect(() => {
    if (open) {
      confirmRef.current?.focus();
    }
  }, [open]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLDivElement>) => {
      if (e.key === 'Escape' && !submitting) {
        e.preventDefault();
        onCancel();
        return;
      }
      // 포커스 트랩 — 확인/취소 두 버튼 사이에서만 Tab 순환.
      if (e.key === 'Tab') {
        const focusables = [confirmRef.current, cancelRef.current].filter(
          (el): el is HTMLButtonElement => el !== null,
        );
        if (focusables.length === 0) {
          return;
        }
        const first = focusables[0];
        const last = focusables[focusables.length - 1];
        const active = document.activeElement;
        if (e.shiftKey && active === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && active === last) {
          e.preventDefault();
          first.focus();
        }
      }
    },
    [onCancel, submitting],
  );

  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onKeyDown={handleKeyDown}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="end-cohort-title"
        aria-describedby="end-cohort-desc"
        data-testid="end-cohort-dialog"
        className="w-full max-w-md rounded bg-white p-6 shadow-lg"
      >
        <h2 id="end-cohort-title" className="text-lg font-semibold">
          코호트를 종료할까요?
        </h2>
        <p id="end-cohort-desc" className="mt-2 text-sm text-gray-700">
          코호트를 종료하면 수료·정산 판정이 확정되고 <strong>되돌릴 수 없습니다</strong>.
          진행할까요?
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <button
            ref={cancelRef}
            type="button"
            data-testid="end-cohort-cancel"
            disabled={submitting}
            onClick={onCancel}
            className="rounded border border-gray-300 px-4 py-2 text-sm hover:border-accent disabled:opacity-50"
          >
            취소
          </button>
          <button
            ref={confirmRef}
            type="button"
            data-testid="end-cohort-confirm"
            disabled={submitting}
            onClick={onConfirm}
            className="rounded bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-50"
          >
            {submitting ? '종료 중...' : '종료하기'}
          </button>
        </div>
      </div>
    </div>
  );
}
