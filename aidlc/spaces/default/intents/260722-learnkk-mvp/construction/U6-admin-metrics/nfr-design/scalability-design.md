# Scalability Design — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 서포트 aws-platform·quality
> 상위 입력: `nfr-requirements/scalability-requirements.md`(기준 인덱스·확장 전략·트리거), `functional-design/business-logic-model.md`(읽기 전용), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 인스턴스, <100명(U1 상속). U6은 읽기 전용 소비자.

## 1. 확장 아키텍처(파일럿)

`scalability-requirements.md` §1:
- U6은 읽기 전용 소비자. 데이터 규모는 소스 유닛(U2~U5)에 종속. 파일럿 규모에서 집계·이력 쿼리 비용 낮음(§2 기준 인덱스 전제).
- 파일럿은 단일 인스턴스·수직 확장만(U1 상속).

## 2. 다중 인스턴스 안전성 & 기준 인덱스

`scalability-requirements.md` §0/§2:
- **다중 인스턴스 안전**: U6은 상태 비저장·읽기 전용이라 인스턴스 로컬 상태가 없다 → 수평 확장 시 U6 자체 제약 없음(세션은 U1 트리거).
- **기준 인덱스(파일럿 필수)**: `cohort(status)`, `enrollment(cohort_id, status)`, `certificate(cohort_id)`, `attendance_evidence(created_at)`, `final_report(submitted_at)` — 소스 유닛 소유이나 U6 조회 성능의 전제(`performance-design.md` §2와 동일 목록). 이 인덱스 없이는 집계·이력 목표가 보장되지 않으므로 U6 NFR이 요구로 명시.

## 3. 집계 성능 확장

`scalability-requirements.md` §2~3:
- **파일럿**: 실시간 집계(캐시 없음). 데이터 소량이라 충분.
- **확장 시**: 데이터가 대규모(수만+ 코호트/참여)로 성장하면 실시간 집계 비용이 증가 → **지표 캐시(TTL)·머티리얼라이즈드 뷰·사전 집계 테이블** 도입 검토. 이력은 페이지네이션 유지 + 필요 시 `(cohort_id, created_at/submitted_at)` 복합 인덱스 추가.

## 4. 확장 트리거

- 지표 조회 응답이 목표를 지속 초과하거나 데이터가 수만 건+로 성장 시 → 집계 캐시(TTL) 또는 사전 집계 스케줄 도입. 파일럿 규모(<100명)에서는 불필요.
- 이력 대량 축적 시 페이지네이션 유지 + 인덱스 보강.
