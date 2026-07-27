# Tech Stack Decisions — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 devsecops·quality
> 상위 입력: `U6-admin-metrics/functional-design/business-logic-model.md`(집계·이력 쿼리), `business-rules.md`(R-U6-*), `requirements-analysis/requirements.md`(NFR-1/7)

## 1. 상속 스택
U1-foundation 표준 스택 상속. U6은 신규 저장소·엔티티 없음(읽기 전용).

## 2. U6 고유 기술 선택
| 항목 | 선택 | 근거 |
|---|---|---|
| 집계 | 읽기 전용 JPQL/native 집계 쿼리(`MetricsRepository`) | R-U6-04~07, 실시간 계산 |
| 이력 조회 | Spring Data `Pageable`(기본 20건) + 조인 쿼리 | R-U6-09/10 |
| 관리자 인가 | `@PreAuthorize("hasRole('ADMIN')")` | R-U6-01(U1 R-U1-16a 상속) |
| 백분율 표시 | 정수 산술 후 표시단 반올림(내부 판정 미사용) | R-U6-04/05 |

## 3. 보류/확장
- U1 보류 상속. 지표 캐시·머티리얼라이즈드 뷰는 파일럿 미도입(실시간 계산). 대규모 데이터 시 도입 검토(확장 후속).
