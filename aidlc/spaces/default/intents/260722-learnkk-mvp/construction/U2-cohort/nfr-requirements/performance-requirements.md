# Performance Requirements — U2 cohort (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 quality
> 상위 입력: `U2-cohort/functional-design/business-logic-model.md`(조회·생성), `business-rules.md`(R-U2-19 목록), `requirements-analysis/requirements.md`(NFR-4)
> 전제: <100명 파일럿, 로컬 단일 서버(U1 성능 전제 상속).

## 1. 응답 시간 목표 (목표 latency)
| 연산 | 목표 | 비고 |
|---|---|---|
| 코호트 목록/탐색(20건 페이지) | ≤ 300ms | 인덱스+페이지네이션 |
| 코호트 상세(회차+공지 포함) | ≤ 350ms | 조인 |
| 코호트 개설(+회차 N건 생성) | ≤ 500ms | N 소량(수~수십) |
| 코호트 수정/공지 작성 | ≤ 300ms | |

- 통계적 p95는 파일럿에서 강제하지 않음(U1 방침 상속): 목표 대비 스모크 확인, 엄밀 부하 검증은 Operation performance-validation.

## 2. 처리량 / 리소스
- 코호트 데이터 소량. 목록 조회에 상태·mentorId 인덱스 활용. N+1 쿼리 방지(fetch join 또는 batch size).
- 회차 벌크 생성은 단일 트랜잭션 batch insert로 라운드트립 최소화.

## 3. 검증
- 목록/상세/개설 경로 스모크 측정. N+1 회귀 방지 테스트(쿼리 수 단언) 권장.
