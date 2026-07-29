# Scalability Design — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 서포트 aws-platform·quality
> 상위 입력: `nfr-requirements/scalability-requirements.md`(동시성 특성·다중 인스턴스 안전성·트리거), `functional-design/business-logic-model.md`(락 §2), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 1. 확장 아키텍처(파일럿)

`scalability-requirements.md` §1~2:
- 파일럿은 단일 인스턴스·수직 확장만(U1 상속).
- 동시성은 인기 코호트의 마감 임박 순간에 집중(코호트당 수십 동시 신청 수준). 비관적 락이 **코호트 단위 직렬화**이므로 서로 다른 코호트는 독립 처리 → 병목은 단일 인기 코호트에 국한.

## 2. 다중 인스턴스 안전성(U3 핵심 강점)

`scalability-requirements.md` §2의 요구를 설계로 확정한다.

- **정합성이 애플리케이션 메모리가 아니라 DB에 위임됨**: 정원 제어는 (a) DB 행 락(`SELECT ... FOR UPDATE`)과 (b) UNIQUE(cohortId, menteeId) 제약에 의존한다. 따라서 **여러 인스턴스가 동시에 join을 처리해도 DB가 직렬화의 단일 진실 소스**가 되어 정원 초과 확정이 발생하지 않는다 → 수평 확장 시에도 U3 정합성 코드는 변경 불필요(U3의 확장 강점).
- 알림도 DB 기반이라 인스턴스 로컬 상태 없음. U3는 세션 외 추가 상태 결합점을 만들지 않는다(세션 확장 제약은 U1 트리거).
- **인덱스 설계**(성능·확장 공통): `enrollment(cohort_id, status)`, `enrollment(mentee_id)`, UNIQUE `enrollment(cohort_id, mentee_id)`, `notification(user_id, is_read, created_at)`.

## 3. 데이터 확장

- Enrollment/Notification 데이터 소량. 대기열(WAITING) 대량 축적도 페이지네이션 조회(`performance-design.md` §4)로 감당.
- 대기 자동 승격은 파일럿 범위 외(`cid:user-stories:c4`) → WAITING은 관리자 수동 처리 대상으로만 유지.

## 4. 확장 트리거

`scalability-requirements.md` §3:
- 단일 인기 코호트에 **수백+ 동시 신청**이 상시 발생하여 락 경합 지연이 목표를 지속 초과 → 낙관적 재시도·큐잉·원자 정원 카운터(`UPDATE ... WHERE confirmed<capacity`) 검토(확장 후속). 파일럿 규모에서는 불필요하며, 비관적 락이 정확성을 이미 보장.
- 다중 인스턴스 확장 시 U3 자체는 추가 제약이 없고(§2), 세션 제약만 U1 트리거를 따름.
