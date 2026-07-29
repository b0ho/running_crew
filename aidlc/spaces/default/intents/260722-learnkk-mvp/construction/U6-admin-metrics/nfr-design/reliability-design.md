# Reliability Design — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `nfr-requirements/reliability-requirements.md`(지표 정확성·0 나눗셈·범위 일관), `functional-design/business-logic-model.md`(W-U6-1 산식 §2), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 서버(U1 가용성 상속). U6은 읽기 전용.

## 1. 지표 정확성(핵심 — FR-11)

`reliability-requirements.md` §1:
- **실시간 계산으로 드리프트 제거(INV-U6-2)**: 캐시 없이 조회 시점 소스 데이터에서 계산 → 지표가 항상 실제 데이터와 일치(FR-11). 파일럿에서 실시간이 정확성·단순성 모두 우수.
- **0 나눗셈 안전 처리(INV-U6-3)**: 출석률·수료율 분모가 0이면 예외 없이 **0%로 표시**(R-U6-04/05). 집계 쿼리에서 `COALESCE`/분모 0 가드를 적용하고, 애플리케이션에서도 분모 0 시 0 반환.
- **집계 범위 일관(INV-U6-4)**: 출석률·수료율·완주 수는 **종료됨(CLOSED) 코호트** 기준으로 일관 계산. 진행중/모집중 코호트는 집계에서 제외하여 지표 의미를 명확히.
- **백분율 산술**: 내부 계산은 정수/정밀 산술로, 표시단에서만 반올림(내부 판정 미사용, `tech-stack-decisions.md`).

## 2. 결함 허용

`reliability-requirements.md` §2:
- 읽기 전용이라 데이터 변조 위험 없음. 조회 실패(DB 오류)는 500으로 정규화(U1 공통 핸들러). **부분 결과 반환 금지** — 일관된 지표 세트만 노출(일부 집계 실패 시 전체 실패).
- **소스 데이터 일시 부정합 방지**: 종료 진행 중인 코호트를 조회해도 트랜잭션 격리(READ_COMMITTED)로 **커밋된 상태만** 읽는다. 지표 계산은 `@Transactional(readOnly=true)` 스냅샷 내에서 일관 조회.

## 3. 가용성 & 내구성

`reliability-requirements.md` §3:
- U1 best-effort 가용성 상속.
- **U6은 자체 영속 데이터가 없다**(읽기 전용) → 별도 백업 대상 없음. 소스 유닛 데이터가 U1 백업 정책(DB + 파일 볼륨)에 이미 포함.

## 4. 검증(quality)

`reliability-requirements.md` §4:
- **지표 산식 정확성**: 작은 고정 데이터셋으로 완주 수·출석률·수료율·증서 수 기대값 단언(Testcontainers).
- **분모 0 안전 처리**: 종료 코호트 0건 / 확정 멘티 0명 시 0% 반환 테스트.
- **이력 분리 조회**: 증빙 이력과 보고서 이력이 별도 뷰로 반환되는지 검증.
- **관리자 인가**: 비관리자 403·미인증 401 테스트.
