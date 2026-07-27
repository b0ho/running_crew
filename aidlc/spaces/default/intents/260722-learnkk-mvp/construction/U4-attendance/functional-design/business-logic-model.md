# Business Logic Model — U4 attendance (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U4 책임), `unit-of-work-story-map.md`(US-9/10), `requirements-analysis/requirements.md`(FR-5), `application-design/components.md`(AttendanceEvidence), `component-methods.md`(AttendanceService.uploadEvidence/sessionsOf, FileStorageService.store/load), `services.md`(AttendanceService·FileStorageService)
> 범위: 회차 증빙 업로드·검증·회차 인증 전이·증빙 이력·진도/출석 조회.

## 1. U4 워크플로 목록

| # | 워크플로 | 스토리 | 서비스 메서드 |
|---|---|---|---|
| W-U4-1 | 회차 증빙 업로드 + 회차 인증 | US-9 | AttendanceService.uploadEvidence |
| W-U4-2 | 진도·출석 조회 | US-10 | AttendanceService.sessionsOf |
| W-U4-3 | 증빙 다운로드 | US-10 | FileStorageService.load(경유) |

## 2. W-U4-1 증빙 업로드·회차 인증 (AttendanceService.uploadEvidence)

`uploadEvidence(mentorId, sessionId, file): EvidenceDto`.

**정확한 실패 처리 알고리즘 (A1 해소 — INV-U4-1 보장):**
1. **사전 검증(트랜잭션 밖)**: 회차 조회 → 소속 Cohort 확인(없으면 404, R-U4-08). 권한: cohort.mentorId == 요청자 아니면 403(R-U4-01). 코호트 종료됨이면 409 COHORT_CLOSED(R-U4-07). 파일 MIME·크기 사전 검증(위반 시 400, 파일 저장 전).
2. **파일 저장(비트랜잭션 외부 I/O)**: **U1 `FileStorageService.store(file, {mime∈허용, size≤10MB})`** 호출(R-U4-02~04). 실패(제약 위반) 시 400 FILE_CONSTRAINT_VIOLATION, 이후 단계 진입 안 함. 성공 시 `storedPath` 획득.
3. **DB 트랜잭션(원자적)** `@Transactional { }`:
   - a. AttendanceEvidence 이력 저장(filePath=storedPath·mimeType·size·uploadedBy·createdAt, R-U4-06).
   - b. U2 `SessionService.markVerified(sessionId)` 호출로 Session.status 예정→인증(R-U4-05).
   - **(a)와 (b)는 동일 트랜잭션**이므로 함께 커밋되거나 함께 롤백된다 → "인증되었는데 증빙 이력 없음"은 발생 불가(INV-U4-1 보장).
4. **트랜잭션 실패 시 보상(compensation)**: 3단계 트랜잭션이 롤백되면 2단계에서 저장한 파일은 고아가 된다. 이때 **U1 `FileStorageService.delete(storedPath)`**(§6 요구 계약)를 호출해 고아 파일을 즉시 삭제 시도한다. delete까지 실패하면 `storedPath`를 ERROR 로그로 남기고(수동 정리 대상) 500 INTERNAL_ERROR 반환. **INV-U4-1은 여전히 유지**(회차가 인증되지 않았고 이력도 없음 — 일관).
5. 성공 시 201 EvidenceDto 반환.

결정 트리:
```
uploadEvidence(mentorId, sessionId, file)
  ├─ 회차/코호트 없음? ─> 404
  ├─ 소유 멘토 아님? ─> 403
  ├─ 코호트 종료됨? ─> 409 COHORT_CLOSED
  ├─ 파일 형식/크기 위반? ─> 400 FILE_CONSTRAINT_VIOLATION (저장 전)
  └─ FileStorage.store(성공) 
       └─ [TX] Evidence 저장 + U2.markVerified  (원자적)
            ├─ 커밋 성공 ─> 201 EvidenceDto
            └─ 롤백 ─> FileStorage.delete(고아파일) 시도 -> 실패 시 경로 ERROR 로그 -> 500
```
<!-- Text fallback: uploadEvidence는 트랜잭션 밖에서 존재/권한/종료/파일제약을 검증하고 파일을 저장한 뒤, 하나의 DB 트랜잭션에서 증빙 이력 저장과 회차 인증(markVerified)을 원자적으로 수행한다. 트랜잭션이 롤백되면 저장된 파일을 U1 delete로 삭제하고, 삭제도 실패하면 경로를 로그로 남기고 500을 반환한다. 회차 인증과 이력은 같은 트랜잭션이라 인증만 되고 이력이 없는 상태는 발생하지 않는다. -->

