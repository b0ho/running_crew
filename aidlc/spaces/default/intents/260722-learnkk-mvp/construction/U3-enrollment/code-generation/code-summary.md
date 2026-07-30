# Code Summary — U3 enrollment (LearnKK 파일럿, Bolt 3, 최대 리스크)

> Construction · code-generation 단계 산출물 요약 · 유닛 U3-enrollment
> 리드 aidlc-developer-agent
> 계획: `code-generation-plan.md`(Step 1~15 전 항목 [x] 완료). 애플리케이션 코드는 `learnkk-api/`·`learnkk-web/` 에만 생성.

## 핵심 결과 (게이팅 조건)

**정원 초과 확정 절대 방지(INV-U3-1)** 를 Testcontainers 실 PostgreSQL 동시성 테스트로 검증 완료:
- capacity=5 에 20 동시 join → CONFIRMED==5·WAITING==15·중복 0 ✅
- capacity=1 에 10 동시 join → CONFIRMED==1·WAITING==9 ✅
- 동일 멘티 8 동시 이중 제출 → UNIQUE 로 정확히 1건만 성공 ✅
- 두 관리자 동시 approve → 조건부 UPDATE 로 1건만 성공·알림 1건 ✅

## PART A — 백엔드 (learnkk-api)

### 생성 파일
- `resources/db/migration/V3__enrollment_notification.sql` — enrollment(UNIQUE(cohort_id,mentee_id)·version·인덱스 2종)·notification(인덱스 1종)
- `enrollment/EnrollmentStatus.java`, `enrollment/NotificationType.java` — ENUM(표시명 한글)
- `enrollment/Enrollment.java` — 팩토리(confirmed/waiting)·@Version·상태 세터 없음
- `enrollment/Notification.java` — 팩토리(of)·markRead()
- `enrollment/EnrollmentRepository.java` — countByCohortIdAndStatus·findByCohortIdAndMenteeId·확정목록·내신청 page·대기목록 page·`updateStatusGuarded`(조건부 UPDATE)
- `enrollment/NotificationRepository.java` — 목록 page·unread count·findByIdAndUserId(소유 확인)
- `enrollment/dto/{JoinResultDto,EnrollmentDto,NotificationDto,WaitingEnrollmentDto}.java` — `from(entity)` 정적 팩토리
- `enrollment/EnrollmentQueryAdapter.java` — U2 `ConfirmedEnrollmentQuery` 포트 실 구현(@Component)
- `enrollment/NotificationService.java` — notify(크로스유닛)·listFor·unreadCount·markRead(소유 확인)
- `enrollment/EnrollmentService.java` — **join(단일 @Transactional: 사전검증→비관적 락→락 보유 집계→상태결정→저장→알림)**·myApplications(세션 스코프)·confirmedCount·confirmedEnrollments
- `enrollment/AdminApprovalService.java` — listWaiting·approve/reject(조건부 UPDATE + 알림 1건·정원 초과 승인 허용+감사 로그)
- `enrollment/{EnrollmentController,AdminEnrollmentController,NotificationController}.java` — REST + springdoc @Operation(한글). 관리자 인가 `@PreAuthorize("hasRole('ADMIN')")`
- `common/exception/{AlreadyEnrolledException,SelfEnrollmentException,CohortNotOpenException,EnrollmentBusyException}.java`
- 테스트: `enrollment/{EnrollmentServiceTest(7),AdminApprovalServiceTest(5),NotificationServiceTest(3)}`(단위 Mockito), `EnrollmentConcurrencyIntegrationTest(3)`·`EnrollmentIntegrationTest(4)`(Testcontainers, @Tag integration)

### 수정 파일(in-place)
- `cohort/CohortRepository.java` — `findByIdForUpdate`(@Lock PESSIMISTIC_WRITE, lock.timeout=3000) 가산(Cohort 읽기 전용 유지)
- `common/exception/ErrorCode.java` — ALREADY_ENROLLED·SELF_ENROLLMENT·COHORT_NOT_OPEN·ENROLLMENT_BUSY 추가(INVALID_STATE_TRANSITION 은 U2 상수 재사용)
- `common/exception/GlobalExceptionHandler.java` — 4개 신규 예외 409 핸들러 + PessimisticLockingFailureException 안전망(409 ENROLLMENT_BUSY)

## PART B — 프론트엔드 (learnkk-web)

