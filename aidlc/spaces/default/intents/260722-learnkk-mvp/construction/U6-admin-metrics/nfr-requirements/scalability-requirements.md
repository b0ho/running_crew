# Scalability Requirements — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 aws-platform·quality
> 상위 입력: `U6-admin-metrics/functional-design/business-logic-model.md`(읽기 전용), `business-rules.md`(INV-U6-2), `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 0. 파일럿 구현 전제 — 기준 인덱스 (필수, 확장 아님)

performance-requirements §1의 목표(≤500ms/350ms)를 재현 가능하게 하려면 아래 인덱스를 **code-generation 시 반드시 생성**한다(확장 후속 아님, 파일럿 필수). 이 인덱스들은 business-logic-model §2 집계 쿼리·§3/4 이력 조인이 의존한다.

| 인덱스 | 대상 | 용도 |
|---|---|---|
| `cohort(status)` | Cohort | 집계 범위(CLOSED) 필터, 완주 수 |
| `enrollment(cohort_id, status)` | Enrollment | 확정 멘티 수(수료율 분모) |
| `certificate(cohort_id)` | Certificate | 증서 수 집계 |
| `attendance_evidence(created_at)` | AttendanceEvidence | 증빙 이력 정렬(최신순) |
| `final_report(submitted_at)` | FinalReport | 보고서 이력 정렬(최신순) |

> 이 인덱스는 소스 유닛(U2~U5) 스키마에 속하나, U6 조회 성능의 전제이므로 U6 NFR이 요구사항으로 명시한다. 각 소유 유닛/인프라 단계에서 생성.

## 1. 부하·데이터 규모
- U6은 읽기 전용 소비자. 데이터 규모는 소스 유닛(U2~U5)에 종속. 파일럿 규모에서 집계·이력 쿼리 비용 낮음(§0 기준 인덱스 전제).
- 이력은 페이지네이션(20건)으로 데이터 증가에 견딤.

## 2. 확장 전략 & 다중 인스턴스
- **다중 인스턴스 안전**: U6은 상태 비저장·읽기 전용이라 인스턴스 로컬 상태 없음 → 수평 확장 시 U6 자체 제약 없음(세션은 U1 트리거).
- **집계 성능 확장**: 데이터가 대규모(수만+ 코호트/참여)로 성장하면 실시간 집계 비용 증가 → 지표 캐시·머티리얼라이즈드 뷰·사전 집계 테이블 도입 검토(확장 후속). 파일럿은 실시간 계산.

## 3. 확장 트리거
- 지표 조회 응답이 목표 지속 초과하거나 데이터가 수만 건+로 성장 시: 집계 캐시(TTL) 또는 사전 집계 스케줄 도입. 파일럿 규모(<100명)에서는 불필요.
- 이력 대량 축적 시 페이지네이션 유지 + 필요 시 인덱스 추가(cohortId, createdAt/submittedAt).
