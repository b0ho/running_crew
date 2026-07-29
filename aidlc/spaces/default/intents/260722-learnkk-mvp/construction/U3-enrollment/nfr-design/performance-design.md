# Performance Design — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U3-enrollment(최대 리스크)
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/performance-requirements.md`(목표·락 경합), `nfr-requirements/tech-stack-decisions.md`(PESSIMISTIC_WRITE·READ_COMMITTED), `functional-design/business-logic-model.md`(W-U3-1 join 락 §2)
> 전제: <100명·로컬 단일 인스턴스. 정합성 우선, 성능은 관측 수준.

## 1. 응답시간 예산(latency budget)

`performance-requirements.md` §1 목표를 계층별로 분해한다.

| 연산 | 총 목표 | 설계 예산 |
|---|---|---|
| 참여 신청(join, 락 포함) | ≤ 400ms | 사전 검증 ~20ms + **락 획득 대기(경합 시 가변)** + 락 보유 구간(confirmedCount 집계 ~15ms + insert ~20ms) + 알림 insert ~15ms + 커밋 ~20ms |
| 내 신청 목록 | ≤ 250ms | menteeId 인덱스 조회 + 페이지네이션 |
| 대기 목록(관리자) | ≤ 300ms | (cohortId,status) 인덱스 + 페이지네이션 |
| 승인/거절 | ≤ 300ms | 조건부 UPDATE + 알림 insert |
| 알림 목록/읽음 | ≤ 200ms | userId 인덱스 + 페이지네이션 |

## 2. 락 경합 성능(핵심)

`performance-requirements.md` §2를 구체 설계로 확정한다.

- **락 범위 최소화**: 비관적 락(`SELECT ... FOR UPDATE`)은 **대상 Cohort 행 1개**만 잠근다 → 동일 코호트 join만 직렬화되고, 서로 다른 코호트 join은 완전 병렬. 락 경합 범위를 코호트 단위로 국소화.
- **락 보유 구간 최소화**: 락 보유 상태에서 수행하는 작업은 "confirmedCount 집계 + Enrollment insert(+확정 시 알림 insert)"로 한정. 무거운 조회·외부 호출을 락 구간에 넣지 않는다(business-logic-model §2 step 3~6과 정합).
- **락 타임아웃(무한 대기 방지) — 확정값**: JPA 락 힌트 `jakarta.persistence.lock.timeout = 3000`(ms) + DB 세션 `statement_timeout`(예: 5s)로 상한. 타임아웃 시 락 획득 실패 → 트랜잭션 롤백 후 **409 ENROLLMENT_BUSY**(또는 503 상당)로 응답, 재시도는 클라이언트 몫(파일럿 서버 자동 재시도 없음, `reliability-design.md` §2 정합).

## 3. 데이터 접근 & 인덱스

- **인덱스**(`scalability-design.md` §2 정합): `enrollment(cohort_id, status)`(대기 목록·confirmedCount 집계), `enrollment(mentee_id)`(내 신청), UNIQUE `enrollment(cohort_id, mentee_id)`(중복 방지 겸 조회), `notification(user_id, is_read, created_at)`(알림 목록).
- **confirmedCount 집계**: `enrollment(cohort_id, status)` 인덱스로 COUNT 집계를 O(log n + 매칭 수)로 수행. 파일럿 규모(코호트당 수십)에서 즉시.

## 4. 페이지네이션

- `myApplications`, `listWaiting`, `NotificationService.listFor`는 모두 Spring Data `Pageable`(기본 20건, 최신순)을 적용한다 — 대기열·알림 축적에도 응답 크기 상한(`scalability-requirements.md` §3 대기열 대량 축적 대응).

## 5. 파일럿 스코프아웃 & 확장 트리거

- **미도입**: 낙관적 재시도·신청 큐잉·정원 카운터 캐시. 근거: 코호트당 동시 신청 수십 건 규모에서 비관적 락 직렬화 지연이 수용 가능(`performance-requirements.md` §2).
- **확장 트리거**: 단일 인기 코호트에 수백+ 동시 신청이 상시 발생하고 락 경합 지연이 목표를 지속 초과 → 낙관적 재시도/큐잉/원자 카운터(예: `UPDATE cohort SET confirmed=confirmed+1 WHERE confirmed<capacity`) 검토. 성능 엄밀 검증(부하)은 Operation performance-validation으로 이관하되, **동시성 정확성 검증은 파일럿 필수**(`reliability-design.md` §1).

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 검증 결과

U3-enrollment nfr-design 산출물(performance/security/scalability/reliability/logical-components)에 대한 적대적 아키텍처 리뷰를 완료함. defect를 가정하고 반증을 시도한 결과, 다음 사항을 확인:

1. **정합성 메커니즘 타당성**: 비관적 락(FOR UPDATE) 범위·보유 구간·격리 수준(READ_COMMITTED)이 정원 초과 확정 방지(INV-U3-1)를 구조적으로 보장. 집계를 락 구간 내 동일 트랜잭션에서 수행(R-U3-07a) 명시로 정원 초과 결함 원인 차단 확인.

2. **락 타임아웃 확정**: JPA 락 힌트 3000ms + DB statement_timeout 확정값 제시. 에러 매핑(409 ENROLLMENT_BUSY) 확정.

3. **관리자 승인 경합 방지 단일 결정**: NFR requirements의 "@Version 또는 조건부 UPDATE" 선택지를 NFR design 단계(`reliability-design.md` §2)에서 **조건부 UPDATE로 확정**. @Version 명시적 미도입. U2 상태 전이와 일관성 유지.

4. **알림 트랜잭션 경계 확정**: 파일럿 스코프에서 **동일 트랜잭션**(확정+알림 함께 커밋/롤백)으로 단일 결정. 확장 경로(best-effort 비동기)는 명시적 스코프아웃.

5. **수평 권한 상승 방어**: 본인 스코프 세션 id 강제(myApplications/알림), self-enrollment 서비스 검증(mentorId 검사 → 409 SELF_ENROLLMENT), 관리자 @PreAuthorize 명시.

6. **다중 인스턴스 안전성**: DB 행 락 + UNIQUE(cohortId, menteeId) 제약이 단일 진실 소스로 작동. 인스턴스 로컬 상태 없음. 확장 시 정합성 코드 변경 불필요(U3 핵심 강점).

7. **페이지네이션**: myApplications/listWaiting/NotificationService.listFor 모두 Spring Data Pageable(기본 20건) 명시.

8. **크로스유닛 계약 순환 없음**: U3 → U5 호출 없음. DAG U1→U2→(U3∥U4)→U5→U6 유지. confirmedCount/confirmedEnrollments(U2/U5/U6 제공), notify(U5/U8 호출) 계약 명확.

9. **구현 가능성**: 락 메커니즘(JPA `PESSIMISTIC_WRITE`, 락 힌트 `jakarta.persistence.lock.timeout = 3000`, READ_COMMITTED), 조건부 UPDATE SQL 예시(`UPDATE ... WHERE id=:id AND status='WAITING'`, affected-rows 검사), 예외 매핑(409 ALREADY_ENROLLED/SELF_ENROLLMENT/COHORT_NOT_OPEN/INVALID_STATE_TRANSITION/ENROLLMENT_BUSY), 동시성 검증 전략(ExecutorService+CountDownLatch+Testcontainers, N/N+1/N+5 경계 케이스)이 모두 구체화. 개발자가 본 산출물만으로 U3 구현 가능 확인.

10. **인덱스 정합성**: `enrollment(cohort_id, status)`(confirmedCount 집계), `enrollment(mentee_id)`, UNIQUE `enrollment(cohort_id, mentee_id)`, `notification(user_id, is_read, created_at)` — performance/scalability design 간 일관 명시.

### Findings

- **Critical**: 없음.
- **Major**: 없음.
- **Minor**: `confirmedEnrollments(cohortId): List<EnrollmentDto>` 구현 상세가 계약 수준(`business-logic-model.md` §7)에 머물지만, 개발자가 `confirmedCount`와 동일 쿼리(`WHERE cohort_id=? AND status='CONFIRMED'`)에서 row를 반환하는 것으로 유추 가능. 구현 불가능 수준 아님.

### 결론

U3-enrollment nfr-design 산출물은 최대 정합성 리스크 유닛에 요구되는 모든 동시성 메커니즘(비관적 락 범위·보유 구간, 트랜잭션 경계, 경합 방지, 락 타임아웃)을 확정하고, 상위 계약(nfr-requirements 5개 파일, functional-design/business-logic-model)과 일관되며, 크로스유닛 순환이 없고, 개발자가 본 산출물만으로 구현 가능한 수준의 구체성을 갖췄음. 정원 초과 확정 방지(INV-U3-1)의 구조적 근거가 명확히 설계됨. **개발자가 이 산출물만으로 U3를 아키텍처 가이던스 없이 구현 가능함이 확인됨.**

**판정: READY**
