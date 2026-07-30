import { useState } from 'react';
import type { FormEvent } from 'react';
import type { CohortCreateRequest } from '../api/types';

export interface CohortFormValue {
  title: string;
  description: string;
  capacity: number;
  startDate: string;
  endDate: string;
  sessionCount: number;
}

const EMPTY: CohortFormValue = {
  title: '',
  description: '',
  capacity: 1,
  startDate: '',
  endDate: '',
  sessionCount: 1,
};

/**
 * 코호트 개설/수정 공용 폼 (frontend-components §2.2).
 *
 * 클라이언트 검증은 UX 보조이며 서버 검증(R-U2-01~10)이 권위. 폼 라벨·aria-describedby, 제출 중 버튼 비활성화로 중복 제출을 방지한다(접근성 §4).
 */
export function CohortForm({
  initial,
  submitLabel,
  submitting,
  serverError,
  onSubmit,
}: {
  initial?: Partial<CohortFormValue>;
  submitLabel: string;
  submitting: boolean;
  serverError?: string | null;
  onSubmit: (req: CohortCreateRequest) => void;
}) {
  const [value, setValue] = useState<CohortFormValue>({ ...EMPTY, ...initial });
  const [clientError, setClientError] = useState<string | null>(null);

  function update<K extends keyof CohortFormValue>(key: K, v: CohortFormValue[K]) {
    setValue((prev) => ({ ...prev, [key]: v }));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setClientError(null);

    if (!value.title.trim()) {
      setClientError('제목을 입력하세요');
      return;
    }
    if (value.title.length > 200) {
      setClientError('제목은 최대 200자입니다');
      return;
    }
    if (value.capacity < 1) {
      setClientError('정원은 1 이상이어야 합니다');
      return;
    }
    if (value.sessionCount < 1 || value.sessionCount > 100) {
      setClientError('회차 수는 1 이상 100 이하여야 합니다');
      return;
    }
    if (!value.startDate || !value.endDate) {
      setClientError('시작일과 종료일을 입력하세요');
      return;
    }
    if (value.endDate < value.startDate) {
      setClientError('종료일은 시작일과 같거나 이후여야 합니다');
      return;
    }

    onSubmit({
      title: value.title.trim(),
      description: value.description.trim() || null,
      capacity: value.capacity,
      startDate: value.startDate,
      endDate: value.endDate,
      sessionCount: value.sessionCount,
    });
  }

  const error = clientError ?? serverError ?? null;

  return (
    <form onSubmit={handleSubmit} data-testid="cohort-form" noValidate className="space-y-4">
      <div>
        <label htmlFor="cohort-title" className="block text-sm font-medium">
          제목
        </label>
        <input
          id="cohort-title"
          data-testid="cohort-title"
          type="text"
          value={value.title}
          maxLength={200}
          onChange={(e) => update('title', e.target.value)}
          aria-describedby={error ? 'cohort-form-error' : undefined}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
        />
      </div>

      <div>
        <label htmlFor="cohort-description" className="block text-sm font-medium">
          설명 (선택)
        </label>
        <textarea
          id="cohort-description"
          data-testid="cohort-description"
          value={value.description}
          onChange={(e) => update('description', e.target.value)}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="cohort-capacity" className="block text-sm font-medium">
            정원
          </label>
          <input
            id="cohort-capacity"
            data-testid="cohort-capacity"
            type="number"
            min={1}
            value={value.capacity}
            onChange={(e) => update('capacity', Number(e.target.value))}
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
          />
        </div>
        <div>
          <label htmlFor="cohort-session-count" className="block text-sm font-medium">
            회차 수
          </label>
          <input
            id="cohort-session-count"
            data-testid="cohort-session-count"
            type="number"
            min={1}
            max={100}
            value={value.sessionCount}
            onChange={(e) => update('sessionCount', Number(e.target.value))}
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="cohort-start-date" className="block text-sm font-medium">
            시작일
          </label>
          <input
            id="cohort-start-date"
            data-testid="cohort-start-date"
            type="date"
            value={value.startDate}
            onChange={(e) => update('startDate', e.target.value)}
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
          />
        </div>
        <div>
          <label htmlFor="cohort-end-date" className="block text-sm font-medium">
            종료일
          </label>
          <input
            id="cohort-end-date"
            data-testid="cohort-end-date"
            type="date"
            value={value.endDate}
            onChange={(e) => update('endDate', e.target.value)}
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
          />
        </div>
      </div>

      {error && (
        <p id="cohort-form-error" role="alert" data-testid="cohort-form-error" className="text-sm text-red-600">
          {error}
        </p>
      )}

      <button
        type="submit"
        data-testid="cohort-submit"
        disabled={submitting}
        className="w-full rounded bg-accent px-4 py-2 text-white hover:bg-accent-hover disabled:opacity-50"
      >
        {submitting ? '처리 중...' : submitLabel}
      </button>
    </form>
  );
}
