# Business Rules — U4 attendance (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U4 책임), `unit-of-work-story-map.md`(US-9/10), `requirements-analysis/requirements.md`(FR-5, NFR-5), `application-design/components.md`(AttendanceEvidence), `component-methods.md`(AttendanceService.uploadEvidence/sessionsOf, FileStorageService.store), `services.md`
> 규칙 표기: R-U4-nn. U1 공통 에러 핸들러·매핑 표(파일 제약 R-U1-21~24 포함) 재사용.

## 1. 증빙 업로드·회차 인증 규칙 (US-9 / FR-5)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U4-01 | 증빙 업로드는 **해당 회차가 속한 코호트의 소유 멘토만**(session→cohort.mentorId == 요청자) | 403 FORBIDDEN |
| R-U4-02 | 허용 형식은 이미지(jpg/png)와 문서(pdf)만. 그 외 거부 | 400 FILE_CONSTRAINT_VIOLATION (U1 R-U1-22) |
| R-U4-03 | 파일당 최대 크기 10MB 초과 거부 | 400 FILE_CONSTRAINT_VIOLATION (U1 R-U1-23) |
| R-U4-04 | 파일 저장은 **U1 `FileStorageService.store`**를 사용(웹루트 밖·서버 생성 파일명, U1 R-U1-21/24). U4는 저장 경로 메타만 보관 | — |
| R-U4-05 | 업로드 성공 즉시 해당 회차를 인증한다: **U2 `SessionService.markVerified(sessionId)`** 호출로 Session.status 예정→인증 전이(FR-5 수용기준) | — |
| R-U4-06 | 업로드마다 AttendanceEvidence 이력 1건 적재. 재업로드 시 이전 증빙도 이력 보존, 회차는 인증 상태 유지 | — |
| R-U4-07 | 종료됨(CLOSED) 코호트의 회차에는 증빙 업로드 불가 | 409 CONFLICT (code: COHORT_CLOSED, U2 핸들러 재사용) |
| R-U4-08 | 대상 회차/코호트가 없으면 404 | 404 NOT_FOUND |

- **증빙 단위(회차)**: 회차 증빙은 그 회차의 출석을 인증하며, 코호트 확정 멘티 전원의 해당 회차 출석으로 간주(파일럿, domain-entities §2). 멘티별 증빙은 범위 외.
- **파일 보안(파일럿 잔여 리스크)**: 바이러스 스캔·심층 콘텐츠 검증은 보류(`cid:practices-discovery:c3`, NFR-5). MIME·크기·확장자 기본 검증만 강제(U1 계약).

## 2. 진도·출석 조회 규칙 (US-10)

| ID | 규칙 |
|---|---|
| R-U4-09 | 회차 목록·인증 상태 조회(`sessionsOf(cohortId)`)는 해당 코호트 참여자(멘토·확정 멘티)와 관리자만 | 
| R-U4-10 | 진도 = 인증(VERIFIED) 회차 수 / 전체 회차 수. 조회 응답에 인증 회차 수·전체 회차 수·진도율 포함 |
| R-U4-11 | 증빙 파일 다운로드(`load`)는 참여자·관리자만. U1 FileStorageService.load 경유(경로 이탈 방지) | 403 FORBIDDEN / 404 |

## 3. 신규 예외 → HTTP 매핑 (U1 공통 표에 추가/재사용)

| ID | 예외 | HTTP | code |
|---|---|---|---|
| R-U4-12a | `FileConstraintViolationException`(형식/크기) | 400 | FILE_CONSTRAINT_VIOLATION (U1 R-U1-17h 재사용) |
| R-U4-12b | `CohortClosedException`(종료된 코호트 업로드) | 409 | COHORT_CLOSED (U2 R-U2-21a 재사용) |
| R-U4-12c | `EntityNotFoundException`(회차/코호트 미존재) | 404 | NOT_FOUND (U1 R-U1-17g 재사용) |

- U4는 신규 예외 타입을 최소화하고 U1/U2가 확립한 핸들러를 재사용한다.

## 3.5 잔여 리스크·정책 (파일럿)

| ID | 항목 | 정책 |
|---|---|---|
| R-U4-13 | **고아 파일 리스크**: 파일 저장(비트랜잭션) 후 DB 트랜잭션이 롤백되면 파일이 고아가 될 수 있다. 보상으로 `FileStorageService.delete(path)`를 즉시 호출하고, 실패 시 경로를 ERROR 로그로 남긴다(수동/후속 배치 정리). **증빙 이력 저장과 회차 인증(markVerified)은 동일 트랜잭션**이므로 "인증됐는데 이력 없음"은 발생하지 않는다(INV-U4-1 구조적 보장). 잔여 리스크는 디스크 고아 파일뿐이며 파일럿에서 허용(확장 시 주기적 GC 배치 도입) |
| R-U4-14 | **코호트 삭제 경합(A2)**: 증빙 업로드 중 관리자가 코호트를 삭제(CASCADE)하면 트랜잭션이 FK 제약으로 실패해 500을 반환한다. 파일럿은 코호트 하드 삭제를 지양(종료됨 전이 우선, `cid:application-design:c2`)하므로 발생 가능성 낮음 — 허용 리스크로 기록 |
| R-U4-15 | **사용자 삭제 정책(A3)**: AttendanceEvidence.uploadedBy는 ON DELETE RESTRICT이므로 증빙을 올린 멘토 User는 삭제되지 않는다(감사 이력 보존). 파일럿은 사용자 삭제를 지원하지 않는다(확장 시 soft-delete 또는 uploadedBy 재할당 검토). U1 사용자 삭제 API는 증빙 존재 시 409로 거부됨을 전제 |

## 4. 불변식 (Invariants)

- INV-U4-1: 인증(VERIFIED) 회차에는 AttendanceEvidence가 최소 1건 존재한다(증빙 없이 인증되지 않음). **보장 메커니즘**: 증빙 이력 저장과 markVerified가 동일 DB 트랜잭션(business-logic-model §2 3단계)이므로 함께 커밋/롤백된다. 파일 저장 실패나 트랜잭션 롤백 시 회차는 인증되지 않는다.
- INV-U4-2: 저장 파일은 항상 웹루트 밖에 있으며 직접 URL 접근 불가(U1 R-U1-21).
- INV-U4-3: 저장 파일의 mimeType ∈ {image/jpeg, image/png, application/pdf}, size <= 10MB.
- INV-U4-4: 회차 인증 전이는 U4가 직접 Session을 수정하지 않고 U2 계약(markVerified)으로만 수행한다.
