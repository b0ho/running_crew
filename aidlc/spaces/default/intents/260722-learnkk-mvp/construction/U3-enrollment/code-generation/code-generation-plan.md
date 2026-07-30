# Code Generation Plan — U3 enrollment (LearnKK 파일럿, Bolt 3, 최대 리스크)

> Construction · code-generation 단계 계획 · 유닛 U3-enrollment (복잡도 L — 최대 정합성 리스크)
> 리드 aidlc-developer-agent, 리뷰어 aidlc-architecture-reviewer-agent
> 상위 입력: functional-design(business-logic-model·business-rules·domain-entities·frontend-components), nfr-design(performance·security), infrastructure-design(deployment), units-generation/unit-of-work(U3), requirements(FR-3/4/10)
> 기반: U1(공통 인프라·인증·에러·ApiClient)·U2(Cohort/CohortRepository·ConfirmedEnrollmentQuery 포트) 재사용·확장.
> 핵심: 선착순 참여 동시성 제어(비관적 락 + UNIQUE 제약) — 정원 초과 확정 절대 방지(INV-U3-1).

## 크로스유닛 계약 (code-generation 시 반드시 반영)

| 방향 | 계약 | U3 구현 방식 |
|---|---|---|
| U2 → U3 (읽기) | `ConfirmedEnrollmentQuery.confirmedCount(cohortId): int` | **U3가 실제 빈 제공**(EnrollmentRepository.countByCohortIdAndStatus(CONFIRMED) 위임 어댑터). U2의 @ConditionalOnMissingBean 기본 빈(0) 자동 대체 |
| U5/U6 → U3 (읽기) | `confirmedEnrollments(cohortId): List<EnrollmentDto>` | U3 제공(확정 멘티 목록) |
| U5/U8 → U3 (쓰기) | `NotificationService.notify(userId, type, message)` | U3 제공(타 유닛 호출용 알림 생성 계약) |
| U3 → U2 (읽기+락) | Cohort 조회 + **FOR UPDATE 락** | U2 CohortRepository에 `findByIdForUpdate`(@Lock PESSIMISTIC_WRITE, lock.timeout=3000) 추가 후 주입 |

## 테스트 전략 (Comprehensive + team.md 정련 — 동시성 필수 게이트)

핵심 도메인(선착순 확정/대기·동시성·승인 경합) 80% 커버리지 목표. **동시성 정확성 검증이 U3 신뢰성의 게이팅 조건**(reliability-design §1): Testcontainers 실 PostgreSQL + ExecutorService + CountDownLatch로 capacity=N에 N+k 동시 join → CONFIRMED==N·WAITING==k·중복 0·정원 초과 0 단언(R-U3-10, 최소 2 시나리오: 정원 여유 다수 동시 / 정원 경계 초과 다수 동시). 단위(Mockito), FE Jest/RTL.

---

## PART A — 백엔드 (learnkk-api)

### Step 1: DB 스키마 & Flyway 마이그레이션 (domain-entities §2~4)
- [x] `V3__enrollment_notification.sql`:
  - `enrollment`(id BIGSERIAL PK, cohort_id BIGINT NOT NULL FK→cohort(id) ON DELETE CASCADE, mentee_id BIGINT NOT NULL FK→users(id) ON DELETE RESTRICT, status VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT now(), decided_at TIMESTAMP NULL, version BIGINT NOT NULL DEFAULT 0, **UNIQUE(cohort_id, mentee_id)**)
  - `notification`(id BIGSERIAL PK, user_id BIGINT NOT NULL FK→users(id) ON DELETE CASCADE, type VARCHAR(40) NOT NULL, message VARCHAR(500) NOT NULL, is_read BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMP NOT NULL DEFAULT now())
  - 인덱스(performance-design §3): `enrollment(cohort_id, status)`(confirmedCount 집계·대기목록), `enrollment(mentee_id)`(내 신청), `notification(user_id, is_read, created_at)`(알림 목록). UNIQUE(cohort_id, mentee_id)로 중복 방어.
- 트레이스: FR-3/4/10, INV-U3-2, domain-entities §2~4

