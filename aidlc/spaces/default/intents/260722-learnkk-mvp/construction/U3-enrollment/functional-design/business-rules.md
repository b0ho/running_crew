# Business Rules — U3 enrollment (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U3-enrollment
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U3 책임), `unit-of-work-story-map.md`(US-6a/6b/7/8), `requirements-analysis/requirements.md`(FR-3/4/10), `application-design/components.md`(Enrollment·Notification·UNIQUE), `component-methods.md`(EnrollmentService·AdminApprovalService·NotificationService), `services.md`(동시성 노트)
> 규칙 표기: R-U3-nn. U1 공통 에러 핸들러·매핑 표를 재사용하고 U3 신규 예외를 추가.

## 1. 선착순 참여 규칙 (US-6a·6b / FR-3) — 핵심 동시성

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U3-01 | 참여 신청은 **인증된 사용자**만. status는 신청 시점 정원 여유에 따라 확정 또는 대기중으로 결정 | 401 UNAUTHORIZED |
| R-U3-02 | **정원 여유(confirmed < capacity)면 확정(CONFIRMED)**, 여유 없으면 **대기중(WAITING)**으로 등록(FR-3) | — |
| R-U3-03 | **정원 초과 확정이 절대 발생하지 않아야 한다**: 정원 N에 동시 N+k 요청 시 정확히 N명만 CONFIRMED, 나머지는 WAITING(FR-3 수용기준) | 동시성 결함 = 심각 결함 |
| R-U3-04 | 동일 (cohortId, menteeId) **중복 신청 불가**. 이미 Enrollment가 있으면 상태에 따라 거부(확정/대기중이면 409 ALREADY_ENROLLED) | 409 ALREADY_ENROLLED |
| R-U3-05 | 자기 자신이 멘토인 코호트에는 멘티로 참여 불가(self-enrollment 방지) | 409 CONFLICT (code: SELF_ENROLLMENT) |
| R-U3-06 | 종료됨(CLOSED) 코호트에는 신규 참여 불가. 모집중/진행중만 참여 허용(참여 마감 정책: 파일럿은 진행중도 허용) | 409 CONFLICT (code: COHORT_NOT_OPEN) |

### 1.1 동시성 제어 규칙 (team.md: 유니크 제약 + 비관적 락)

| ID | 규칙 |
|---|---|
| R-U3-07 | join은 **단일 `@Transactional` 경계** 내에서 대상 Cohort 행에 **비관적 쓰기 락**(JPA `LockModeType.PESSIMISTIC_WRITE` = `SELECT ... FOR UPDATE`)을 획득해 정원 확인→확정을 원자적으로 수행한다(team.md). 락은 **트랜잭션 커밋 시점까지 보유**되며, 커밋 전에는 해제되지 않는다 |
| R-U3-07a | 락 획득(`findByIdForUpdate(cohortId)`), 확정 수 집계, Enrollment insert가 **모두 동일 트랜잭션·동일 락 구간 안에서** 순서대로 실행되어야 한다. confirmedCount 집계를 락 밖 별도 트랜잭션/커넥션에서 수행하는 구현은 금지(정원 초과 결함 원인) |
| R-U3-07b | 트랜잭션 격리 수준은 최소 **READ_COMMITTED**로 충분하다 — 정합성은 격리 수준이 아니라 Cohort 행에 대한 PESSIMISTIC_WRITE 락(행 직렬화)이 보장한다. 락이 동일 코호트의 동시 join을 직렬화하므로 팬텀/논리팬텀 문제는 발생하지 않는다(각 join은 자신이 삽입할 단일 행만 다루고 집계는 락 보유 하에 수행) |
| R-U3-08 | (cohortId, menteeId) **UNIQUE 제약**을 최종 방어선으로 둔다. 락 경합을 통과한 중복/이중 제출은 제약 위반(`DataIntegrityViolationException`)으로 차단해 409 ALREADY_ENROLLED로 매핑 |
| R-U3-09 | 확정 수 판정은 **락 보유 상태에서** `confirmedCount(cohortId)`(status=CONFIRMED 집계 `SELECT COUNT(*) FROM enrollment WHERE cohort_id=? AND status='CONFIRMED'`)로 계산한다. capacity와 비교 후 상태 결정. 집계는 R-U3-07의 트랜잭션 안에서 실행(R-U3-07a) |
| R-U3-10 | 동시성 검증 테스트(**ExecutorService + CountDownLatch**, Testcontainers 실 DB)를 작성한다. 시나리오: capacity=N에 동시 스레드 N+5 join → CONFIRMED == N, WAITING == 5, 중복 0, 정원 초과 0을 단언. 최소 2개 시나리오(정원 여유 다수 동시, 정원 경계 초과 다수 동시)로 검증(team.md 핵심 도메인 80% 대상) |

