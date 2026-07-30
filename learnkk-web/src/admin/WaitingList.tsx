import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { adminApi } from '../api/adminApi';
import type { WaitingEnrollmentDto } from '../api/types';
import { Toast } from '../common/Toast';
import type { ToastMessage } from '../common/Toast';

/**
 * 관리자 대기 승인 목록 (frontend-components §2.4).
 *
 * 대기중 신청을 승인/거절한다. 대기 신청은 정원 마감 상황이므로 승인은 정원 초과가 될 수 있어(R-U3-13) 확인 다이얼로그를 거친다. 처리 중에는 해당 행 버튼을
 * 비활성화한다. 성공 시 목록에서 제거하고 결과를 aria-live 토스트로 안내한다.
 */
export function WaitingList() {
  const [items, setItems] = useState<WaitingEnrollmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [processing, setProcessing] = useState<number[]>([]);
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastMessage | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    adminApi
      .listWaiting({})
      .then((page) => setItems(page.content))
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : '대기 목록을 불러오지 못했습니다'),
      )
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function isProcessing(id: number) {
    return processing.includes(id);
  }

  async function runAction(id: number, action: 'approve' | 'reject') {
    setProcessing((prev) => [...prev, id]);
    try {
      if (action === 'approve') {
        await adminApi.approve(id);
        setToast({ text: '참여를 확정했습니다', variant: 'success' });
      } else {
        await adminApi.reject(id);
        setToast({ text: '참여를 거절했습니다', variant: 'info' });
      }
      setItems((prev) => prev.filter((item) => item.enrollmentId !== id));
    } catch (err) {
      setToast({
        text: err instanceof ApiError ? err.message : '처리 중 오류가 발생했습니다',
        variant: 'error',
      });
    } finally {
      setProcessing((prev) => prev.filter((pid) => pid !== id));
    }
  }

  async function confirmApprove() {
    if (confirmingId == null) {
      return;
    }
    const id = confirmingId;
    setConfirmingId(null);
    await runAction(id, 'approve');
  }

  return (
    <div>
      <h2 className="text-lg font-semibold" data-testid="waiting-list-title">
        대기 승인
      </h2>

      {loading && (
        <p data-testid="waiting-loading" className="mt-4 text-sm text-gray-400">
          불러오는 중...
        </p>
      )}

      {error && (
        <p role="alert" data-testid="waiting-error" className="mt-4 text-sm text-red-600">
          {error}
        </p>
      )}

      {!loading && !error && items.length === 0 && (
        <div
          data-testid="waiting-empty"
          className="mt-4 rounded border border-dashed border-gray-300 p-8 text-center text-gray-500"
        >
          대기중인 신청이 없습니다.
        </div>
      )}

      {!loading && items.length > 0 && (
        <ul data-testid="waiting-list" className="mt-4 grid gap-3">
          {items.map((item) => (
            <li
              key={item.enrollmentId}
              data-testid={`waiting-row-${item.enrollmentId}`}
              className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4"
            >
              <div>
                <h3 className="font-medium">{item.cohortTitle ?? '코호트'}</h3>
                <p className="mt-1 text-sm text-gray-600">
                  {item.menteeName ?? '멘티'}
                  {item.menteeNickname ? ` (${item.menteeNickname})` : ''} · 신청일{' '}
                  {item.createdAt.slice(0, 10)}
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  data-testid={`waiting-approve-${item.enrollmentId}`}
                  disabled={isProcessing(item.enrollmentId)}
                  onClick={() => setConfirmingId(item.enrollmentId)}
                  className="rounded bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent-hover disabled:opacity-50"
                >
                  승인
                </button>
                <button
                  type="button"
                  data-testid={`waiting-reject-${item.enrollmentId}`}
                  disabled={isProcessing(item.enrollmentId)}
                  onClick={() => runAction(item.enrollmentId, 'reject')}
                  className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  거절
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {confirmingId != null && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="approve-confirm-title"
          data-testid="approve-confirm-dialog"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
        >
          <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h3 id="approve-confirm-title" className="text-base font-semibold">
              정원 초과 승인 확인
            </h3>
            <p className="mt-2 text-sm text-gray-600">
              대기 신청 승인은 코호트 정원을 초과할 수 있습니다. 승인하시겠습니까?
            </p>
            <div className="mt-4 flex justify-end gap-2">
              <button
                type="button"
                data-testid="approve-confirm-cancel"
                onClick={() => setConfirmingId(null)}
                className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
              >
                취소
              </button>
              <button
                type="button"
                data-testid="approve-confirm-ok"
                onClick={confirmApprove}
                className="rounded bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent-hover"
              >
                승인
              </button>
            </div>
          </div>
        </div>
      )}

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
