# Domain Entities — U4 attendance (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U4 책임), `unit-of-work-story-map.md`(US-9/10), `requirements-analysis/requirements.md`(FR-5), `application-design/components.md`(AttendanceEvidence·FK), `component-methods.md`(AttendanceService·FileStorageService), `services.md`(AttendanceService·FileStorageService)
> 범위: U4 소유 AttendanceEvidence 상세화. Session(U2)·FileStorage(U1)는 참조·호출.

## 1. U4 소유 엔티티

| 엔티티 | 소유 | U4에서의 처리 |
|---|---|---|
| AttendanceEvidence | **U4** | 회차 증빙 업로드·검증·이력 |
| Session | U2 | 조회 + `SessionService.markVerified(sessionId)` 호출로 인증 전이 |
| (파일 저장) | U1 | `FileStorageService.store/load` 호출 |

## 2. AttendanceEvidence 엔티티

`components.md` 정의를 FR-5에 맞춰 상세화. **증빙은 회차(Session) 단위**로 멘토가 업로드하며, 업로드 즉시 해당 회차가 인증된다(FR-5, `cid:scope-definition:c2`).

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| sessionId | BIGINT FK→Session.id | NOT NULL, ON DELETE CASCADE | 대상 회차 |
| filePath | VARCHAR(512) | NOT NULL | FileStorageService가 반환한 저장 경로(웹루트 밖) |
| mimeType | VARCHAR(100) | NOT NULL | image/jpeg, image/png, application/pdf 중 하나 |
| size | BIGINT | NOT NULL, <= 10MB | 파일 크기(바이트) |
| uploadedBy | BIGINT FK→User.id | NOT NULL, ON DELETE RESTRICT | 업로더(회차 코호트의 멘토) |
| createdAt | TIMESTAMP | NOT NULL | 업로드 시각(이력 순서) |

- **증빙 범위(scope) 해석**: 파일럿에서 증빙은 **회차 단위**다. 멘토가 회차 증빙을 올리면 그 회차가 인증(VERIFIED)되고, 해당 코호트 확정 멘티 전원의 그 회차 출석이 인증된 것으로 본다. 멘티별 개별 증빙(per-mentee granularity)은 파일럿 범위 외. (components.md의 `menteeScope`는 이 회차 단위 모델에서 사용하지 않으며, 확장 시 멘티별 증빙 도입 지점으로 남긴다.)
- 이력: 한 회차에 복수 증빙 업로드 가능(재업로드·추가 증빙). 최신 업로드가 회차 인증을 유지하며 이전 증빙도 이력으로 보존.

## 3. 관계·FK 정책 (components.md 준수)

- AttendanceEvidence.sessionId → Session.id (CASCADE): 회차 삭제 시 증빙 삭제.
- AttendanceEvidence.uploadedBy → User.id (RESTRICT).

## 4. 회차 인증 전이 (U2 계약 사용)

- U4는 Session 엔티티를 소유하지 않는다. 회차 status 예정→인증 전이는 **U2가 제공하는 `SessionService.markVerified(sessionId)`**를 호출해 수행한다(U2 business-logic-model §8 계약, 리포지토리 직접 접근 금지).

## 5. 크로스유닛 계약 (U4 제공/요구)

| 방향 | 계약 | 비고 |
|---|---|---|
| U4 → U1 (호출) | `FileStorageService.store(file, constraints)` / `load(path)` / **`delete(path)`**(보상 삭제) | 파일 저장·조회·삭제(U1 제공; delete는 U1에 추가 요구 계약) |
| U4 → U2 (호출) | `SessionService.markVerified(sessionId)` | 회차 인증 전이(U2 제공) |
| U4 → U2 (읽기) | Session 목록·소속 Cohort·mentorId 조회 | 권한 판정·진도 조회 |
| U5/U6 → U4 (읽기) | 회차 인증 상태·증빙 이력 조회 | 수료 판정(U5)·집계·이력(U6) |

## 6. 다른 유닛과의 경계

- U4는 Session(U2)을 소유하지 않고 인증 전이를 U2 계약으로 위임. FileStorage(U1)도 호출만.
- 수료 판정(출석≥80%)은 U5가 회차 인증 상태를 읽어 수행. U4→U5 호출 없음.
- 증빙 이력 조회(관리자, US-15)는 U6(HistoryService)이 U4 데이터를 읽어 제공.
