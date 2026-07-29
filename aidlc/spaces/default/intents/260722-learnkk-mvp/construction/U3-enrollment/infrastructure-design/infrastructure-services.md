# Infrastructure Services — U3 enrollment (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U3-enrollment
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 인프라 서비스 상속. U3 고유는 스키마·인덱스·DB 락 설정.

## 1. 데이터베이스(공유 PostgreSQL — U3 스키마)

U1 공유 PostgreSQL에 U3 테이블 추가(`components.md`):
- **테이블**: Enrollment, Notification. FK: Enrollment.cohortId→Cohort CASCADE, Enrollment.menteeId→User RESTRICT, Notification.userId→User CASCADE(`components.md`).
- **인덱스**(`scalability-design.md` §2·`performance-design.md` §3): `enrollment(cohort_id, status)`, `enrollment(mentee_id)`, UNIQUE `enrollment(cohort_id, mentee_id)`(중복·경쟁 방지 겸 조회), `notification(user_id, is_read, created_at)`.
- **동시성 설정**: 비관적 락(`PESSIMISTIC_WRITE` = `SELECT ... FOR UPDATE`), 락 타임아웃 3000ms, `statement_timeout`, 격리 READ_COMMITTED(`reliability-design.md` §1, `performance-design.md` §2).
- Flyway 마이그레이션(U1 이력에 추가, DAG U2 다음 (U3∥U4)).

## 2. 캐시/큐/검색/파일

- **캐시·큐·검색**: 미도입(U1 방침). 대기열은 페이지네이션 조회. 알림은 동일 트랜잭션 DB 레코드(`reliability-design.md` §2, 비동기 브로커는 확장).
- **파일 스토리지**: U3 미사용.

## 3. 시크릿·디스커버리

U1 상속. U3 신규 시크릿 없음.

## 4. 다중 인스턴스 안전성(핵심)

정원 제어가 DB 행 락+UNIQUE에 위임되어 DB가 단일 진실 소스(`scalability-design.md` §2). 인스턴스 로컬 상태 없음 → 수평 확장 시 U3 인프라 추가 제약 없음(세션은 U1 트리거).