## 2. 관리자 승인/거절 규칙 (US-8 / FR-10)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U3-11 | 대기 목록 조회·승인·거절은 **관리자(ROLE_ADMIN)만**(`@PreAuthorize` — U1 R-U1-16a) | 403 FORBIDDEN |
| R-U3-12 | 승인 대상은 **대기중(WAITING)** 상태만. 이미 확정/거절이면 409 INVALID_STATE_TRANSITION | 409 |
| R-U3-13 | 승인 시 status → 확정(CONFIRMED), decidedAt 기록. 정원 마감 상황의 수동 승인이므로 **capacity를 초과할 수 있다**(관리자 판단 — `cid:scope-definition:c1`). 초과 승인은 경고 없이 허용하되 감사 로그에 남긴다 |
| R-U3-14 | 거절 시 status → 거절(REJECTED), decidedAt 기록 |
| R-U3-15 | 승인·거절 결과는 **즉시 멘티 상태에 반영**되고 알림을 생성한다(FR-4/10 수용기준, R-U3-18) |

## 3. 신청 상태·알림 규칙 (US-7 / FR-4)

| ID | 규칙 |
|---|---|
| R-U3-16 | 멘티는 자신의 신청 상태(대기중/확정/거절)를 조회할 수 있다(`myApplications`) |
| R-U3-17 | 타인의 신청 상태는 조회 불가(본인·관리자만) | 
| R-U3-18 | 상태 변경(확정·거절) 시 `NotificationService.notify`로 멘티에게 알림 생성. 알림 유형: ENROLLMENT_CONFIRMED / ENROLLMENT_REJECTED |
| R-U3-19 | 알림 조회(`listFor`)·읽음 처리(`markRead`)는 본인만 |

## 4. 범위 외 규칙 (`cid:user-stories:c4`)

| ID | 규칙 |
|---|---|
| R-U3-20 | 대기 신청 취소, 확정 취소 시 대기자 자동 승격, 거절 후 재신청은 **파일럿 범위 외**. 해당 API·전이를 제공하지 않는다 |

## 5. 신규 예외 → HTTP 매핑 (U1 공통 표에 추가)

| ID | 예외 | HTTP | code |
|---|---|---|---|
| R-U3-21a | `AlreadyEnrolledException` | 409 | ALREADY_ENROLLED |
| R-U3-21b | `DataIntegrityViolationException`(UNIQUE(cohortId,menteeId) 위반) | 409 | ALREADY_ENROLLED |
| R-U3-21c | `SelfEnrollmentException` | 409 | SELF_ENROLLMENT |
| R-U3-21d | `CohortNotOpenException` | 409 | COHORT_NOT_OPEN |
| R-U3-21e | `InvalidStateTransitionException`(승인/거절 대상 상태 오류) | 409 | INVALID_STATE_TRANSITION (U2 R-U2-21d와 동일 핸들러 재사용) |

## 6. 불변식 (Invariants)

- INV-U3-1: 어떤 동시 실행에서도 한 코호트의 CONFIRMED 수는 자동 확정 경로만으로는 capacity를 초과하지 않는다(관리자 수동 승인은 예외, R-U3-13).
- INV-U3-2: (cohortId, menteeId)당 Enrollment는 최대 1건(UNIQUE).
- INV-U3-3: 상태 전이는 신청→(확정|대기중), 대기중→(확정|거절)만. 그 외 전이 없음.
- INV-U3-4: 모든 응답은 DTO 경계를 거친다(Entity 직접 노출 금지).
