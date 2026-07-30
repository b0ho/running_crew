import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';
import type { ReportDto } from '../api/types';

/**
 * 최종 보고서 이력 (US-11/15, 참여자·관리자).
 *
 * 제출된 보고서를 최신순으로 표시한다. 첨부 존재 여부를 표기한다(첨부 다운로드 엔드포인트는 파일럿 백엔드 계약에 미포함 — 존재 표시만). {@code reloadKey}
 * 가 바뀌면 목록을 다시 불러온다(제출 직후 갱신).
 */
export function ReportList({ cohortId, reloadKey = 0 }: { cohortId: number; reloadKey?: number }) {
  const [reports, setReports] = useState<ReportDto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    completionApi
      .listReports(cohortId)
      .then((page) => {
        setReports(page.content);
        setError(null);
      })
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : '보고서 이력을 불러오지 못했습니다'),
      )
      .finally(() => setLoading(false));
  }, [cohortId]);

  useEffect(() => {
    load();
  }, [load, reloadKey]);

  if (loading) {
    return (
      <p data-testid="report-list-loading" className="text-sm text-gray-500">
        불러오는 중...
      </p>
    );
  }

  if (error) {
    return (
      <p role="alert" data-testid="report-list-error" className="text-sm text-red-600">
        {error}
      </p>
    );
  }

  if (reports.length === 0) {
    return (
      <p data-testid="report-list-empty" className="text-sm text-gray-500">
        아직 제출된 보고서가 없습니다.
      </p>
    );
  }

  return (
    <ul data-testid="report-list" className="space-y-3">
      {reports.map((report) => (
        <li
          key={report.id}
          data-testid={`report-item-${report.id}`}
          className="rounded border border-gray-200 p-3"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500">{report.submittedAt}</span>
            {report.hasAttachment && (
              <span
                data-testid={`report-attachment-${report.id}`}
                className="rounded bg-accent/10 px-2 py-0.5 text-xs text-accent"
              >
                첨부 있음
              </span>
            )}
          </div>
          <p className="mt-1 whitespace-pre-wrap text-sm text-gray-800">{report.body}</p>
        </li>
      ))}
    </ul>
  );
}
