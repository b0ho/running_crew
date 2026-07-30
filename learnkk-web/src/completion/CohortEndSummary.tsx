import type { CohortEndSummaryDto } from '../api/types';

/**
 * 코호트 종료 요약 (US-4 종료 결과, 멘토).
 *
 * 종료 직후 수료자 수/전체 확정 멘티 수·정산 조건 충족 여부·발급 증서 수를 표시한다. 색+텍스트를 병기해 접근성을 확보한다(§4).
 */
export function CohortEndSummary({ summary }: { summary: CohortEndSummaryDto }) {
  return (
    <section
      data-testid="cohort-end-summary"
      className="rounded border border-gray-200 bg-gray-50 p-4"
    >
      <h3 className="text-sm font-semibold text-gray-800">종료 요약</h3>
      <dl className="mt-2 grid grid-cols-2 gap-2 text-sm text-gray-700">
        <dt>수료자</dt>
        <dd data-testid="summary-certified">
          {summary.certifiedCount}명 / 전체 {summary.totalConfirmed}명
        </dd>
        <dt>미수료</dt>
        <dd data-testid="summary-not-certified">{summary.notCertifiedCount}명</dd>
        <dt>발급 증서</dt>
        <dd data-testid="summary-issued">{summary.issuedCertificateCount}장</dd>
        <dt>정산 조건</dt>
        <dd data-testid="summary-settlement">
          {summary.settlementSatisfied ? (
            <span className="text-green-700">정산 조건 충족</span>
          ) : (
            <span className="text-gray-500">정산 조건 미충족</span>
          )}
        </dd>
      </dl>
    </section>
  );
}
