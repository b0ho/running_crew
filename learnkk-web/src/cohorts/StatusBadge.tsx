import type { CohortStatus, EnrollmentStatus, SessionStatus } from '../api/types';

// 색만으로 의미를 전달하지 않도록 항상 텍스트 라벨을 함께 렌더한다(접근성 §4).
const COHORT_CLASSES: Record<CohortStatus, string> = {
  RECRUITING: 'bg-blue-100 text-blue-800',
  ONGOING: 'bg-green-100 text-green-800',
  CLOSED: 'bg-gray-200 text-gray-700',
};
const COHORT_LABELS: Record<CohortStatus, string> = {
  RECRUITING: '모집중',
  ONGOING: '진행중',
  CLOSED: '종료됨',
};

const SESSION_CLASSES: Record<SessionStatus, string> = {
  SCHEDULED: 'bg-gray-100 text-gray-700',
  VERIFIED: 'bg-green-100 text-green-800',
};
const SESSION_LABELS: Record<SessionStatus, string> = {
  SCHEDULED: '예정',
  VERIFIED: '인증',
};

/** 코호트 상태 배지 (모집중/진행중/종료됨) — 색+텍스트 병기. */
export function CohortStatusBadge({ status, label }: { status: CohortStatus; label?: string }) {
  return (
    <span
      data-testid={`cohort-status-${status}`}
      className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${COHORT_CLASSES[status]}`}
    >
      {label ?? COHORT_LABELS[status]}
    </span>
  );
}

/** 회차 상태 배지 (예정/인증) — 색+텍스트 병기. */
export function SessionStatusBadge({ status, label }: { status: SessionStatus; label?: string }) {
  return (
    <span
      data-testid={`session-status-${status}`}
      className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${SESSION_CLASSES[status]}`}
    >
      {label ?? SESSION_LABELS[status]}
    </span>
  );
}

const ENROLLMENT_CLASSES: Record<EnrollmentStatus, string> = {
  CONFIRMED: 'bg-green-100 text-green-800',
  WAITING: 'bg-amber-100 text-amber-800',
  REJECTED: 'bg-gray-200 text-gray-700',
};
const ENROLLMENT_LABELS: Record<EnrollmentStatus, string> = {
  CONFIRMED: '확정',
  WAITING: '대기중',
  REJECTED: '거절',
};

/** 참여 상태 배지 (확정/대기중/거절) — 색+텍스트 병기(색만으로 상태 전달 금지, 접근성 §4). */
export function EnrollmentStatusBadge({
  status,
  label,
}: {
  status: EnrollmentStatus;
  label?: string;
}) {
  return (
    <span
      data-testid={`enrollment-status-${status}`}
      className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${ENROLLMENT_CLASSES[status]}`}
    >
      {label ?? ENROLLMENT_LABELS[status]}
    </span>
  );
}
