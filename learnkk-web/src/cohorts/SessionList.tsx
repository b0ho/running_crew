import type { SessionDto } from '../api/types';
import { SessionStatusBadge } from './StatusBadge';

/** 회차 목록 (frontend-components §2.3). seq·status 배지(색+텍스트). */
export function SessionList({ sessions }: { sessions: SessionDto[] }) {
  if (sessions.length === 0) {
    return (
      <p data-testid="session-empty" className="text-sm text-gray-500">
        회차가 없습니다.
      </p>
    );
  }
  return (
    <ul data-testid="session-list" className="divide-y divide-gray-100">
      {sessions.map((s) => (
        <li key={s.id} data-testid={`session-item-${s.seq}`} className="flex items-center justify-between py-2">
          <span className="text-sm">{s.seq}회차</span>
          <SessionStatusBadge status={s.status} label={s.statusLabel} />
        </li>
      ))}
    </ul>
  );
}
