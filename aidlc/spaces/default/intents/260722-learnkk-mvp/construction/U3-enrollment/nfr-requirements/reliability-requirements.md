# Reliability Requirements — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 관점 quality·devsecops
> 상위 입력: `U3-enrollment/functional-design/business-logic-model.md`(join·동시성 §2), `business-rules.md`(R-U3-03/07~10, INV-U3-*), `requirements-analysis/requirements.md`(FR-3, NFR-6)
> 전제: 로컬 단일 서버(U1 가용성 상속). U3는 최대 정합성 리스크 유닛.

## 1. 정합성 (핵심 — FR-3 보장)
- **불변식(INV-U3-1)**: 자동 확정 경로로 정원 초과 확정이 절대 발생하지 않는다. 비관적 락(R-U3-07)이 "정원 확인→확정"을 코호트 단위로 직렬화하고, UNIQUE(cohortId,menteeId)(R-U3-08)가 최종 방어선.
- **검증(신뢰성 핵심)**: N+k 동시 join → CONFIRMED == min(N, 요청수), 초과분 WAITING, 중복 0. ExecutorService+CountDownLatch+Testcontainers 실 DB로 정원 경계(N/N+1, N+5) 검증(R-U3-10). 이 테스트는 U3 신뢰성의 게이팅 조건.
- 관리자 동시 승인 경합은 @Version/조건부 UPDATE로 방지 → 알림 중복 생성 없음(1건만).

## 2. 결함 허용
- join 트랜잭션 실패 시 롤백(부분 확정 없음). 락 타임아웃/데드락은 트랜잭션 롤백 후 오류 응답(파일럿: 재시도 없음, 클라이언트 재요청).
- 알림 생성 실패가 join 확정을 롤백시키지 않도록 경계 설계(확정은 커밋, 알림은 best-effort 후속 — 단, 파일럿에서는 동일 트랜잭션 내 생성으로 단순화하고 실패 시 함께 롤백해도 무방; 선택은 code-gen에서, 기본은 확정 우선).

## 3. 가용성 & 내구성
- U1 best-effort 가용성 상속. Enrollment/Notification은 RDB 영속, U1 일 1회 스냅샷 백업 포함.

## 4. 검증 요약
- 동시성 정원 경계(필수), 중복 신청 차단(UNIQUE), 관리자 승인 멱등(@Version), 상태 전이(WAITING→CONFIRMED/REJECTED만) 통합 테스트.
