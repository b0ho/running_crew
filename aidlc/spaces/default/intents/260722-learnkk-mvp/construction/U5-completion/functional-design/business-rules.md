# Business Rules — U5 completion (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U5-completion
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U5 책임), `unit-of-work-story-map.md`(US-4 종료/US-11/12/13), `requirements-analysis/requirements.md`(FR-7/8/9), `application-design/components.md`(FinalReport·Certificate·SettlementStatus), `component-methods.md`(CompletionService.evaluateOnEnd/certificateOf, ReportService.submit/historyOf), `services.md`
> 규칙 표기: R-U5-nn. U1/U2/U3 공통 에러 핸들러 재사용.

## 1. 코호트 종료 액션 규칙 (US-4 종료 / 오케스트레이션 소유)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U5-01 | 코호트 종료(`endCohort`)는 **소유 멘토만**(cohort.mentorId == 요청자) | 403 FORBIDDEN |
| R-U5-02 | 종료 대상은 status가 **진행중(ONGOING)**인 코호트만. 모집중은 종료 불가(먼저 시작 필요), 이미 종료됨이면 409 | 409 INVALID_STATE_TRANSITION |
| R-U5-03 | 종료 액션은 **단일 트랜잭션**에서: (1) 수료 판정+수료증 발급, (2) 정산 판정, (3) Cohort.status→종료됨 전이(U2 세터), (4) 수료 결과 알림 생성을 원자적으로 수행 | 부분 실패 시 전체 롤백 |
| R-U5-04 | 종료 후 재종료·역전이 불가(U2 R-U2-11 단방향 준수) | 409 |

## 2. 수료 판정·수료증 규칙 (US-12 / FR-8)

| ID | 규칙 |
|---|---|
| R-U5-05 | 수료 대상은 해당 코호트의 **확정(CONFIRMED) 멘티**(U3 조회). 대기/거절 멘티는 제외 |
| R-U5-06 | **수료 기준: 인증 출석 회차 수 ≥ 전체 회차 수 × 0.8**. 부동소수 오차 회피를 위해 정수 비교 사용: `verifiedSessions * 100 >= totalSessions * 80` |
| R-U5-07 | 파일럿 단순화(U4 회차 단위 증빙): 한 코호트의 모든 확정 멘티는 동일한 인증 회차 집합을 공유 → 출석률이 동일. 따라서 코호트가 기준 충족이면 전 확정 멘티 수료, 미충족이면 전원 미수료(멘티별 차등 출석은 범위 외) |
| R-U5-08 | 수료 멘티에게 **수료증(Certificate) 1장** 발급. 확정 멘티 목록은 U3 `confirmedEnrollments(cohortId)`로 조회(count 아님). 멘티별로 이미지 생성(템플릿→PNG)→U1 `store`→imagePath로 Certificate insert. UNIQUE(cohortId, menteeId)로 중복 발급 방지(재종료 시 이미 있으면 skip) |
| R-U5-08a | **수료증 이미지 생성 순서·보상**: store(비트랜잭션)로 생성한 imagePath들을 리스트에 누적하고, 종료 트랜잭션 롤백 시 누적 imagePath 전부에 `FileStorageService.delete` 호출(루프 레벨 보상). delete 실패 경로는 ERROR 로그(수동 정리). Certificate.imagePath는 NOT NULL이므로 store 성공 후에만 insert |
| R-U5-09 | 미수료 멘티에게는 수료증 미발급. 결과 통지 시 "미수료 — 부족 항목(출석률)" 안내(FR-8 수용기준) |
| R-U5-10 | 전체 회차 수가 0이면(비정상) 수료 판정 불가 — 데이터 정합 오류로 처리(종료 전 회차 존재 보장은 U2 R-U2-03) |

- 경계 예시: totalSessions=5 → verified≥4 수료(4/5=80%), 3/5=60% 미수료. totalSessions=10 → verified≥8. **79/80% 경계 테스트 필수**(US-12 AC, team.md).

## 3. 정산 조건 판정 규칙 (US-13 / FR-9)

