/**
 * 지표 카드 (frontend-components §2.1, 접근성 §4).
 *
 * 수치와 라벨을 병기하고, 스크린리더용 설명 텍스트(description)를 제공한다. 카드 전체를 group 역할로 묶어 라벨→값 순서로 읽히게 한다.
 */
export function MetricCard({
  label,
  value,
  description,
  testId,
}: {
  label: string;
  value: string;
  description?: string;
  testId: string;
}) {
  return (
    <div
      data-testid={testId}
      role="group"
      aria-label={description ?? label}
      className="rounded-lg border border-gray-200 bg-white p-4"
    >
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-gray-900" data-testid={`${testId}-value`}>
        {value}
      </p>
      {description && <span className="sr-only">{description}</span>}
    </div>
  );
}