### Step 2: 엔티티 & ENUM (domain-entities §2~3)
- [x] `com.learnkk.enrollment.EnrollmentStatus` enum: CONFIRMED / WAITING / REJECTED (표시명 확정/대기중/거절)
- [x] `com.learnkk.enrollment.NotificationType` enum(또는 VARCHAR 상수): ENROLLMENT_CONFIRMED / ENROLLMENT_REJECTED / COMPLETION_RESULT(확장 대비)
- [x] `Enrollment` 엔티티(cohortId·menteeId·status·createdAt·decidedAt·`@Version version`, 팩토리 `confirmed(...)`/`waiting(...)`, 상태 전이 도메인 메서드 `confirm()`/`reject()` — decidedAt 세팅, @PrePersist). Entity 미노출(INV-U3-4)
- [x] `Notification` 엔티티(userId·type·message·read·createdAt, 팩토리, markRead())
- 트레이스: domain-entities §2~3, INV-U3-2/3

### Step 3: 리포지토리 (data access)
- [x] `EnrollmentRepository extends JpaRepository<Enrollment,Long>`:
  - `int countByCohortIdAndStatus(Long cohortId, EnrollmentStatus status)` (confirmedCount 집계, 락 보유 하 호출)
  - `Optional<Enrollment> findByCohortIdAndMenteeId(...)` (사전 중복 조회)
  - `List<Enrollment> findByCohortIdAndStatus(cohortId, status)` (confirmedEnrollments, 대기목록)
  - `Page<Enrollment> findByMenteeIdOrderByCreatedAtDesc(menteeId, Pageable)` (내 신청)
  - `Page<Enrollment> findByStatus(EnrollmentStatus, Pageable)` / `findByCohortIdAndStatus(..., Pageable)` (대기 목록, 관리자)
  - `@Modifying @Query("UPDATE Enrollment e SET e.status=:to, e.decidedAt=CURRENT_TIMESTAMP WHERE e.id=:id AND e.status=:from")` `int approveGuarded/decideGuarded(...)` (승인/거절 조건부 UPDATE, 영향 행 0이면 이미 처리 → 409)
- [x] `NotificationRepository`: `Page<Notification> findByUserIdOrderByCreatedAtDesc(userId, Pageable)`, `countByUserIdAndReadFalse(userId)`(안읽은 수), `findByIdAndUserId(id, userId)`(소유 확인)
- [x] **U2 CohortRepository 확장(가산)**: `@Lock(PESSIMISTIC_WRITE) @QueryHints(@QueryHint(name="jakarta.persistence.lock.timeout", value="3000")) Optional<Cohort> findByIdForUpdate(Long id)` — join 락 획득용
- 트레이스: performance-design §2/§3, R-U3-07/09

### Step 4: DTO (API 경계, INV-U3-4)
- [x] 응답: `JoinResultDto`(status·대기순번? — WAITING 시 대기 위치 선택), `EnrollmentDto`(id·cohortId·코호트제목요약·menteeId·status·createdAt·decidedAt), `NotificationDto`(id·type·message·read·createdAt), `WaitingEnrollmentDto`(관리자 대기목록: enrollmentId·cohort·mentee 요약·createdAt), `Page<T>` 매핑
- [x] 각 DTO `from(entity)` 정적 팩토리. 코호트 제목 등 U2 정보는 조회 조합
- 트레이스: R-U3-16, INV-U3-4

### Step 5: 예외 & ErrorCode 확장 (business-rules §5)
- [x] ErrorCode 추가: `ALREADY_ENROLLED`, `SELF_ENROLLMENT`, `COHORT_NOT_OPEN`, `ENROLLMENT_BUSY`(락 타임아웃). `INVALID_STATE_TRANSITION`은 U2 상수 재사용
- [x] 신규 예외: `AlreadyEnrolledException`, `SelfEnrollmentException`, `CohortNotOpenException`, `EnrollmentBusyException`(모두 409). `InvalidStateTransitionException`은 U2 재사용
- [x] `GlobalExceptionHandler` 확장: 4개 신규 예외 409 핸들러 + `DataIntegrityViolationException`은 U1이 이미 DUPLICATE_EMAIL로 매핑 중 → **enrollment UNIQUE 위반 구분 필요**. 접근: EnrollmentService에서 save 시 DataIntegrityViolationException을 잡아 `AlreadyEnrolledException`으로 변환(서비스 레벨 변환)하여 U1 핸들러 충돌 회피(R-U3-21b) + Pessimistic lock 예외(LockTimeoutException/PessimisticLockingFailureException) → 409 ENROLLMENT_BUSY 핸들러
- 트레이스: R-U3-21a~e, performance-design §2