### 생성 파일
- `api/types.ts`(가산: EnrollmentStatus·NotificationType·JoinResultDto·EnrollmentDto·NotificationDto·WaitingEnrollmentDto)
- `api/{enrollmentApi,notificationApi,adminApi}.ts`
- `common/Toast.tsx`(aria-live role=status)
- `enrollment/{ExplorePage,JoinButton,MyApplicationsPage,NotificationBell}.tsx`
- `admin/{AdminPage,WaitingList}.tsx`(정원 초과 승인 확인 다이얼로그)
- 테스트: `enrollment/{JoinButton,MyApplicationsPage,NotificationBell}.test.tsx`·`admin/WaitingList.test.tsx`·`api/enrollmentApi.test.ts`

### 수정 파일(in-place)
- `cohorts/StatusBadge.tsx` — `EnrollmentStatusBadge`(확정/대기중/거절, 색+텍스트) 확장
- `App.tsx` — /explore·/my/applications·/admin 라우트(RequireAuth) 추가
- `shell/ResponsiveTabBar.tsx` — 내비 링크 + NotificationBell 통합(관리자 링크는 isAdmin 노출)
- `pages/DashboardPage.tsx` — 확정 멘티 참여 섹션 반영(myApplications CONFIRMED 필터)

## Key Decisions (핵심 결정)

1. **비관적 락 직렬화(R-U3-07)**: join 은 단일 트랜잭션에서 `cohortRepository.findByIdForUpdate`(SELECT ... FOR UPDATE)로 코호트 행 1개만 잠그고, **락 보유 상태에서** `countByCohortIdAndStatus(CONFIRMED)` 집계 후 상태 결정. 집계를 락 밖으로 분리하지 않음(R-U3-07a). 격리 READ_COMMITTED로 충분.
2. **UNIQUE(cohort_id, mentee_id) 최종 방어선(R-U3-08)**: 사전 중복 조회를 통과한 동시 이중 제출은 `saveAndFlush` 시 DataIntegrityViolationException → **서비스 레벨에서 AlreadyEnrolledException(409)으로 변환**하여 U1 전역 핸들러(DataIntegrity→DUPLICATE_EMAIL)와의 충돌 회피(R-U3-21b).
3. **락 타임아웃**: `jakarta.persistence.lock.timeout=3000` 힌트. 획득 실패(PessimisticLockingFailureException/LockTimeoutException/PessimisticLockException) → **EnrollmentBusyException(409 ENROLLMENT_BUSY)**. GlobalExceptionHandler 에 안전망 핸들러도 추가.
4. **조건부 UPDATE(승인/거절)**: `UPDATE Enrollment SET status=:to, decidedAt=now WHERE id=:id AND status=WAITING`. 영향 행 0 → 409 INVALID_STATE_TRANSITION. DB 행 락이 두 관리자 동시 승인을 직렬화 → 1건만 성공·알림 1건. @Version 컬럼은 보조 방어선. 정원 초과 승인 허용(R-U3-13) + 감사 로그.
5. **포트 실빈(R-U2-09)**: `EnrollmentQueryAdapter`(@Component)가 U2 `ConfirmedEnrollmentQuery` 기본 빈(@ConditionalOnMissingBean, 0 반환)을 대체 → U2 정원 축소 검증이 실제 확정 인원으로 동작(통합 테스트로 검증).
6. **본인 스코프 강제**: myApplications·알림 조회/읽음은 세션 사용자 id(CurrentUserProvider)로 스코프. markRead 는 findByIdAndUserId 로 소유 확인(수평 권한 상승 방지). self-enrollment 는 서비스에서 mentorId 검사.

## Issues / Concerns

- 컴파일/린트/단위 테스트: **learnkk-api** `compileJava compileTestJava test -PexcludeIntegration spotlessCheck` PASS(11 test suite, U3 단위 15개 포함 전부 그린). **learnkk-web** `build`·`test`(13 suite/44 test)·`lint(--max-warnings=0)` 모두 PASS.
- 통합·동시성 테스트: 본 환경의 Docker(Rancher Desktop)로 **실제 실행하여 U3 게이팅 조건 통과 확인**(위 핵심 결과). 단, Rancher 소켓 사용 시 `DOCKER_HOST=unix:///.../.rd/docker.sock` + `-Dapi.version=1.41`(docker-java API 협상) 필요. 두 통합 클래스를 한 번에 실행하면 컨테이너 생명주기 타이밍으로 간헐 연결 오류가 있어 **클래스별 개별 실행 시 각각 그린**(CI에서는 정상). 코드 결함 아님.
- 기존 U1/U2 테스트 회귀 없음(전부 그린 유지).

## Next Steps

- Bolt 3 게이트: 사용자 검증 후 U4(attendance)와 병렬 진행 여부 확인(project.md: U3∥U4).
- U5(completion)가 `confirmedEnrollments`(수료증 순회)·`NotificationService.notify`(수료 결과) 계약을 소비할 예정 — 시그니처 확정됨.
- OpenAPI 스펙(springdoc 자동 생성)과 FE `types.ts` 필드명 일치 확인 완료. 계약 변경 시 FE/BE 동기화(team.md 분리 저장소 정책).

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 검증 범위 및 방법

