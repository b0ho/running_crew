# Code Generation Plan — U2 cohort (LearnKK 파일럿, Bolt 2)

> Construction · code-generation 단계 계획 · 유닛 U2-cohort
> 리드 aidlc-developer-agent (오케스트레이터 계획 → 개발자 에이전트 실행), 리뷰어 aidlc-architecture-reviewer-agent
> 상위 입력: functional-design(business-logic-model·business-rules·domain-entities·frontend-components), nfr-design(performance·security), infrastructure-design(deployment), units-generation/unit-of-work(U2), requirements(FR-2/6)
> 범위: 코호트 개설·수정·시작·조회, 회차(Session) N건 생성·조회·인증 전이 세터, 공지(외부 링크). 종료 액션 오케스트레이션은 U5(§경계).
> 기반: U1-foundation이 확립한 공통 인프라(에러 DTO·@RestControllerAdvice·Spring Security 세션·DTO 경계·ApiClient·ResponsiveTabBar)를 그대로 재사용·확장.

## 크로스유닛 계약 (code-generation 시 반드시 반영)

business-logic-model §8 계약을 다음과 같이 실체화한다:

| 방향 | 계약 | U2 구현 방식 |
|---|---|---|
| U2 → U3 (읽기) | `confirmedCount(cohortId): int` | U2 내 포트 인터페이스 `ConfirmedEnrollmentQuery` 정의 + 파일럿 기본 구현(0 반환, `@ConditionalOnMissingBean`). U3 빌드 시 실제 빈이 대체. 순환 없음 |
| U4 → U2 (쓰기) | `SessionService.markVerified(sessionId)` | U2가 지금 제공(회차 예정→인증 전이). U4가 이후 호출 |
| U5 → U2 (쓰기) | Cohort.status 종료됨 전이 세터 | U2가 상태 가드 UPDATE 경로 제공. U5가 판정 후 호출 |
| U3/U4/U5/U6 → U2 (읽기) | Cohort/Session 조회 | U2 get/list 제공 |

## 테스트 전략 (Comprehensive + team.md 정련)

핵심 도메인(코호트 CRUD·상태 전이·소유권 인가·정원 축소 검증·회차 락) 80% 라인 커버리지 목표. 백엔드 단위(JUnit5+Mockito), 통합(Testcontainers 실 PostgreSQL), N+1 회귀 방지(쿼리 카운트 단언), DTO 경계 ArchUnit. 프론트 Jest/RTL. DTO·getter/setter·단순 마크업은 커버리지 제외. 테스트 파일은 필수 산출물.

---

## PART A — 백엔드 (learnkk-api)

