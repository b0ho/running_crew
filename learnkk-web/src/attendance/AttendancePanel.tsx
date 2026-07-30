import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { attendanceApi } from '../api/attendanceApi';
import type { CohortAttendanceDto } from '../api/types';
import type { ToastMessage } from '../common/Toast';
import { ProgressSummary } from './ProgressSummary';
import { SessionAttendanceList } from './SessionAttendanceList';

/**
 * 코호트 상세 "진도·출석" 탭 패널 (frontend-components §2, U2 CohortDetailPage 플레이스홀더 대체).
 *
 * 진도·출석을 조회해 진도 요약 + 회차별 출석 목록을 렌더한다. 멘토에게는 회차별 증빙 업로드를 노출하며, 업로드 성공 시 즉시 재조회해 인증 배지를 갱신한다.
 */
export function AttendancePanel({
  cohortId,
  isMentor,
  onToast,
}: {
  cohortId: number;
  isMentor: boolean;
  onToast: (message: ToastMessage) => void;
}) {
  const [attendance, setAttendance] = useState<CohortAttendanceDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    attendanceApi
      .getAttendance(cohortId)
      .then(setAttendance)
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : '진도·출석을 불러오지 못했습니다'),
      );
  }, [cohortId]);

  useEffect(() => {
    load();
  }, [load]);

  if (error && !attendance) {
    return (
      <p role="alert" data-testid="attendance-error" className="text-sm text-red-600">
        {error}
      </p>
    );
  }

  if (!attendance) {
    return (
      <p data-testid="attendance-loading" className="text-sm text-gray-500">
        불러오는 중...
      </p>
    );
  }

  return (
    <div data-testid="attendance-panel">
      <ProgressSummary attendance={attendance} />
      <SessionAttendanceList
        sessions={attendance.sessions}
        isMentor={isMentor}
        onUploaded={load}
        onToast={onToast}
      />
    </div>
  );
}