### Step 6: 크로스유닛 포트 구현 (U2 계약 충족)
- [x] `com.learnkk.enrollment.EnrollmentQueryAdapter implements com.learnkk.cohort.port.ConfirmedEnrollmentQuery`(@Component, `confirmedCount` → `countByCohortIdAndStatus(cohortId, CONFIRMED)`). U2 기본 빈(@ConditionalOnMissingBean) 자동 대체
- 트레이스: business-logic-model §3/§7, R-U2-09

### Step 7: 서비스 레이어 (business-logic-model §2~6) — 핵심 동시성
- [x] `EnrollmentService`:
  - `join(menteeId, cohortId): JoinResultDto` — **단일 @Transactional**: 사전 검증(인증·코호트 404·CLOSED→409 COHORT_NOT_OPEN·멘토 자기신청→409 SELF_ENROLLMENT) → **`cohortRepository.findByIdForUpdate(cohortId)` 비관적 락** → 락 보유 하 `countByCohortIdAndStatus(CONFIRMED)` 집계 → confirmed<capacity면 CONFIRMED 저장+알림, 아니면 WAITING 저장 → save 시 DataIntegrityViolation(UNIQUE) → AlreadyEnrolledException(409). 락 타임아웃 → EnrollmentBusyException(409). (R-U3-01~09)
  - `myApplications(menteeId, Pageable): Page<EnrollmentDto>` — 세션 id 스코프(파라미터 무시, R-U3-16/17)
  - `confirmedCount(cohortId): int`, `confirmedEnrollments(cohortId): List<EnrollmentDto>` — 크로스유닛 read 계약
- [x] `AdminApprovalService`:
  - `listWaiting(cohortId?, Pageable): Page<WaitingEnrollmentDto>` (관리자)
  - `approve(adminId, enrollmentId)` — **조건부 UPDATE**(WAITING→CONFIRMED, 영향 행 0이면 409 INVALID_STATE_TRANSITION) → decidedAt → 알림(ENROLLMENT_CONFIRMED) 1건. 정원 초과 허용(R-U3-13) + 감사 로그
  - `reject(adminId, enrollmentId)` — 조건부 UPDATE(WAITING→REJECTED) → 알림(ENROLLMENT_REJECTED)
- [x] `NotificationService`:
  - `notify(userId, type, message)` — 타 유닛 호출 계약(U5/U8)
  - `listFor(userId, Pageable): Page<NotificationDto>`, `unreadCount(userId): long`, `markRead(userId, notificationId)` — 소유 확인(R-U3-19)
- [x] 트랜잭션 경계: join/approve/reject/notify는 @Transactional, 조회는 readOnly. 승인/거절 시 알림 생성은 동일 트랜잭션(reliability-design §2)
- 트레이스: business-logic-model §2~6, business-rules R-U3-01~19, INV-U3-1

### Step 8: 컨트롤러 (frontend-components §3, REST + springdoc)
- [x] `EnrollmentController`:
  - `POST /api/cohorts/{cohortId}/enrollments` → 201 JoinResultDto (join)
  - `GET /api/me/enrollments` (page) → 200 Page<EnrollmentDto> (myApplications, 세션 id)
- [x] `AdminEnrollmentController` (`@PreAuthorize("hasRole('ADMIN')")`):
  - `GET /api/admin/enrollments/waiting` (page, cohortId?) → 200 Page<WaitingEnrollmentDto>
  - `POST /api/admin/enrollments/{id}/approve` → 200, `POST /.../reject` → 200
- [x] `NotificationController`:
  - `GET /api/me/notifications` (page) → 200 Page<NotificationDto>, `GET /api/me/notifications/unread-count` → 200
  - `POST /api/me/notifications/{id}/read` → 204 (markRead)
- [x] 사용자 id는 CurrentUserProvider(U2 신설)로 해석. springdoc @Operation(한글)
- 트레이스: frontend-components §3, security-design §1

