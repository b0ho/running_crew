# Scalability Design — U2 cohort (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/scalability-requirements.md`(데이터 규모·확장 전략·트리거), `functional-design/business-logic-model.md`(조회·CRUD), `nfr-requirements/tech-stack-decisions.md`(Pageable)
> 전제: 로컬 단일 인스턴스, <100명(U1 확장 방침 상속).

## 1. 확장 아키텍처(파일럿)

`scalability-requirements.md` §2:
- 파일럿은 U1과 동일 **수직 확장만**. U2는 상태 비저장 조회/CRUD라 **인스턴스 로컬 상태를 두지 않는다** → 다중 인스턴스 확장 시 U2 자체는 세션(U1 트리거) 외 추가 제약이 없다.
- 코호트/회차/공지는 DB에 위임되어 무상태.

## 2. 데이터 확장 & 인덱스 설계

`scalability-requirements.md` §1의 "대량 목록 대비 인덱스 사전 설계"를 확정한다.

| 인덱스 | 대상 쿼리 |
|---|---|
| `cohort(status)` | 목록/탐색(모집중·진행중 필터, R-U2-19) |
| `cohort(mentor_id)` | 멘토 소유 코호트 조회 |
| `cohort(created_at)` | 목록 정렬(기본 desc) |
| `session(cohort_id, seq)` | 상세 회차 로딩·seq 유일성(INV-U2-2) |
| `announcement(cohort_id, created_at)` | 공지 목록 |

- 페이지네이션(20건)으로 데이터 증가에 견딤(`performance-design.md` §3).
- 회차 seq 유일성은 `(cohort_id, seq)` UNIQUE 제약으로 보장(INV-U2-2, `reliability-design.md` §2와 연동).

## 3. 동시성 확장

- U2는 고동시성 대상이 아니다(코호트 CRUD는 소유 멘토 단독 작업). 고동시성(선착순·정원)은 U3 관심사.
- 정원 축소 검증 시 U3 `confirmedCount` 읽기는 read-only 조회로, 경합 없음(`reliability-design.md` §3 안전 실패와 연동).

## 4. 확장 트리거

`scalability-requirements.md` §3:
- 코호트/회차 데이터가 페이지네이션·인덱스로 감당 불가한 규모(수만 건+)로 성장 시 → 전문 검색(Elasticsearch 등) 또는 목록 캐시 검토. 파일럿 규모(<수백 코호트)에서는 불필요.
- 다중 인스턴스 확장의 세션 제약은 U1 트리거를 따름(U2 고유 제약 없음).
