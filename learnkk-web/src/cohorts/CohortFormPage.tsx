import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/ApiClient';
import { cohortApi } from '../api/cohortApi';
import type { CohortCreateRequest } from '../api/types';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';
import { CapacityWarningBanner } from './CapacityWarningBanner';
import { CohortForm } from './CohortForm';
import type { CohortFormValue } from './CohortForm';

/**
 * 코호트 개설/수정 페이지 (라우트 /cohorts/new, /cohorts/:id/edit).
 *
 * 수정 모드는 기존 코호트를 프리필한다. 409(CAPACITY_BELOW_CONFIRMED·COHORT_CLOSED·SESSION_VERIFIED_LOCK) 및
 * 400(VALIDATION_ERROR)은 인라인 에러로 표시하고, 정원 축소 경고(warnings)는 배너로 노출한다.
 */
export function CohortFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const editMode = Boolean(id);

  const [initial, setInitial] = useState<Partial<CohortFormValue> | undefined>(
    editMode ? undefined : {},
  );
  const [loading, setLoading] = useState(editMode);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);

  useEffect(() => {
    if (!editMode || !id) {
      return;
    }
    let active = true;
    cohortApi
      .get(Number(id))
      .then((detail) => {
        if (!active) return;
        setInitial({
          title: detail.title,
          description: detail.description ?? '',
          capacity: detail.capacity,
          startDate: detail.startDate,
          endDate: detail.endDate,
          sessionCount: detail.sessionCount,
        });
      })
      .catch((err) => {
        if (active) {
          setServerError(err instanceof ApiError ? err.message : '코호트를 불러오지 못했습니다');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [editMode, id]);

  async function handleSubmit(req: CohortCreateRequest) {
    setSubmitting(true);
    setServerError(null);
    setWarnings([]);
    try {
      if (editMode && id) {
        const updated = await cohortApi.update(Number(id), req);
        if (updated.warnings.length > 0) {
          setWarnings(updated.warnings);
        } else {
          navigate(`/cohorts/${updated.id}`);
        }
      } else {
        const created = await cohortApi.create(req);
        navigate(`/cohorts/${created.id}`);
      }
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : '요청을 처리하지 못했습니다');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      <ResponsiveTabBar />
      <main className="mx-auto max-w-2xl p-6">
        <h1 className="mb-4 text-xl font-semibold" data-testid="cohort-form-title">
          {editMode ? '코호트 수정' : '코호트 개설'}
        </h1>

        {warnings.length > 0 && (
          <div className="mb-4 space-y-3">
            <CapacityWarningBanner warnings={warnings} />
            <button
              type="button"
              data-testid="go-detail"
              onClick={() => navigate(`/cohorts/${id}`)}
              className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent-hover"
            >
              상세로 이동
            </button>
          </div>
        )}

        {loading ? (
          <p data-testid="cohort-form-loading" className="text-sm text-gray-500">
            불러오는 중...
          </p>
        ) : (
          <CohortForm
            initial={initial}
            submitLabel={editMode ? '수정 저장' : '개설'}
            submitting={submitting}
            serverError={serverError}
            onSubmit={handleSubmit}
          />
        )}
      </main>
    </div>
  );
}