### Step 9: 백엔드 테스트 (Comprehensive — 동시성 필수)
- [x] `EnrollmentServiceTest`(단위, Mockito): join 사전 검증(404·CLOSED·SELF_ENROLLMENT·ALREADY_ENROLLED), 확정/대기 결정 분기, myApplications 세션 스코프
- [x] `AdminApprovalServiceTest`(단위): approve WAITING→CONFIRMED·이미 처리 409·정원 초과 허용·알림 1건, reject
- [x] `NotificationServiceTest`(단위): notify, listFor 스코프, markRead 소유 확인(타인 알림 거부)
- [x] `EnrollmentConcurrencyIntegrationTest`(**Testcontainers, @Tag("integration") — 게이팅**): ExecutorService+CountDownLatch. 시나리오1: capacity=5에 20 동시 join → CONFIRMED==5·WAITING==15·중복 0. 시나리오2: capacity=1에 10 동시 join → 정확히 1 CONFIRMED. UNIQUE 이중제출 → 1건만 성공. (R-U3-03/10, INV-U3-1)
- [x] `EnrollmentIntegrationTest`(Testcontainers): 마이그레이션, confirmedCount/confirmedEnrollments 정확성, 승인/거절 조건부 UPDATE 동시 경합(2 관리자 동시 approve → 1건만·알림 1건), 락 타임아웃 경로(선택)
- [x] ArchUnit DTO 경계는 기존 ArchitectureTest가 전체 컨트롤러 자동 커버
- 트레이스: NFR-6, reliability-design §1(게이팅), R-U3-10

### Step 10: OpenAPI 계약 동기화
- [x] springdoc 자동 생성 확인, FE types.ts와 DTO 필드명 일치

---

## PART B — 프론트엔드 (learnkk-web)

### Step 11: API 클라이언트 & 타입
- [x] `api/types.ts` 추가: EnrollmentStatus, JoinResultDto, EnrollmentDto, NotificationDto, WaitingEnrollmentDto
- [x] `api/enrollmentApi.ts`(join, myApplications), `api/notificationApi.ts`(listFor, unreadCount, markRead), `api/adminApi.ts`(listWaiting, approve, reject) — 또는 통합 모듈
- 트레이스: frontend-components §3

### Step 12: 컴포넌트 (frontend-components §2)
- [x] `enrollment/ExplorePage.tsx`(라우트 /explore — 모집중/진행중 코호트 목록 U2 조회 + JoinButton), `enrollment/JoinButton.tsx`(제출 중 비활성·중복 방지, 결과 Toast 확정/대기/409 분기)
- [x] `common/Toast.tsx`(aria-live), `enrollment/MyApplicationsPage.tsx`(라우트 /my/applications — ApplicationRow + StatusBadge 재사용)
- [x] `enrollment/NotificationBell.tsx`(헤더 위젯 — 안읽은 수 배지, 드롭다운 목록, 클릭 시 markRead; 진입 시 폴링/조회)
- [x] `admin/AdminPage.tsx`(라우트 /admin, 관리자 전용) + 대기승인 탭 `admin/WaitingList.tsx`(Approve/Reject, 정원 초과 승인 확인 다이얼로그, 처리 중 비활성)
- [x] StatusBadge는 U2 것 재사용 또는 확장(대기중/확정/거절 — 색+텍스트)
- 트레이스: frontend-components §2, 접근성 §4

### Step 13: 라우팅·셸 통합
- [x] `App.tsx` 라우트: /explore, /my/applications, /admin (RequireAuth). NotificationBell을 ResponsiveTabBar 또는 공통 헤더에 통합
- [x] DashboardPage: 확정 멘티 코호트 목록 채우기(U2 대시보드 플레이스홀더 → myApplications 확정분 반영)
- 트레이스: frontend-components §1, NFR-3

### Step 14: 프론트엔드 테스트 (Jest/RTL)
- [x] `enrollment/JoinButton.test.tsx`(확정/대기/409 토스트·중복 클릭 방지)
- [x] `enrollment/MyApplicationsPage.test.tsx`(상태 배지 렌더)
- [x] `enrollment/NotificationBell.test.tsx`(안읽은 배지·markRead)
- [x] `admin/WaitingList.test.tsx`(승인/거절 액션·초과 승인 확인)
- [x] `api/enrollmentApi.test.ts`
- 트레이스: NFR-6

---

## Step 15: 코드 요약 산출
- [x] `code-summary.md`: 생성/수정 파일, 핵심 결정(비관적 락·UNIQUE·조건부 UPDATE·포트 실빈·락 타임아웃), 동시성 테스트 결과, 계획 대비 편차

## 산출물(코드) 위치
- 백엔드: `learnkk-api/src/main/java/com/learnkk/enrollment/**`, `common/exception/`(확장), U2 `cohort/CohortRepository.java`(findByIdForUpdate 가산), `resources/db/migration/V3__*.sql`, 테스트 `.../enrollment/**`
- 프론트: `learnkk-web/src/enrollment/**`, `src/admin/**`, `src/common/Toast.tsx`, `src/api/*.ts`, `App.tsx`·DashboardPage 갱신
- 애플리케이션 코드는 워크스페이스 루트 하위에만. 레코드 디렉터리에는 계획·요약만.
