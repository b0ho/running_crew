# Reliability Design — U4 attendance (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `nfr-requirements/reliability-requirements.md`(파일+DB 정합성·보상·백업), `functional-design/business-logic-model.md`(W-U4-1 §2 보상 알고리즘·§6 delete 계약), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 서버(U1 가용성 상속). U4 핵심은 파일+DB 정합성.

## 1. 파일+DB 정합성 설계(핵심 — INV-U4-1)

`reliability-requirements.md` §1 + business-logic-model §2의 실패 처리 알고리즘을 확정한다.

- **INV-U4-1 보장(회차 인증 → 증빙 최소 1건)**: 증빙 이력 저장(AttendanceEvidence)과 회차 인증 전이(U2 `SessionService.markVerified`)를 **동일 DB 트랜잭션**에서 수행한다 → 함께 커밋되거나 함께 롤백. "인증됐는데 증빙 없음"은 구조적으로 불가.
- **파일 저장은 트랜잭션 밖 선행(비트랜잭션 I/O)**: `FileStorageService.store`는 파일시스템 작업이라 DB 트랜잭션에 참여하지 않으므로, 트랜잭션 진입 전에 수행하고 `storedPath`를 획득한다.
- **처리 순서(확정)**:
  1. 사전 검증(존재·권한·종료·파일 제약) — 위반 시 저장 전 4xx.
  2. 파일 store(트랜잭션 밖) → storedPath.
  3. `@Transactional`: 이력 insert(filePath=storedPath) + `markVerified(sessionId)` (원자적).

## 2. 보상(Compensation) 설계

`reliability-requirements.md` §1의 보상 요구(R-U4-13)를 확정한다.

- **트랜잭션 롤백 시 고아 파일 보상**: 3단계 트랜잭션이 롤백되면 2단계에서 저장한 파일이 고아가 된다. 이때 U1 `FileStorageService.delete(storedPath)`(멱등)를 호출해 즉시 삭제한다.
- **보상 실패 처리(확정)**: delete까지 실패하면 `storedPath`를 **ERROR 로그**로 남기고(수동/후속 정리 대상) 500 INTERNAL_ERROR를 반환한다. **INV-U4-1은 여전히 유지**(회차 미인증·이력 없음 — 일관). 유일 잔여 리스크는 고아 파일이며 파일럿에서 수용(정합성 훼손 아님).
  - **로그 형식(확정 — grep 가능 패턴)**: 고아 파일 탐지·정리 자동화를 위해 고정 토큰으로 로깅한다: `log.error("ORPHAN_FILE_COMPENSATION_FAILED path={} error={}", storedPath, e.getMessage())`. 운영은 `ORPHAN_FILE_COMPENSATION_FAILED` 토큰을 grep해 잔여 고아 파일을 수동 정리한다.
- **보상 구현 위치**: 트랜잭션 롤백 감지 후 보상이므로, 서비스 메서드에서 트랜잭션 커밋/롤백 결과를 판단해 delete를 호출하는 구조(예: try/catch로 트랜잭션 템플릿 실패를 잡아 보상). `@TransactionalEventListener(AFTER_ROLLBACK)` 또는 명시적 try/catch 중 **명시적 try/catch**를 채택(파일럿 단순성·제어 명확성).

## 3. 결함 허용

`reliability-requirements.md` §2:
- 파일 검증 실패 → 400(트랜잭션 진입 전, 부작용 없음).
- 이력 저장/`markVerified` 실패 → 롤백 + 보상 delete + 500.
- U2 `markVerified` 호출 실패도 전체 롤백(회차 미인증·이력 미적재로 일관). 파일럿은 자동 재시도 없음.

## 4. 가용성 & 내구성

`reliability-requirements.md` §3:
- U1 best-effort 가용성 상속.
- **증빙 파일 내구성(중요)**: 로컬 볼륨 저장. U1 일 1회 스냅샷 백업 대상에 **DB뿐 아니라 업로드 파일 볼륨도 포함**한다 — 증빙 유실 방지. 파일 볼륨 백업은 infrastructure-design/code-generation에서 구현(볼륨 스냅샷 또는 rsync 스냅샷).

## 5. 검증(quality)

`reliability-requirements.md` §4:
- 업로드→인증→이력 **원자성**: 강제 롤백 시 회차 미인증 & 이력 0건 단언(Testcontainers 실 DB + 임시 디렉토리 파일 저장).
- 형식/크기 거부(400) 테스트.
- **고아 파일 보상 delete** 테스트: 트랜잭션 롤백 유도 후 저장 파일이 삭제됐는지 확인.
- markVerified 실패 시 전체 롤백 테스트.
