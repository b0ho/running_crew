# Domain Entities — U2 cohort (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U2 책임), `unit-of-work-story-map.md`(US-3/4/5), `requirements-analysis/requirements.md`(FR-2/6), `application-design/components.md`(Cohort·Session·Announcement·FK), `component-methods.md`(CohortService·AnnouncementService), `services.md`(CohortService·AnnouncementService)
> 범위: U2가 소유하는 엔티티만 상세화(Cohort, Session, Announcement). User(U1)·Enrollment(U3)·판정 관련(U5)은 참조만.

## 1. U2 소유 엔티티

| 엔티티 | 소유 | U2에서의 처리 |
|---|---|---|
| Cohort | **U2** | 개설·수정·조회·상태 필드 소유(CRUD). 종료 액션 오케스트레이션은 U5 |
| Session | **U2** | 회차 N건 생성·조회. 회차 출석 인증 전이(예정→인증)는 U4 |
| Announcement | **U2** | 공지 작성·조회 |
| User(mentor) | U1 | mentorId FK 참조만 |
| Enrollment | U3 | 정원 대비 확정 수는 U3가 관리(U2는 capacity 필드만 소유) |

## 2. Cohort 엔티티

`components.md` 정의를 FR-2에 맞춰 상세화.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| mentorId | BIGINT FK→User.id | NOT NULL, ON DELETE RESTRICT | 개설자(컨텍스트 역할 멘토) |
| title | VARCHAR(200) | NOT NULL | 코호트 제목 |
| description | TEXT | NULL 허용 | 설명 |
| capacity | INT | NOT NULL, >= 1 | 정원 |
| startDate | DATE | NOT NULL | 시작일 |
| endDate | DATE | NOT NULL, >= startDate | 종료일 |
| sessionCount | INT | NOT NULL, >= 1 | 회차 수(개설 시 확정) |
| status | ENUM | NOT NULL, default 모집중 | 모집중 / 진행중 / 종료됨 |
| createdAt | TIMESTAMP | NOT NULL | 생성 시각 |

### 2.1 Cohort 상태 생명주기

```
[모집중(RECRUITING)] --start(멘토/시작일 도래)--> [진행중(ONGOING)] --end(멘토 종료 액션: U5)--> [종료됨(CLOSED)]
```
<!-- Text fallback: 코호트는 모집중에서 시작(멘토 또는 시작일)으로 진행중이 되고, 멘토의 종료 액션(U5 소유)으로 종료됨으로 전이한다. 역전이는 없다. -->

- **모집중→진행중**: 코호트 시작(멘토의 명시 시작 또는 startDate 도래). U2가 소유하는 전이.
- **진행중→종료됨**: 멘토의 "코호트 종료" 액션. 이 전이의 오케스트레이션(수료·정산 판정 동반)은 **U5가 소유**하며, U5가 U2 데이터를 읽어 status를 종료됨으로 갱신한다. U2는 status 필드와 세터(리포지토리 수준)만 제공하고 U5를 호출하지 않는다(`cid:units-generation:c2` 순환 회피).
- 역전이(종료됨→진행중 등) 금지. 하드 삭제 대신 종료됨 상태 전이 우선(`cid:application-design:c2`).

## 3. Session 엔티티

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| cohortId | BIGINT FK→Cohort.id | NOT NULL, ON DELETE CASCADE | 소속 코호트 |
| seq | INT | NOT NULL, >= 1 | 회차 순번(1..sessionCount) |
| status | ENUM | NOT NULL, default 예정 | 예정(SCHEDULED) / 인증(VERIFIED) |

- 개설 시 sessionCount만큼 seq 1..N으로 자동 생성(business-logic-model W-U2-1).
- UNIQUE(cohortId, seq) — 회차 순번 중복 방지.
- status 예정→인증 전이는 **U4(attendance)**가 증빙 업로드로 수행하되, U2가 제공하는 `SessionService.markVerified(sessionId)` 서비스 메서드를 호출한다(리포지토리 직접 접근 금지 — 캡슐화). 계약은 business-logic-model §8 참조. U2는 회차 조회·생성·이 전이 세터를 제공.

## 4. Announcement 엔티티

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| cohortId | BIGINT FK→Cohort.id | NOT NULL, ON DELETE CASCADE | 소속 코호트 |
| body | TEXT | NOT NULL | 공지 본문 |
| externalLink | VARCHAR(2048) | NULL 허용 | 외부 미팅 링크(URL) |
| createdAt | TIMESTAMP | NOT NULL | 작성 시각 |

- externalLink는 플랫폼 내장 화상이 아니라 멘토가 붙이는 외부 URL(`cid:intent-capture` 범위 결정, requirements FR-6).

## 5. 관계·FK 정책(요약, components.md 준수)

- Cohort.mentorId → User.id (RESTRICT), Session/Announcement.cohortId → Cohort.id (CASCADE).
- 하위(회차·공지) CASCADE는 코호트 하드 삭제 시에만 의미. 파일럿은 하드 삭제를 지양하고 종료됨 전이를 우선.

## 6. 다른 유닛과의 경계

- U3(enrollment)는 Cohort.capacity·status를 읽어 참여/대기 판정. U2는 확정 참여 수를 소유하지 않는다.
- U5(completion)는 Cohort/Session을 읽어 종료 전이 및 판정. U2→U5 호출 없음(단방향).
- U4(attendance)는 Session.status를 인증으로 전이. U2는 회차 구조만 제공.