| ID | 규칙 |
|---|---|
| R-U5-11 | **정산 조건(멘토): 전 회차 출석 인증 완료(verifiedSessions == totalSessions) AND 멘토의 최종 보고서 제출**. "멘토 보고서 제출" 판정은 U5 자체 `ReportService.mentorReportExists(cohortId, mentorId): boolean` = `EXISTS(FinalReport WHERE cohortId=? AND authorId=cohort.mentorId)`로 조회(멘티 보고서와 구분 — authorId==mentorId) | 
| R-U5-12 | 조건 충족 시 SettlementStatus.satisfied=true, "정산 조건 충족" 메시지. 미충족 시 satisfied=false, 미표시(FR-9 수용기준) |
| R-U5-13 | 실제 정산 처리·결제 없음(`cid:scope-definition:c5`). satisfied 플래그와 메시지 수준으로만 표현 |
| R-U5-14 | 수료 기준(멘티 80%)과 정산 기준(멘토 전회차+보고서)은 **별개**로 판정한다(`cid:requirements-analysis:c1`) |

## 4. 최종 보고서 규칙 (US-11 / FR-7)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U5-15 | 최종 보고서는 멘토·멘티가 제출 가능. body(자유 서식) 필수, 파일 첨부 선택 | 400 VALIDATION_ERROR |
| R-U5-16 | 첨부 파일은 U1 FileStorageService 사용(형식/크기 제약·웹루트 밖, U1 R-U1-21~24). 파일+DB 결합 트랜잭션은 U4와 동일 보상 패턴(store→[TX submit]→롤백 시 delete, R-U5-17) | 400 FILE_CONSTRAINT_VIOLATION |
| R-U5-17 | 보고서 첨부 저장 실패 처리는 U4 R-U4-13과 동일: TX 롤백 시 `FileStorageService.delete(path)` 보상, 실패 시 경로 로그+500. 첨부 없는 보고서는 순수 DB 트랜잭션 | — |
| R-U5-18 | 제출 후 상태 "제출됨". 관리자 보고서 이력(US-15)에 표시(U6이 U5 데이터 조회) | — |
| R-U5-19 | 보고서 조회(`historyOf(cohortId)`)는 코호트 참여자·관리자만 | 403 FORBIDDEN |

## 5. 알림 연계

| ID | 규칙 |
|---|---|
| R-U5-20 | 종료 시 각 확정 멘티에게 수료/미수료 결과 알림 생성: **U3 `NotificationService.notify(userId, type, message)`** 호출(type: COMPLETION_RESULT). U5→U3 호출(단방향) |

## 6. 신규 예외 → HTTP 매핑 (U1 공통 표에 추가/재사용)

| ID | 예외 | HTTP | code |
|---|---|---|---|
| R-U5-21a | `InvalidStateTransitionException`(종료 대상 상태 오류) | 409 | INVALID_STATE_TRANSITION (재사용) |
| R-U5-21b | `FileConstraintViolationException`(보고서 첨부) | 400 | FILE_CONSTRAINT_VIOLATION (U1 재사용) |
| R-U5-21c | `EntityNotFoundException`(코호트/보고서 미존재) | 404 | NOT_FOUND (재사용) |
| R-U5-21d | `DataIntegrityException`(회차 수 0 등 정합 오류) | 500 | INTERNAL_ERROR |

## 7. 불변식 (Invariants)

- INV-U5-1: Certificate는 수료 기준(R-U5-06) 충족 멘티에게만, 코호트당 멘티당 1장(UNIQUE).
- INV-U5-2: SettlementStatus는 코호트당 1건(UNIQUE(cohortId)).
- INV-U5-3: 코호트 종료 트랜잭션은 원자적 — 수료증·정산·상태전이·알림이 모두 커밋되거나 모두 롤백.
- INV-U5-4: 종료됨 상태 전이는 U2 세터로만 수행(U5가 Cohort를 직접 수정하지 않음).
- INV-U5-5: 수료 판정은 정수 비교(R-U5-06)로 수행해 부동소수 경계 오차가 없다.