### Step 1: DB 스키마 & Flyway 마이그레이션 (domain-entities §2~5)
- [x] `V2__cohort_session_announcement.sql`:
  - `cohort`(id BIGSERIAL PK, mentor_id BIGINT NOT NULL FK→users(id) ON DELETE RESTRICT, title VARCHAR(200) NOT NULL, description TEXT, capacity INT NOT NULL CHECK(capacity>=1), start_date DATE NOT NULL, end_date DATE NOT NULL, session_count INT NOT NULL CHECK(session_count>=1), status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING', created_at TIMESTAMP NOT NULL DEFAULT now())
  - `session`(id BIGSERIAL PK, cohort_id BIGINT NOT NULL FK→cohort(id) ON DELETE CASCADE, seq INT NOT NULL CHECK(seq>=1), status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', UNIQUE(cohort_id, seq))
  - `announcement`(id BIGSERIAL PK, cohort_id BIGINT NOT NULL FK→cohort(id) ON DELETE CASCADE, body TEXT NOT NULL, external_link VARCHAR(2048), created_at TIMESTAMP NOT NULL DEFAULT now())
  - 인덱스(performance-design §2 / scalability-design §2): `cohort(status)`, `cohort(mentor_id)`, `cohort(created_at)`, `session(cohort_id, seq)`(UNIQUE로 충족), `announcement(cohort_id, created_at)`
- 트레이스: FR-2/6, INV-U2-2, domain-entities §2~5

### Step 2: 엔티티 & ENUM (domain-entities §2~4)
- [x] `com.learnkk.cohort.CohortStatus` enum: RECRUITING / ONGOING / CLOSED (표시명 모집중/진행중/종료됨)
- [x] `com.learnkk.cohort.SessionStatus` enum: SCHEDULED / VERIFIED
- [x] `Cohort` 엔티티(필드·제약, `@Enumerated(STRING)`, 팩토리 `open(...)`, prePersist createdAt, 상태 전이 도메인 메서드는 서비스의 가드 UPDATE와 정합). Entity 직접 노출 금지(INV-U2-4)
- [x] `Session` 엔티티(cohortId, seq, status, 팩토리 `scheduled(cohortId, seq)`, `markVerified()`)
- [x] `Announcement` 엔티티(cohortId, body, externalLink, createdAt, 팩토리)
- 트레이스: domain-entities §2~4, INV-U2-1/2/3

### Step 3: 리포지토리 (data access)
- [x] `CohortRepository extends JpaRepository<Cohort,Long>`:
  - `Page<Cohort> findByStatusIn(Collection<CohortStatus>, Pageable)` (목록 R-U2-19)
  - 제목 키워드 필터 검색(`findByStatusInAndTitleContainingIgnoreCase(...)`)
  - `@Query` fetch join으로 상세 조회(회차 포함, N+1 회피 — performance-design §2)
  - `@Modifying` 가드 UPDATE: `updateStatusGuarded(id, from, to)` → 영향 행 수 반환(상태 전이 동시성, project.md cid:nfr-design:state-transition-guarded-update)
  - 대시보드용 `findByMentorId(Long)`
- [x] `SessionRepository`: `findByCohortIdOrderBySeqAsc`, `findByCohortIdAndStatus`, 벌크 저장(batch insert)
- [x] `AnnouncementRepository`: `Page<Announcement> findByCohortIdOrderByCreatedAtDesc(Long, Pageable)`, 최근 N건(`findTop5ByCohortIdOrderByCreatedAtDesc`)
- 트레이스: performance-design §2/§3, R-U2-19

### Step 4: DTO (API 경계, INV-U2-4 / NFR-7)
- [x] 요청: `CohortCreateRequest`(title·description·capacity·startDate·endDate·sessionCount, Bean Validation: @NotBlank/@Size(200)/@Min(1)/sessionCount @Max(100) — performance-design §3 상한, endDate≥startDate 클래스 레벨 검증), `CohortUpdateRequest`, `AnnouncementCreateRequest`(body @NotBlank, externalLink `@SafeExternalUrl`)
- [x] 응답: `CohortSummaryDto`(목록/카드용 요약: id·title·status·capacity·기간·sessionCount·mentorId), `CohortDto`(단건, `warnings: List<String>` 포함 — 정원 축소 경고), `CohortDetailDto`(기본정보 + `List<SessionDto>` + 최근 공지 `List<AnnouncementDto>` 상한 5건), `SessionDto`(id·seq·status), `AnnouncementDto`(id·body·externalLink·createdAt)
- [x] 각 DTO는 `from(entity)` 정적 팩토리로 매핑
- 트레이스: R-U2-20, INV-U2-4, performance-design §3, reliability-design(warnings[])

### Step 5: 커스텀 검증 & 공통 확장
- [x] `com.learnkk.common.validation.SafeExternalUrl` 애너테이션 + `SafeExternalUrlValidator`(security-design §3: null/blank 허용, `java.net.URI` 파싱, 스킴 화이트리스트 {http,https}, host 존재 확인, 위반 시 400 VALIDATION_ERROR)
- [x] `com.learnkk.common.security.CurrentUserProvider`(SecurityContext principal(email) → UserRepository 조회 → 현재 User id 반환; 미인증 시 예외)
- [x] `ErrorCode`에 상수 추가: `COHORT_CLOSED`, `CAPACITY_BELOW_CONFIRMED`, `SESSION_VERIFIED_LOCK`, `INVALID_STATE_TRANSITION`
- 트레이스: security-design §3, business-rules §6

### Step 6: 예외 & 핸들러 확장 (business-rules §6)
- [x] 신규 예외: `CohortClosedException`, `CapacityBelowConfirmedException`, `SessionVerifiedLockException`, `InvalidStateTransitionException`(모두 `com.learnkk.common.exception` 또는 cohort 패키지)
- [x] `GlobalExceptionHandler`에 4개 핸들러 추가(각각 409 + 해당 code). `EntityNotFoundException`(404)은 U1 핸들러 재사용
- 트레이스: R-U2-21a~e

### Step 7: 크로스유닛 포트 (U3 계약)
- [x] `com.learnkk.cohort.port.ConfirmedEnrollmentQuery` 인터페이스(`int confirmedCount(Long cohortId)`)
- [x] `DefaultConfirmedEnrollmentQuery`(파일럿 기본 구현, 0 반환, `@ConditionalOnMissingBean` — U3가 실제 빈 제공 시 대체). 근거: business-logic-model §8, memory Interpretations
- 트레이스: business-logic-model §8, R-U2-09

### Step 8: 서비스 레이어 (business-logic-model §2~7)
- [x] `CohortService`:
  - `create(mentorId, CohortCreateRequest): CohortDto`(검증 → Cohort 저장(RECRUITING) → 트랜잭션 내 Session 1..N batch insert → CohortDto) — W-U2-1
  - `update(mentorId, cohortId, CohortUpdateRequest): CohortDto`(조회→404 / 소유검증→403 / CLOSED→409 / capacity 축소 시 `ConfirmedEnrollmentQuery.confirmedCount` 조회 → 확정 미만 축소 409, 이상이면 warnings 포함 / sessionCount 축소 시 인증 회차 절단 검사→409, 증가 시 회차 추가) — W-U2-2
  - `start(mentorId, cohortId): CohortDto`(소유검증 → 가드 UPDATE RECRUITING→ONGOING, 0행이면 409 INVALID_STATE_TRANSITION) — W-U2-5
  - `list/search(filter, Pageable): Page<CohortSummaryDto>`(기본 20건, createdAt desc, 상태 필터) — W-U2-3
  - `get(cohortId, 요청자): CohortDetailDto`(fetch join 회차 + 최근 공지 5건, 종료됨 조회 권한 R-U2-19/20) — W-U2-4
  - `closeByCompletion(cohortId)`: U5가 호출할 종료됨 전이 세터(가드 UPDATE ONGOING→CLOSED). U2는 이 경로만 제공, 종료 판정·오케스트레이션 없음(§경계, cid:units-generation:c2)
- [x] `SessionService`:
  - `listByCohort(cohortId): List<SessionDto>`
  - `markVerified(sessionId)`(U4 호출용 예정→인증 전이 세터; 이미 인증이면 멱등/무시) — business-logic-model §8
- [x] `AnnouncementService`:
  - `create(mentorId, cohortId, AnnouncementCreateRequest): AnnouncementDto`(소유검증 R-U2-15, body 필수, externalLink 검증) — W-U2-6
  - `list(cohortId, 요청자, Pageable): Page<AnnouncementDto>`(참여자·관리자 권한 R-U2-18, createdAt desc)
- [x] 트랜잭션 경계: create/update/start/close/markVerified/announcement.create는 `@Transactional`, 조회는 `@Transactional(readOnly=true)`
- 트레이스: business-logic-model §2~7, business-rules R-U2-01~20, §7 경계

### Step 9: 컨트롤러 (frontend-components §3, REST + springdoc)
- [x] `CohortController` (`/api/cohorts`):
  - `POST /` → 201 CohortDto (create)
  - `PUT /{id}` → 200 CohortDto (update)
  - `POST /{id}/start` → 200 CohortDto (start)
  - `GET /` (page·filter·status) → 200 Page<CohortSummaryDto> (list/search)
  - `GET /{id}` → 200 CohortDetailDto (get)
  - `GET /mine` → 대시보드용(내가 멘토인 코호트) — DashboardPage 연동
- [x] `AnnouncementController` (`/api/cohorts/{cohortId}/announcements`): `POST /` → 201 AnnouncementDto, `GET /` (page) → 200 Page<AnnouncementDto>
- [x] `mentorId`는 `CurrentUserProvider`로 해석(요청 바디로 받지 않음 — 신뢰 경계). springdoc `@Operation` 요약(한글)
- 트레이스: frontend-components §3, security-design §1

### Step 10: 백엔드 테스트 (Comprehensive)
- [x] `CohortServiceTest`(단위, Mockito): create 검증/회차 생성, update 소유권(403)·CLOSED(409)·capacity 축소(409/warnings)·session 축소 락(409), start 전이(409), get 권한
- [x] `SessionServiceTest`(단위): markVerified 전이·멱등
- [x] `AnnouncementServiceTest`(단위): create 소유권·body 필수, list 권한
- [x] `SafeExternalUrlValidatorTest`(단위): http/https 허용, javascript/data/file/상대URL 거부, null/blank 허용
- [x] `CohortIntegrationTest`(Testcontainers, `@Tag("integration")`): 마이그레이션 적용, 개설→회차 N건 확인, 목록 페이지네이션, 상세 fetch join, **N+1 회귀 방지(쿼리 카운트 단언 — 상세 조회 시 회차 로딩 쿼리 상한)**, 상태 전이 가드 UPDATE 동시성(선택: ExecutorService로 이중 start 시 1건만 성공)
- [x] `CohortArchitectureTest`(ArchUnit): controller 메서드 반환 타입이 `@Entity` 클래스가 아님(DTO 경계 강제 — security-design §4). 기존 `ArchitectureTest`에 규칙 추가 또는 신규 클래스
- 트레이스: NFR-6, performance-design §2(N+1), security-design §4(ArchUnit), team.md 테스트 도구

### Step 11: OpenAPI 계약 동기화
- [x] springdoc 자동 생성 확인, FE `types.ts`와 DTO 필드명 일치 검증(FE/BE 분리 저장소 계약 동기화 — team.md)

---

## PART B — 프론트엔드 (learnkk-web)

### Step 12: API 클라이언트 확장 & 타입 (api/)
- [x] `ApiClient.ts`에 `put`·`get`(쿼리스트링) 지원 추가(현재 get/post만; update용 PUT 필요)
- [x] `api/types.ts`에 추가: `CohortStatus`, `SessionStatus`, `CohortSummaryDto`, `CohortDto`(warnings), `CohortDetailDto`, `SessionDto`, `AnnouncementDto`, `CohortCreateRequest`, `CohortUpdateRequest`, `AnnouncementCreateRequest`, `Page<T>`(page 응답 래퍼)
- [x] `api/cohortApi.ts`: create/update/start/list/get/getMine/createAnnouncement/listAnnouncements
- 트레이스: frontend-components §3

### Step 13: 코호트 컴포넌트 (frontend-components §2)
- [x] `cohorts/CohortForm.tsx`(개설/수정 공용 폼: title·description·capacity·기간·sessionCount, 클라이언트 검증 보조, 접근성 라벨·aria-describedby·제출 중 비활성화)
- [x] `cohorts/CohortFormPage.tsx`(라우트 /cohorts/new, /cohorts/:id/edit; 409 인라인 에러; `CapacityWarningBanner` 노출)
- [x] `cohorts/CapacityWarningBanner.tsx`
- [x] `cohorts/CohortDetailPage.tsx`(라우트 /cohorts/:id; Tabs 공지|진도·출석|멤버|보고서 — U2는 공지·회차·기본정보 렌더, 나머지 탭 플레이스홀더; 멘토에게만 공지 작성·수정 진입점)
- [x] `cohorts/SessionList.tsx`(회차 seq·status 배지 — 색+텍스트 병기 접근성)
- [x] `cohorts/AnnouncementList.tsx`(최신순, externalLink `target=_blank rel="noopener noreferrer"`), `cohorts/AnnouncementForm.tsx`(body·externalLink)
- [x] `cohorts/CohortCard.tsx`(상태 배지·기간·정원 요약, 클릭 시 상세)
- [x] 상태 배지 공용 컴포넌트(모집중/진행중/종료됨, 예정/인증 — 색+텍스트)
- 트레이스: frontend-components §2, NFR-3, 접근성 §4

### Step 14: 대시보드 & 라우팅 통합
- [x] `pages/DashboardPage.tsx` 갱신: 내가 멘토인 코호트(`/api/cohorts/mine`) `CohortCard` 목록 + `EmptyState`("코호트를 탐색해보세요"). 확정 멘티 코호트는 U3 데이터이므로 U2에서는 멘토 코호트 우선(멘티 참여 목록은 U3에서 채움 — 플레이스홀더)
- [x] `App.tsx` 라우트 추가: `/cohorts/new`, `/cohorts/:id`, `/cohorts/:id/edit`(모두 RequireAuth)
- 트레이스: frontend-components §2.1, NFR-3

### Step 15: 프론트엔드 테스트 (Comprehensive, Jest/RTL)
- [x] `cohorts/CohortForm.test.tsx`(필수 검증·제출·에러 표시)
- [x] `cohorts/CohortDetailPage.test.tsx`(회차·공지 렌더, 멘토/비멘토 공지 작성 진입점 분기)
- [x] `cohorts/AnnouncementForm.test.tsx`(body 필수, 외부 링크 입력)
- [x] `api/cohortApi.test.ts`(요청 경로·메서드·에러 정규화)
- 트레이스: NFR-6

---

## Step 16: 코드 요약 산출
- [x] `code-summary.md` 작성: 생성/수정 파일, 핵심 구현 결정(포트·CurrentUserProvider·가드 UPDATE·@SafeExternalUrl), 테스트 커버리지 요약, 계획 대비 편차

## 산출물(코드) 위치
- 백엔드: `learnkk-api/src/main/java/com/learnkk/cohort/**`, `.../common/validation/`, `.../common/security/`, `learnkk-api/src/main/resources/db/migration/V2__*.sql`, 테스트 `learnkk-api/src/test/java/com/learnkk/cohort/**`
- 프론트: `learnkk-web/src/cohorts/**`, `learnkk-web/src/api/{cohortApi.ts,types.ts,ApiClient.ts}`, `learnkk-web/src/pages/DashboardPage.tsx`, `learnkk-web/src/App.tsx`
- 애플리케이션 코드는 워크스페이스 루트 하위(learnkk-api/·learnkk-web/)에만 생성. 레코드 디렉터리에는 계획·요약만.
