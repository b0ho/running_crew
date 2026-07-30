import { useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { adminMetricsApi } from '../api/adminMetricsApi';
import { attendanceApi } from '../api/attendanceApi';
import type { EvidenceHistoryItem } from '../api/types';

/** 파일 크기 사람이 읽기 쉬운 표기 (bytes → KB/MB). */
function formatSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * 증빙 이력 테이블 (frontend-components §2.2, US-15).
 *
 * 관리자에게 회차 증빙 업로드 이력을 최신순·20건 페이지네이션으로 보여준다. 컬럼: 코호트·회차·업로더·형식·크기·업로드일·다운로드. 다운로드는 U1
 * load 경유 스트리밍 엔드포인트(sessionId·evidenceId)로 연결한다. 헤더 scope 지정, 페이지네이션은 키보드 접근 가능한 버튼, 로딩·빈 상태를 명시한다.
 */
export function EvidenceHistoryTable() {
  const [items, setItems] = useState<EvidenceHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    adminMetricsApi
      .listEvidenceHistory({ page })
      .then((result) => {
        if (active) {
          setItems(result.content);
          setTotalPages(result.totalPages);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err instanceof ApiError ? err.message : '증빙 이력을 불러오지 못했습니다');
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
      <h2 className="text-lg font-semibold" data-testid="evidence-history-title">
        증빙 이력
      </h2>

      {loading && (
        <p data-testid="evidence-loading" className="mt-4 text-sm text-gray-400">
          불러오는 중...
        </p>
      )}

      {error && (
        <p role="alert" data-testid="evidence-error" className="mt-4 text-sm text-red-600">
          {error}
        </p>
      )}

      {!loading && !error && items.length === 0 && (
        <div
          data-testid="evidence-empty"
          className="mt-4 rounded border border-dashed border-gray-300 p-8 text-center text-gray-500"
        >
          증빙 이력이 없습니다.
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="mt-4 overflow-x-auto">
          <table data-testid="evidence-table" className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-200 text-gray-500">
                <th scope="col" className="py-2 pr-4">
                  코호트
                </th>
                <th scope="col" className="py-2 pr-4">
                  회차
                </th>
                <th scope="col" className="py-2 pr-4">
                  업로더
                </th>
                <th scope="col" className="py-2 pr-4">
                  형식
                </th>
                <th scope="col" className="py-2 pr-4">
                  크기
                </th>
                <th scope="col" className="py-2 pr-4">
                  업로드일
                </th>
                <th scope="col" className="py-2 pr-4">
                  다운로드
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr
                  key={item.evidenceId}
                  data-testid={`evidence-row-${item.evidenceId}`}
                  className="border-b border-gray-100"
                >
                  <td className="py-2 pr-4">{item.cohortTitle}</td>
                  <td className="py-2 pr-4">{item.sessionSeq}회차</td>
                  <td className="py-2 pr-4">{item.uploadedBy}</td>
                  <td className="py-2 pr-4">{item.mimeType}</td>
                  <td className="py-2 pr-4">{formatSize(item.size)}</td>
                  <td className="py-2 pr-4">{item.createdAt.slice(0, 10)}</td>
                  <td className="py-2 pr-4">
                    <a
                      data-testid={`evidence-download-${item.evidenceId}`}
                      href={attendanceApi.evidenceDownloadUrl(item.sessionId, item.evidenceId)}
                      download
                      className="text-accent underline hover:text-accent-hover"
                    >
                      다운로드
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !error && totalPages > 1 && (
        <nav
          data-testid="evidence-pagination"
          aria-label="증빙 이력 페이지네이션"
          className="mt-4 flex items-center justify-center gap-3"
        >
          <button
            type="button"
            data-testid="evidence-prev"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(p - 1, 0))}
            className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            이전
          </button>
          <span data-testid="evidence-page-indicator" className="text-sm text-gray-600">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            data-testid="evidence-next"
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
