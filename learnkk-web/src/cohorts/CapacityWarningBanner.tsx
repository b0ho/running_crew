/** 정원 축소 경고 배너 (frontend-components §2.2, reliability-design warnings[]). */
export function CapacityWarningBanner({ warnings }: { warnings: string[] }) {
  if (!warnings || warnings.length === 0) {
    return null;
  }
  return (
    <div
      role="status"
      data-testid="capacity-warning"
      className="rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800"
    >
      <ul className="list-inside list-disc">
        {warnings.map((w, i) => (
          <li key={i}>{w}</li>
        ))}
      </ul>
    </div>
  );
}
