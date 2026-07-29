# Infrastructure Services — U2 cohort (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U2-cohort
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 인프라 서비스(PostgreSQL·시크릿·서비스 디스커버리) 상속. U2 고유는 스키마·인덱스.

## 1. 데이터베이스(공유 PostgreSQL — U2 스키마)

U1의 공유 PostgreSQL(`infrastructure-services.md` §1)에 U2 테이블을 추가한다(`components.md` 엔티티·FK):
- **테이블**: Cohort, Session, Announcement. FK 정책은 `components.md`(Cohort→Session/Announcement ON DELETE CASCADE, Cohort.mentorId→User RESTRICT).
- **인덱스**(`scalability-design.md` §2·`performance-design.md` §2): `cohort(status)`, `cohort(mentor_id)`, `cohort(created_at)`, `session(cohort_id, seq)` UNIQUE, `announcement(cohort_id, created_at)`.
- **상태 ENUM**: `@Enumerated(STRING)`(모집중/진행중/종료됨). 상태 전이는 조건부 UPDATE(`reliability-design.md` §2).
- Flyway 마이그레이션으로 스키마·인덱스 생성(U1 이력에 추가).

## 2. 캐시/큐/검색/파일

- **캐시·큐·검색**: U2 미도입(U1과 동일 방침). 코호트 목록은 인덱스+페이지네이션으로 충분(`performance-design.md` §5).
- **파일 스토리지**: U2 미사용(외부 링크는 URL 문자열 저장, 서버 fetch 없음 — `security-design.md` §3).

## 3. 시크릿·서비스 디스커버리

U1 상속(env·Actions Secrets·compose 내부 DNS). U2 신규 시크릿 없음.

## 4. 확장 트리거

코호트/공지 데이터가 수만+로 성장 시 전문 검색·캐시 검토(`scalability-design.md` §4). 파일럿 규모 불필요.
