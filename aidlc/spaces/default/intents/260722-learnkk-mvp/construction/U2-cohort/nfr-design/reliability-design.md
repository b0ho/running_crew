# Reliability Design — U2 cohort (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `nfr-requirements/reliability-requirements.md`(원자성·상태전이·안전실패), `functional-design/business-logic-model.md`(W-U2-1 원자 생성·W-U2-5 전이·§8 크로스유닛), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 서버, recreate 배포(U1 가용성 방침 상속).

## 1. 가용성

`reliability-requirements.md` §1: U1과 동일 best-effort(정형 SLA 없음), 재배포 시 짧은 다운타임 허용. HA는 확장 후속.

## 2. 데이터 정합성(핵심)

`reliability-requirements.md` §2의 요구를 트랜잭션 설계로 확정한다.

- **원자 생성(W-U2-1)**: 코호트 개설(Cohort + 회차 N건)은 **단일 `@Transactional`**. 실패 시 전체 롤백 → 부분 생성(코호트만/회차 일부) 방지. INV-U2-2(seq 1..N 연속·유일)는 `(cohort_id, seq)` UNIQUE 제약 + 트랜잭션 원자성으로 이중 보장.
- **상태 전이 안전(W-U2-5) — 동시성 메커니즘 확정**: 모집중→진행중→종료됨 단방향. 역전이·중복 전이는 409 INVALID_STATE_TRANSITION으로 거부(R-U2-11). 전이는 **상태 가드 조건 UPDATE**로 원자적으로 수행한다: `UPDATE cohort SET status = :next WHERE id = :id AND status = :expected`. 영향 행 수가 0이면 전이 조건 불충족(이미 전이됐거나 상태 상이) → 409로 거부. **결정 근거**: (a) 파일럿은 단일 소유 멘토 저작·저동시성이라 전(全) 필드 낙관적 락(`@Version`)의 오버헤드/충돌 노이즈가 불필요하다; (b) 상태 가드 UPDATE는 "전이 불변식"만 정확히 보호하고, 정원/정보 수정 같은 비상태 필드 동시 수정과 불필요하게 충돌하지 않는다. 따라서 **파일럿은 `@Version`을 두지 않고 상태 전이에만 조건 UPDATE 가드를 적용**한다(다중 멘토·고동시성 편집이 생기는 확장 시 `@Version` 재검토).
- **종료됨 전이 경계**: 진행중→종료됨은 U5 오케스트레이션 경로로만 수행(business-logic-model §7). U2는 status 세터(리포지토리)만 제공, 스스로 종료 판정을 하지 않는다 → 판정 로직 단일 소유(U5)로 정합성 사고 방지.

## 3. 결함 허용 & 안전 실패

- **정원 축소 시 U3 의존(R-U2-09) — 실패 매핑 확정**: 정원 축소 시 U2는 U3의 `EnrollmentService.confirmedCount(cohortId)`를 읽는다. **파일럿에서 U2·U3는 동일 배포단위(`learnkk-api`)의 in-process 모듈**이므로 이 호출은 원격 API가 아니라 **동기 in-process 메서드 호출**이다. 따라서:
  - 정상: 확정 인원 `n` 반환 → capacity < n 이면 409 CAPACITY_BELOW_CONFIRMED, capacity ≥ n 이면 허용(§경고는 §아래 참조).
  - 예외(예상치 못한 내부 오류로 `confirmedCount`가 throw): **재시도하지 않고** 축소 트랜잭션 전체를 롤백하여 정원을 변경하지 않은 채 **500 INTERNAL_ERROR**로 안전 실패(공통 에러 핸들러 경유). 근거: 확정 인원을 확인하지 못한 채 정원을 줄이면 확정 참여자 초과 삭감 위험 → 불확실하면 상태 무변경이 안전(`reliability-requirements.md` §2).
  - **상위 requirements의 "503/409" 표현 해소**: 그 표기는 원격 API 장애를 가정한 자리표시였다. 파일럿 in-process 구조에서는 분산 장애(503)가 존재하지 않으므로, 예상치 못한 실패는 500(내부 오류)으로 단일 매핑하고 409는 순수 비즈니스 규칙 위반(capacity < 확정 인원)에만 사용한다. 다중 배포단위로 분리하는 확장 시 원격 호출 장애를 503 + (선택)재시도로 재설계한다.
  - **정원 축소 경고 표현(R-U2-09 허용+경고)**: capacity ≥ 확정 인원이지만 여유가 줄어드는 축소는 거부하지 않고 허용하되, 응답 `CohortDto.warnings: string[]` 필드에 경고 메시지를 담아 반환한다(로그가 아닌 응답 필드로 노출해 FE가 표시). 경고 없으면 빈 배열.
- **크로스유닛 쓰기 경로**: `SessionService.markVerified`(U4 호출), status 세터(U5 호출)는 U2가 서비스 메서드로 캡슐화 제공(business-logic-model §8) — 리포지토리 직접 접근 금지로 상태 전이 규칙을 U2가 일관 강제.

## 4. 데이터 내구성

`reliability-requirements.md` §3: 코호트/회차/공지는 RDB 영속, U1의 일 1회 스냅샷 백업에 포함(별도 백업 대상 아님). Flyway로 스키마 버전 관리(U1 골격 상속).

## 5. 검증(quality)

`reliability-requirements.md` §4:
- 회차 벌크 생성 **원자성**(강제 롤백 시 회차 0건) 통합 테스트(Testcontainers 실 DB).
- 상태 전이 규칙(허용 전이 성공/역전이·중복 거부) 통합 테스트.
- 정원 축소 경계(확정 인원 미만 축소 거부, U3 조회 실패 시 안전 실패) 테스트 — U3 계약을 목/스텁으로.
