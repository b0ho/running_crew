# Business Rules — U2 cohort (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U2 책임), `unit-of-work-story-map.md`(US-3/4/5), `requirements-analysis/requirements.md`(FR-2/6), `application-design/components.md`(Cohort·Session·Announcement), `component-methods.md`(CohortService.create/update/list/get, AnnouncementService), `services.md`(CohortService·AnnouncementService)
> 규칙 표기: R-U2-nn. U1이 확립한 공통 에러 핸들러(@RestControllerAdvice)·예외→HTTP 매핑 표(U1 business-rules §4.1)를 재사용하며, U2 신규 예외를 그 표에 추가한다.

## 1. 코호트 개설 규칙 (US-3 / FR-2)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U2-01 | title은 비어 있지 않고 최대 200자 | 400 VALIDATION_ERROR |
| R-U2-02 | capacity는 1 이상 정수 | 400 VALIDATION_ERROR |
| R-U2-03 | sessionCount는 1 이상 정수. 개설 즉시 seq 1..sessionCount 회차 자동 생성 | 400 VALIDATION_ERROR |
| R-U2-04 | endDate >= startDate | 400 VALIDATION_ERROR |
| R-U2-05 | 개설자는 인증된 사용자여야 하며, 생성된 코호트의 mentorId = 개설자 id(컨텍스트 역할 멘토) | 401 UNAUTHORIZED |
| R-U2-06 | 개설 직후 status = 모집중(RECRUITING) | (불변) |

## 2. 코호트 수정 규칙 (US-4 수정 / FR-2)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U2-07 | 수정은 **소유 멘토만**(cohort.mentorId == 요청자). 타인 수정 불가 | 403 FORBIDDEN |
| R-U2-08 | status가 종료됨(CLOSED)인 코호트는 수정 불가 | 409 CONFLICT (code: COHORT_CLOSED) |
| R-U2-09 | capacity 축소 시 **현재 확정 인원 미만으로는 축소 불가**. 확정 인원 조회는 U3가 노출하는 read-only 계약 **`EnrollmentService.confirmedCount(cohortId): int`**(§ 8 크로스유닛 계약)를 호출해 판정한다. 축소가 확정 인원 이상이면 허용하되 경고 메시지 반환 | 확정 인원 미만 축소 → 409 CONFLICT (code: CAPACITY_BELOW_CONFIRMED) |
| R-U2-10 | sessionCount 변경 시: 증가는 추가 회차(seq 확장) 생성 허용. 이미 인증(VERIFIED)된 회차를 잘라내는 축소는 불가 | 인증 회차 축소 → 409 CONFLICT (code: SESSION_VERIFIED_LOCK) |

- R-U2-09의 확정 인원 확인은 U2가 소유하지 않는 데이터다. **component-methods.md의 EnrollmentService는 join/myApplications만 선언**하므로, U2는 U3에 read-only 조회 계약 `confirmedCount(cohortId): int` 추가를 **요구**한다(§ 8 참조 — 이 계약은 U3 functional-design이 반드시 구현해야 하는 통합 지점). 쓰기 결합·순환 없음(U2→U3 읽기만).

## 2.1 코호트 시작 전이 규칙 (모집중→진행중)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U2-09s | 모집중→진행중 전이는 **소유 멘토의 명시적 시작 액션**(`POST /api/cohorts/:id/start`)으로만 발생한다. 파일럿에는 스케줄러가 없으므로 startDate 자동 도래 전이는 두지 않는다(확장 후속 과제) | 비소유 403 / 이미 진행중·종료됨 409 INVALID_STATE_TRANSITION |

## 3. 코호트 상태 전이 규칙

| ID | 규칙 |
|---|---|
| R-U2-11 | 허용 전이는 모집중→진행중→종료됨 뿐. 그 외 전이(역전이 포함)는 거부(409 CONFLICT, code: INVALID_STATE_TRANSITION) |
| R-U2-12 | 모집중→진행중 전이는 U2가 소유하며 **멘토의 명시적 시작 액션**으로만 발생(R-U2-09s). 파일럿에 스케줄러 없음 → startDate 자동 전이 없음 |
| R-U2-13 | 진행중→종료됨 전이는 **U5의 종료 액션이 트리거**한다. U2는 status 필드 갱신 경로(리포지토리)만 제공하고 종료 판정·오케스트레이션을 수행하지 않으며 U5를 호출하지 않는다(`cid:units-generation:c2`) |
| R-U2-14 | 하드 삭제 대신 종료됨 상태 전이를 우선한다. 코호트 삭제 API는 파일럿 범위 외(`cid:application-design:c2`) |

## 4. 공지 규칙 (US-5 / FR-6)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U2-15 | 공지 작성은 **소유 멘토만** | 403 FORBIDDEN |
| R-U2-16 | body는 비어 있지 않아야 함 | 400 VALIDATION_ERROR |
| R-U2-17 | externalLink는 선택 항목. 값이 있으면 http/https URL 형식이어야 함 | 400 VALIDATION_ERROR |
| R-U2-18 | 공지 조회는 해당 코호트 참여자(멘토·확정 멘티) 및 관리자가 가능 | 403 FORBIDDEN |

## 5. 조회 규칙 (US-3/4)

| ID | 규칙 |
|---|---|
| R-U2-19 | 코호트 목록/탐색은 인증 사용자에게 공개(모집중·진행중 노출; 종료됨은 참여 이력자·관리자에게만). 페이지네이션 기본 20건 |
| R-U2-20 | 코호트 상세는 기본 정보 + 회차 목록 + 공지를 포함. 응답은 DTO(CohortDetailDto)로 반환하며 Entity를 직접 노출하지 않는다(Mandated NFR-7) |

## 6. 신규 예외 → HTTP 매핑(U1 공통 표에 추가)

U1 business-rules §4.1 표에 아래 U2 예외를 추가한다. 미등록 시 500 누수 방지.

| ID | 예외 | HTTP | code |
|---|---|---|---|
| R-U2-21a | `CohortClosedException` | 409 | COHORT_CLOSED |
| R-U2-21b | `CapacityBelowConfirmedException` | 409 | CAPACITY_BELOW_CONFIRMED |
| R-U2-21c | `SessionVerifiedLockException` | 409 | SESSION_VERIFIED_LOCK |
| R-U2-21d | `InvalidStateTransitionException` | 409 | INVALID_STATE_TRANSITION |
| R-U2-21e | `EntityNotFoundException`(cohort/session 미존재) | 404 | NOT_FOUND |

- R-U2-21e는 U1 business-rules §4.1 R-U1-17g(`EntityNotFoundException` → 404 NOT_FOUND)의 **동일 핸들러를 그대로 재사용**한다(신규 예외 아님). 표에는 U2 스코프에서 발생함을 명시하기 위해 재기재.

## 7. 불변식 (Invariants)

- INV-U2-1: Cohort.status는 항상 {모집중, 진행중, 종료됨} 중 하나이며 전이는 단방향.
- INV-U2-2: 한 Cohort의 Session seq는 1..sessionCount 연속·유일(UNIQUE(cohortId, seq)).
- INV-U2-3: capacity >= 1, sessionCount >= 1 항상 유지.
- INV-U2-4: 모든 응답은 DTO 경계를 거치며 JPA Entity를 직접 노출하지 않는다.