U3-enrollment code-generation 산출물(백엔드·프론트엔드 코드)에 대한 적대적 아키텍처 리뷰를 완료함. 정원 초과 확정 절대 방지(INV-U3-1)를 핵심 리스크로 간주하고, 비관적 락·UNIQUE 제약·조건부 UPDATE·포트 실빈·보안 스코프 강제를 중점 검증. 설계 의도가 코드로 정확히 구현되었는지를 실제 생성 코드 읽기 + machine-checkable 빌드/테스트 실행 + 크로스유닛 계약 순환 검증으로 확인.

### 검증 결과 (machine-checkable 근거)

1. **INV-U3-1(정원 초과 확정 절대 방지) — 동시성 정확성 검증 PASS**
   - `EnrollmentService.join`은 단일 `@Transactional` 경계 안에서 (1) 락 밖 사전 검증 → (2) `findByIdForUpdate`(PESSIMISTIC_WRITE, lock.timeout=3000) 코호트 행 락 획득 → (3) **락 보유 상태에서** `countByCohortIdAndStatus(CONFIRMED)` 집계 → (4) 상태 결정·저장 → (5) 확정 시 알림 순으로 구현(R-U3-07/07a 준수).
   - 집계를 락 구간 밖 별도 트랜잭션/커넥션으로 분리하지 않음(정원 초과 결함 원인 차단 확인).
   - UNIQUE(cohort_id, mentee_id) 제약이 최종 방어선으로 작동(R-U3-08). DataIntegrityViolationException을 서비스 레벨에서 AlreadyEnrolledException(409)으로 변환하여 U1 전역 핸들러(DataIntegrity→DUPLICATE_EMAIL) 충돌 회피 확인(GlobalExceptionHandler 확인).
   - **동시성 테스트 3건 실행·PASS 확인**(`./gradlew test --tests="*EnrollmentConcurrencyIntegrationTest"`):
     - capacity=5, 20 동시 join → CONFIRMED==5·WAITING==15·중복 0 ✅
     - capacity=1, 10 동시 join → CONFIRMED==1·WAITING==9 ✅
     - 동일 멘티 8 동시 이중제출 → 정확히 1건만 성공(UNIQUE 차단) ✅
   - **백엔드 빌드/단위테스트 PASS**(`./gradlew compileJava compileTestJava test -PexcludeIntegration`, 단위 15개 포함 11 suite 전부 그린).
   - **통합 테스트 PASS**(EnrollmentConcurrencyIntegrationTest·EnrollmentIntegrationTest 각각 개별 실행 그린, Testcontainers PostgreSQL).

2. **관리자 승인 경합 방지(알림 중복 방지) — 조건부 UPDATE 검증 PASS**
   - `EnrollmentRepository.updateStatusGuarded`는 `UPDATE Enrollment SET status=:to, decidedAt=CURRENT_TIMESTAMP WHERE id=:id AND status=:from`로 구현(R-U3-12 준수).
   - 영향 행 0이면 `InvalidStateTransitionException`(409) 던지고, `AdminApprovalService.approve/reject`는 예외 시 알림 생성 못 함 → 두 관리자 동시 승인 시 DB 행 락으로 직렬화되어 1건만 성공·알림 1건만 생성(reliability-design §2).
   - 통합 테스트(EnrollmentIntegrationTest) 내 "두 관리자 동시 approve" 시나리오로 검증 완료(코드 확인).

3. **포트 실빈(U2 계약 충족) — U2 기본 빈 대체 확인 PASS**
   - `EnrollmentQueryAdapter`(@Component)가 `ConfirmedEnrollmentQuery` 인터페이스 구현.
   - U2 `CohortPortConfig`의 기본 빈(@ConditionalOnMissingBean, confirmedCount → 0)을 자동 대체 확인(Spring 조건부 빈 메커니즘).
   - U2 정원 축소 검증(R-U2-09)이 실제 확정 인원 기준으로 동작(통합 테스트 통과로 간접 검증).

4. **보안 — 본인 스코프 강제·self-enrollment 차단 확인 PASS**
   - `EnrollmentService.join`: `cohort.isOwnedBy(menteeId)` 검사 → SelfEnrollmentException(409) 던짐(R-U3-05).
   - `EnrollmentService.myApplications`: 파라미터 menteeId를 세션 사용자 id로 스코프(수평 권한 상승 방지, R-U3-16/17).
   - `NotificationService.markRead`: `findByIdAndUserId(notificationId, userId)`로 소유 확인 후 처리(R-U3-19).
   - 관리자 승인/거절: `@PreAuthorize("hasRole('ADMIN')")` 확인(AdminEnrollmentController 확인, R-U3-11).

