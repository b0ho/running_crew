# Reliability Design — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U3-enrollment(최대 정합성 리스크)
> 리드 architect · 관점 quality·devsecops
> 상위 입력: `nfr-requirements/reliability-requirements.md`(정합성·결함허용·검증), `nfr-requirements/tech-stack-decisions.md`(PESSIMISTIC_WRITE·@Version/조건부 UPDATE), `functional-design/business-logic-model.md`(W-U3-1 §2 동시성·W-U3-5 승인)
> 전제: 로컬 단일 서버(U1 가용성 상속). U3는 정합성이 신뢰성의 핵심.

## 1. 정합성 설계(핵심 — FR-3 / INV-U3-1)

`reliability-requirements.md` §1의 불변식을 구체 메커니즘으로 확정한다.

- **정원 초과 확정 절대 불가(INV-U3-1)**: 두 겹 방어.
  1. **비관적 행 락**: `findByIdForUpdate(cohortId)`(`LockModeType.PESSIMISTIC_WRITE`)로 대상 Cohort 행을 잠그고, 락 보유 상태에서 `confirmedCount` 집계→상태 결정→insert를 **동일 트랜잭션**에서 수행(business-logic-model §2 step 2~6). 이로써 "정원 확인→확정" 구간을 코호트 단위로 직렬화(R-U3-03/07).
  2. **UNIQUE(cohort_id, mentee_id)**: 락을 우회하는 어떤 경로·이중 제출도 최종 차단(R-U3-08) → 위반 시 409 ALREADY_ENROLLED.
- **격리 수준**: READ_COMMITTED로 충분(정합성은 행 락이 보장, R-U3-07b). 집계를 별도 트랜잭션/커넥션으로 분리 금지(R-U3-07a — 분리 시 정원 초과 결함).
- **동시성 검증(게이팅 조건)**: Testcontainers 실 DB + ExecutorService + CountDownLatch로 N+k 동시 join 실행 → `CONFIRMED == min(N, 요청수)`, 초과분 WAITING, 중복 0 단언(R-U3-10). 정원 경계 케이스(N, N+1, N+5)를 필수 검증. **이 테스트 통과가 U3 신뢰성의 게이팅 조건**.

## 2. 결함 허용 & 트랜잭션 경계

- **join 실패 롤백**: join 트랜잭션 실패 시 전체 롤백(부분 확정 없음). 락 타임아웃/데드락은 롤백 후 오류 응답(409 ENROLLMENT_BUSY, `performance-design.md` §2). **파일럿은 서버 자동 재시도 없음** — 클라이언트가 재요청(U1 방침 상속).
- **알림 트랜잭션 경계 — 확정 결정**: 파일럿에서는 **확정(Enrollment CONFIRMED)과 알림 생성을 동일 트랜잭션에서 수행**한다(함께 커밋/함께 롤백). 근거: 단일 인스턴스·단일 DB에서 알림은 DB 레코드이므로 동일 트랜잭션이 가장 단순하고 "확정됐는데 알림이 유실"되는 창을 없앤다. best-effort 후속 큐(확정 우선·알림 비동기)는 다중 인스턴스/메시지 브로커 도입 시의 확장 과제로 명시(비동기 알림 도입 시 재설계).
- **관리자 승인 경합 방지 — 확정 메커니즘**: `approve`는 **조건부 UPDATE**(`UPDATE enrollment SET status='CONFIRMED', decided_at=now WHERE id=:id AND status='WAITING'`)로 상태 전이한다. 영향 행 수 0이면 이미 처리됨(다른 관리자가 선행 처리 또는 WAITING 아님) → 409 INVALID_STATE_TRANSITION. 이로써 두 관리자의 동시 승인에도 **CONFIRMED 전이·알림 생성이 1회만** 발생(R-U3-12/13). U2 상태 전이와 동일한 조건부 UPDATE 방식을 채택(일관성). `@Version`은 파일럿에서 미도입(조건부 UPDATE가 이 단일 전이를 충분히 보호).
- **정원 초과 수동 승인(R-U3-13)**: 관리자 승인은 정원 초과를 **의도적으로 허용**(자동 경로 INV-U3-1과 구분). 초과 승인은 감사 로그 후 알림 1건.

## 3. 가용성 & 내구성

`reliability-requirements.md` §3: U1 best-effort 가용성 상속. Enrollment/Notification은 RDB 영속, U1 일 1회 스냅샷 백업 포함. HA는 확장 후속.

## 4. 검증(quality)

`reliability-requirements.md` §4:
- **동시성 정원 경계(필수)**: N+k 동시 join → CONFIRMED 정확성·중복 0(§1).
- 중복 신청 차단(UNIQUE 위반 → 409).
- 관리자 승인 멱등/경합(조건부 UPDATE로 이중 승인 시 1건만 CONFIRMED·알림 1건).
- 상태 전이(WAITING→CONFIRMED/REJECTED만, 그 외 409).
- 락 타임아웃 경로(경합 지속 시 409 응답·롤백).
