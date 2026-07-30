import type { CohortAttendanceDto } from '../api/types';

/**
 * 진도 요약 (US-10, frontend-components §2.1).
 *
 * 인증/전체 회차와 진도율을 색+수치로 병기한다(색만으로 의미 전달 금지, 접근성 §4). 진도율 바에 aria 속성을 부여한다.
 */
export function ProgressSummary({ attendance }: { attendance: CohortAttendanceDto }) {
  const percent = Math.round(attendance.progressRate * 100);
  return (
    <div data-testid="progress-summary" className="mb-4">
      <div className="mb-1 flex items-center justify-between text-sm">
        <span className="font-medium">진도·출석</span>
        <span data-testid="progress-rate" className="text-gray-700">
          인증 {attendance.verifiedCount} / 전체 {attendance.totalCount} 회차 ({percent}%)
        </span>
      </div>
      <div
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        aria-label={`진도율 ${percent}퍼센트`}
        className="h-2 w-full overflow-hidden rounded bg-gray-200"
      >
        <div className="h-full rounded bg-accent" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}
