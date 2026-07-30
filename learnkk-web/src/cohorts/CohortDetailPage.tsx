import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/ApiClient';
import { cohortApi } from '../api/cohortApi';
import type { AnnouncementCreateRequest, CohortDetailDto } from '../api/types';
import { useAuth } from '../auth/authContext';
import { AttendancePanel } from '../attendance/AttendancePanel';
import { Toast, type ToastMessage } from '../common/Toast';
import { ResponsiveTabBar } from '../shell/ResponsiveTabBar';
import { AnnouncementForm } from './AnnouncementForm';
import { AnnouncementList } from './AnnouncementList';
import { CohortStatusBadge } from './StatusBadge';

type Tab = 'announcements' | 'progress' | 'members' | 'reports';

const TABS: { key: Tab; label: string }[] = [
  { key: 'announcements', label: '공지' },
  { key: 'progress', label: '진도·출석' },
  { key: 'members', label: '멤버' },
  { key: 'reports', label: '보고서' },
];

/**
 * 코호트 상세 페이지 (라우트 /cohorts/:id).
 *
 * 공지 탭·회차 목록·기본정보는 U2 가 렌더하고, 진도·출석/멤버/보고서 탭 상세는 상위 유닛(U4/U5)이 채운다. 멘토에게만 공지 작성·수정·시작 진입점을
 * 노출하되 서버 인가(R-U2-07/15)가 최종 방어선이다.
 */
export function CohortDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();

  const [detail, setDetail] = useState<CohortDetailDto | null>(null);
  const [tab, setTab] = useState<Tab>('announcements');
  const [error, setError] = useState<string | null>(null);
  const [announcementError, setAnnouncementError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [starting, setStarting] = useState(false);
  const [toast, setToast] = useState<ToastMessage | null>(null);

  const load = useCallback(() => {
    if (!id) return;
    cohortApi
      .get(Number(id))
      .then(setDetail)
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : '코호트를 불러오지 못했습니다'),
      );
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const isMentor = Boolean(detail && currentUser && detail.mentorId === currentUser.id);

  async function handleAnnouncementSubmit(req: AnnouncementCreateRequest) {
    if (!id) return;
    setSubmitting(true);
    setAnnouncementError(null);
    try {
      await cohortApi.createAnnouncement(Number(id), req);
      load();
    } catch (err) {
      setAnnouncementError(err instanceof ApiError ? err.message : '공지를 등록하지 못했습니다');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStart() {
    if (!id) return;
    setStarting(true);
    setError(null);
    try {
      await cohortApi.start(Number(id));
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '코호트를 시작하지 못했습니다');
    } finally {
      setStarting(false);
    }
  }

  if (error && !detail) {
    return (
      <div className="min-h-screen pb-20 md:pb-0">
        <ResponsiveTabBar />
        <main className="mx-auto max-w-3xl p-6">
          <p role="alert" data-testid="cohort-detail-error" className="text-sm text-red-600">
            {error}
          </p>
        </main>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="min-h-screen pb-20 md:pb-0">
        <ResponsiveTabBar />
        <main className="mx-auto max-w-3xl p-6">
          <p data-testid="cohort-detail-loading" className="text-sm text-gray-500">
            불러오는 중...
          </p>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      <ResponsiveTabBar />
      <main className="mx-auto max-w-3xl p-6">
        <header className="mb-4">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-semibold" data-testid="cohort-detail-title">
              {detail.title}
            </h1>
            <CohortStatusBadge status={detail.status} label={detail.statusLabel} />
          </div>
          <p className="mt-1 text-sm text-gray-600">
            {detail.startDate} ~ {detail.endDate} · 정원 {detail.capacity}명 · {detail.sessionCount}회차
          </p>
          {detail.description && (
            <p className="mt-2 whitespace-pre-wrap text-sm text-gray-700">{detail.description}</p>
          )}

          {isMentor && (
            <div className="mt-3 flex gap-2">
              <button
                type="button"
                data-testid="edit-cohort"
                onClick={() => navigate(`/cohorts/${detail.id}/edit`)}
                className="rounded border border-gray-300 px-3 py-1 text-sm hover:border-accent"
              >
                수정
              </button>
              {detail.status === 'RECRUITING' && (
                <button
                  type="button"
                  data-testid="start-cohort"
                  disabled={starting}
                  onClick={handleStart}
                  className="rounded bg-accent px-3 py-1 text-sm text-white hover:bg-accent-hover disabled:opacity-50"
                >
                  {starting ? '시작 중...' : '코호트 시작'}
                </button>
              )}
            </div>
          )}
          {error && (
            <p role="alert" data-testid="cohort-action-error" className="mt-2 text-sm text-red-600">
              {error}
            </p>
          )}
        </header>

        <nav role="tablist" className="mb-4 flex border-b border-gray-200">
          {TABS.map((t) => (
            <button
              key={t.key}
              role="tab"
              aria-selected={tab === t.key}
              data-testid={`tab-${t.key}`}
              onClick={() => setTab(t.key)}
              className={`px-4 py-2 text-sm ${
                tab === t.key ? 'border-b-2 border-accent font-medium' : 'text-gray-500'
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>

        {tab === 'announcements' && (
          <section data-testid="tab-panel-announcements" className="space-y-4">
            {isMentor && (
              <AnnouncementForm
                submitting={submitting}
                serverError={announcementError}
                onSubmit={handleAnnouncementSubmit}
              />
            )}
            <AnnouncementList announcements={detail.recentAnnouncements} />
          </section>
        )}

        {tab === 'progress' && (
          <section data-testid="tab-panel-progress">
            <AttendancePanel cohortId={detail.id} isMentor={isMentor} onToast={setToast} />
          </section>
        )}

        {tab === 'members' && (
          <section data-testid="tab-panel-members" className="text-sm text-gray-500">
            멤버 목록은 다음 단계(U3)에서 제공됩니다.
          </section>
        )}

        {tab === 'reports' && (
          <section data-testid="tab-panel-reports" className="text-sm text-gray-500">
            보고서는 다음 단계(U5)에서 제공됩니다.
          </section>
        )}
      </main>
      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
