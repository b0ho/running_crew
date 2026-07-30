import { useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { adminMetricsApi } from '../api/adminMetricsApi';
import type { ReportHistoryItem } from '../api/types';

/**
 * 보고서 이력 테이블 (frontend-components §2.3, US-15).
 *
 * 관리자에게 최종 보고서 제출 이력을 최신순·20건 페이지네이션으로 보여준다. 컬럼: 코호트·작성자·첨부유무·제출일. 증빙 이력과 별도 탭이다(R-U6-08,
 * FR-10 분리 조회). 헤더 scope 지정, 페이지네이션은 키보드 접근 가능한 버튼, 로딩·빈 상태를 명시한다.
 */
export function ReportHistoryTable() {
  const [items, setItems] = useState<ReportHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    adminMetricsApi
      .listReportHistory({ page })
      .then((result) => {
        if (active) {
          setItems(result.content);
          setTotalPages(result.totalPages);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err instanceof ApiError ? err.message : '보고서 이력을 불러오지 못했습니다');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [page]);

  return (
    <div>
      <h2 className="text-lg font-semibold" data-testid="report-history-title">
        보고서 이력
      </h2>

      {loading && (
        <p data-testid="report-loading" className="mt-4 text-sm text-gray-400">
          불러오는 중...
        </p>
      )}

      {error && (
        <p role="alert" data-testid="report-error" className="mt-4 text-sm text-red-600">
          {error}
        </p>
      )}

      {!loading && !error && items.length === 0 && (
        <div
          data-testid="report-empty"
          className="mt-4 rounded border border-dashed border-gray-300 p-8 text-center text-gray-500"
        >
          보고서 이력이 없습니다.
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="mt-4 overflow-x-auto">
          <table data-testid="report-table" className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-200 text-gray-500">
                <th scope="col" className="py-2 pr-4">
                  코호트
                </th>
                <th scope="col" className="py-2 pr-4">
                  작성자
                </th>
                <th scope="col" className="py-2 pr-4">
                  첨부
                </th>
                <th scope="col" className="py-2 pr-4">
                  제출일
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr
                  key={item.reportId}
                  data-testid={`report-row-${item.reportId}`}
                  className="border-b border-gray-100"
                >
                  <td className="py-2 pr-4">{item.cohortTitle}</td>
                  <td className="py-2 pr-4">{item.authorName}</td>
                  <td className="py-2 pr-4" data-testid={`report-attachment-${item.reportId}`}>
                    {item.hasAttachment ? '있음' : '없음'}
                  </td>
                  <td className="py-2 pr-4">{item.submittedAt.slice(0, 10)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !error && totalPages > 1 && (
        <nav
          data-testid="report-pagination"
          aria-label="보고서 이력 페이지네이션"
          className="mt-4 flex items-center justify-center gap-3"
        >
          <button
            type="button"
            data-testid="report-prev"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(p - 1, 0))}
            className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            이전
          </button>
          <span data-testid="report-page-indicator" className="text-sm text-gray-600">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            data-testid="report-next"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
            className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            다음
          </button>
        </nav>
      )}
    </div>
  );
}
