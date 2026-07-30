import { attendanceApi } from '../api/attendanceApi';
import type { SessionAttendanceDto } from '../api/types';
import type { ToastMessage } from '../common/Toast';
import { SessionStatusBadge } from '../cohorts/StatusBadge';
import { SessionEvidenceUpload } from './SessionEvidenceUpload';

/** 회차별 출석 행 (frontend-components §2.2). seq·상태 배지·증빙 다운로드·(멘토) 업로드. */
function SessionRow({
  session,
  isMentor,
  onUploaded,
  onToast,
}: {
  session: SessionAttendanceDto;
  isMentor: boolean;
  onUploaded: () => void;
  onToast: (message: ToastMessage) => void;
}) {
  return (
    <li data-testid={`session-item-${session.seq}`} className="py-3">
      <div className="flex items-center justify-between">
        <span className="text-sm">{session.seq}회차</span>
        <div className="flex items-center gap-2">
          {session.hasEvidence && session.latestEvidenceId != null && (
            <a
              data-testid={`evidence-download-${session.seq}`}
              href={attendanceApi.evidenceDownloadUrl(session.sessionId, session.latestEvidenceId)}
              download
              className="text-xs text-accent underline hover:text-accent-hover"
            >
              증빙 다운로드
            </a>
          )}
          <SessionStatusBadge status={session.status} label={session.statusLabel} />
        </div>
      </div>
      {isMentor && (
        <SessionEvidenceUpload
          sessionId={session.sessionId}
          onUploaded={onUploaded}
          onToast={onToast}
        />
      )}
    </li>
  );
}

/** 회차 출석 목록 (frontend-components §2.2). */
export function SessionAttendanceList({
  sessions,
  isMentor,
  onUploaded,
  onToast,
}: {
  sessions: SessionAttendanceDto[];
  isMentor: boolean;
  onUploaded: () => void;
  onToast: (message: ToastMessage) => void;
}) {
  if (sessions.length === 0) {
    return (
      <p data-testid="attendance-empty" className="text-sm text-gray-500">
        회차가 없습니다.
      </p>
    );
  }
  return (
    <ul data-testid="session-attendance-list" className="divide-y divide-gray-100">
      {sessions.map((s) => (
        <SessionRow
          key={s.sessionId}
          session={s}
          isMentor={isMentor}
          onUploaded={onUploaded}
          onToast={onToast}
        />
      ))}
    </ul>
  );
}
