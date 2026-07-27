# Performance Requirements — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 quality
> 상위 입력: `U6-admin-metrics/functional-design/business-logic-model.md`(집계 쿼리 §2), `business-rules.md`(R-U6-04~07), `requirements-analysis/requirements.md`(NFR-4)
> 전제: <100명 파일럿, 로컬 단일 서버(U1 상속).

## 1. 응답 시간 목표 (목표 latency)
| 연산 | 목표 | 비고 |
|---|---|---|
| 운영 지표 집계(overview) | ≤ 500ms | 4개 집계 쿼리(종료 코호트 대상) |
| 증빙 이력(20건 페이지) | ≤ 350ms | 조인+페이지네이션 |
| 보고서 이력(20건 페이지) | ≤ 350ms | 조인+페이지네이션 |

- 통계적 p95는 파일럿 강제 안 함(U1 방침): 목표 대비 스모크. 엄밀 검증은 Operation performance-validation.

## 2. 집계 성능
- 집계는 종료됨 코호트 대상 COUNT/SUM 쿼리. 위 목표는 **scalability §0 기준 인덱스(파일럿 필수)** 존재를 전제한다: `cohort(status)`, `enrollment(cohort_id,status)`, `certificate(cohort_id)`, `attendance_evidence(created_at)`, `final_report(submitted_at)`. 이 인덱스 없이는 목표 재현이 보장되지 않는다.
- 캐시 없이 실시간 계산(INV-U6-2, FR-11 데이터 일치 보장). 데이터 소량이라 캐시 불필요.
- 이력 조회는 페이지네이션(20건)으로 대량 데이터에도 응답 시간 안정.

## 3. 검증
- 지표·이력 경로 스모크 측정. 대규모 데이터 시 집계 성능은 확장 시 재평가(캐시/머티리얼라이즈드 뷰). 파일럿은 실시간으로 충분.
