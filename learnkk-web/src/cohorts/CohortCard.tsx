import { useNavigate } from 'react-router-dom';
import type { CohortSummaryDto } from '../api/types';
import { CohortStatusBadge } from './StatusBadge';

/** 코호트 요약 카드 (frontend-components §2.1). 클릭 시 상세로 이동. */
export function CohortCard({ cohort }: { cohort: CohortSummaryDto }) {
  const navigate = useNavigate();

  return (
    <button
      type="button"
      data-testid={`cohort-card-${cohort.id}`}
      onClick={() => navigate(`/cohorts/${cohort.id}`)}
      className="block w-full rounded-lg border border-gray-200 bg-white p-4 text-left hover:border-accent"
    >
      <div className="flex items-center justify-between">
        <h3 className="font-medium">{cohort.title}</h3>
        <CohortStatusBadge status={cohort.status} label={cohort.statusLabel} />
      </div>
      <dl className="mt-2 text-sm text-gray-600">
        <div className="flex gap-2">
          <dt>기간</dt>
          <dd>
            {cohort.startDate} ~ {cohort.endDate}
          </dd>
        </div>
        <div className="flex gap-2">
          <dt>정원</dt>
          <dd>{cohort.capacity}명</dd>
          <dt className="ml-2">회차</dt>
          <dd>{cohort.sessionCount}회</dd>
        </div>
      </dl>
    </button>
  );
}