- **정합성 우선순위**: Evidence 이력 저장과 markVerified를 동일 트랜잭션에 묶어 INV-U4-1("인증 회차 → 증빙 최소 1건")을 구조적으로 보장한다. 유일한 잔여 리스크는 보상 delete까지 실패한 **고아 파일**이며, 이는 business-rules 잔여 리스크로 기록(수동/후속 정리).

## 3. W-U4-2 진도·출석 조회 (sessionsOf)

`sessionsOf(cohortId): SessionAttendanceDto[]` — 회차별 status(예정/인증)와 진도 요약.
- 권한: 참여자(멘토·확정 멘티)·관리자(R-U4-09).
- 진도율 = 인증 회차 수 / 전체 회차 수(R-U4-10). 응답에 인증 수·전체 수·비율 포함.

## 4. W-U4-3 증빙 다운로드

- `FileStorageService.load(filePath)` 경유. 참여자·관리자만(R-U4-11). 경로 이탈 방지는 U1이 보장.

## 5. 수료 판정 연계 (U5가 읽음)

- 수료 판정(출석 인증 회차 ≥ 80%, FR-8)은 **U5가** U4의 회차 인증 상태(Session.status via U2, AttendanceEvidence via U4)를 읽어 수행한다. U4는 인증 상태·증빙 이력을 read-only로 제공하며 판정을 수행하지 않는다(U4→U5 호출 없음).
- **파일럿 단순화(중요)**: 증빙이 회차 단위이므로 한 코호트의 모든 확정 멘티는 동일한 인증 회차 집합을 공유한다 → 수료 판정 시 멘티별 출석률이 동일. 멘티별 차등 출석은 파일럿 범위 외(domain-entities §2). U5 설계는 이 전제를 사용한다.

## 6. 크로스유닛 통합 계약 (U4 제공/요구)

| 방향 | 계약 | 상태 |
|---|---|---|
| U4 → U1 (호출) | `FileStorageService.store/load` | U1 제공 |
| U4 → U1 (호출) | **`FileStorageService.delete(path)`** — 트랜잭션 롤백 시 고아 파일 보상 삭제(§2 4단계) | **U1이 노출해야 할 계약**(현재 component-methods.md는 store/load만 선언 → U1 functional-design에 delete 추가 요구) |
| U4 → U2 (호출) | `SessionService.markVerified(sessionId)` | U2 제공(U2 §8 계약) |
| U4 → U2 (읽기) | Session·Cohort 조회(권한·진도) | U2 제공 |
| U5 → U4 (읽기) | 회차 인증 상태(수료 판정) | U4 제공 |
| U6 → U4 (읽기) | 증빙 이력(관리자 이력 조회 US-15)·출석 집계 | U4 제공 |

## 7. 프론트엔드 연동

U4는 UI 포함 → 상세는 `frontend-components.md`. 요약: 코호트 상세의 "진도·출석" 탭(SessionList + 인증 배지 + 진도율), 멘토용 회차별 FileUpload, 증빙 미리보기/다운로드. 모든 호출 U1 ApiClient 경유.

## 8. 데이터 흐름 요약

```
U4(AttendanceEvidence) --호출--> U1(FileStorage.store/load), U2(markVerified)
U4 --읽음--> U2(Session/Cohort)
U4 --제공(read)--> U5(회차 인증 상태), U6(증빙 이력·출석 집계)
U4 --호출 안 함--> U5
```
<!-- Text fallback: U4는 U1 파일 저장과 U2 회차 인증 전이를 호출하고 U2의 회차·코호트를 읽는다. 회차 인증 상태와 증빙 이력을 U5·U6에 read-only로 제공하며 U5를 호출하지 않는다. -->

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
