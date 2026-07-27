# Reliability Requirements — U2 cohort (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `U2-cohort/functional-design/business-logic-model.md`(원자 생성·상태전이), `business-rules.md`(R-U2-11 전이·INV-U2-*), `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 서버, recreate 배포(U1 가용성 방침 상속).

## 1. 가용성
- U1과 동일 best-effort(정형 SLA 없음), 재배포 시 짧은 다운타임 허용. HA 확장 후속.

## 2. 데이터 정합성 & 결함 허용
- **원자성**: 코호트 개설(Cohort + 회차 N건)은 단일 트랜잭션 — 부분 생성(코호트만/회차 일부) 방지(INV-U2-2: seq 1..N 연속·유일).
- **상태 전이 안전**: 모집중→진행중→종료됨 단방향, 역전이·중복 전이는 409로 거부(R-U2-11). 종료됨 전이는 U5 소유 경로로만.
- 정원 축소 검증(R-U2-09)은 U3 confirmedCount 읽기 실패 시 안전 실패(요청 거부, U3 API 장애 시 503/409 — 재시도 없음, 파일럿 단순 실패).

## 3. 데이터 내구성
- 코호트/회차/공지는 RDB 영속. U1의 일 1회 스냅샷 백업 정책에 포함(별도 백업 대상 아님).

## 4. 검증
- 회차 벌크 생성 원자성(롤백 시 회차 0건), 상태 전이 규칙(허용/거부), 정원 축소 경계를 통합 테스트로 검증(Testcontainers).