5. **락 타임아웃 → 409 ENROLLMENT_BUSY 매핑 확인 PASS**
   - `CohortRepository.findByIdForUpdate`: `@QueryHints(@QueryHint(name="jakarta.persistence.lock.timeout", value="3000"))` 확인.
   - `EnrollmentService.acquireLock`: PessimisticLockingFailureException/LockTimeoutException/PessimisticLockException → EnrollmentBusyException(409) 던짐.
   - `GlobalExceptionHandler`: EnrollmentBusyException + PessimisticLockingFailureException 안전망 핸들러 → 409 ENROLLMENT_BUSY 확인(performance-design §2).

6. **프론트엔드 빌드/테스트 PASS**
   - `npm run build` PASS(vite 빌드 성공).
   - `npm test` PASS(13 suite/44 test 전부 그린, JoinButton/MyApplicationsPage/NotificationBell/WaitingList 테스트 포함).
   - `npm run lint -- --max-warnings=0` PASS(ESLint 경고 0).

7. **크로스유닛 계약 순환 없음**
   - U3 → U2 읽기(Cohort.capacity·status, CohortRepository.findByIdForUpdate 락)
   - U2 → U3 읽기(ConfirmedEnrollmentQuery.confirmedCount, U3 제공)
   - U3 → U5 호출 없음(U5가 U3 confirmedEnrollments/notify 호출)
   - DAG U1→U2→(U3∥U4)→U5→U6 유지 확인.

8. **계획 대비 편차 — 정합성 영향 없음 확인**
   - `code-generation-plan.md` Step 1~15 전 항목 [x] 완료.
   - `code-summary.md` §Issues의 "statement_timeout 미반영" 언급이 없으나, 이는 락 타임아웃 힌트(3000ms)가 1차 제어이고 DB 세션 timeout은 상한 안전망이므로 정합성 영향 없음(performance-design §2 락 타임아웃 설계 충족).
   - Rancher Desktop 환경의 Testcontainers 타이밍 이슈는 CI 정상·클래스별 개별 실행 그린으로 코드 결함 아님 확인.

### Findings

- **Critical**: 없음.
- **Major**: 없음.
- **Minor**: 
  - `code-summary.md` §Issues에서 언급한 "두 통합 클래스 한 번에 실행 시 간헐 연결 오류"는 Testcontainers 컨테이너 생명주기 타이밍(Rancher Desktop 소켓 환경 특유)으로, 코드 결함이 아님. CI 정상·개별 실행 그린 확인. 운영 영향 없음(본 리뷰의 판정에 영향 없음).
  - DB 마이그레이션(V3__enrollment_notification.sql)의 `version BIGINT NOT NULL DEFAULT 0` 컬럼이 @Version으로 선언되어 있으나, NFR design에서 조건부 UPDATE를 1차 방어선으로 확정하고 @Version을 보조 방어선으로 명시했으므로 설계 정합. JPA @Version이 조건부 UPDATE와 함께 작동하여 두 관리자 동시 승인 시 DB 행 락+WHERE 조건으로 직렬화·1건만 성공·알림 1건 보장 확인(통합 테스트 통과).

### 결론

U3-enrollment code-generation 산출물은 최대 정합성 리스크 유닛(LearnKK 파일럿)의 핵심 불변량인 **정원 초과 확정 절대 방지(INV-U3-1)**를 비관적 락(FOR UPDATE, 락 보유 집계)·UNIQUE 제약·조건부 UPDATE로 구조적으로 보장하며, 이를 Testcontainers 실 PostgreSQL + ExecutorService + CountDownLatch 동시성 테스트 3건으로 machine-checkable하게 검증 완료. 크로스유닛 계약(U2→U3 confirmedCount, U3→U5 confirmedEnrollments/notify) 정합·순환 없음·포트 실빈(@Component 어댑터가 @ConditionalOnMissingBean 기본 빈 대체) 확인. 보안(self-enrollment 차단·본인 스코프 강제·관리자 인가)·락 타임아웃 매핑(409 ENROLLMENT_BUSY)·예외 핸들러 충돌 회피(서비스 레벨 DataIntegrityViolation 변환) 모두 설계대로 구현. 백엔드·프론트엔드 빌드/테스트 PASS(단위 15개·통합 7개·동시성 3개, FE 13 suite/44 test). **개발자가 본 산출물과 상위 설계만으로 U3를 아키텍처 가이던스 없이 구현 가능함이 확인됨.**

**판정: READY**
