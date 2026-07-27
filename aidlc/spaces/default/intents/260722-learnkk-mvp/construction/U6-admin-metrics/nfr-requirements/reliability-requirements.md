# Reliability Requirements — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `U6-admin-metrics/functional-design/business-logic-model.md`(집계 §2), `business-rules.md`(R-U6-04/05 분모0·INV-U6-*), `requirements-analysis/requirements.md`(FR-11, NFR-2/4)
> 전제: 로컬 단일 서버(U1 상속). U6은 읽기 전용.

## 1. 정합성 (지표 정확성 — FR-11)
- **지표는 실제 데이터와 일치**(FR-11): 캐시 없이 조회 시점 실시간 계산(INV-U6-2) → 드리프트 없음.
- **0 나눗셈 안전 처리(INV-U6-3)**: 출석률·수료율 분모 0이면 0%로 표시(예외 없음, R-U6-04/05).
- **집계 범위 일관(INV-U6-4)**: 출석률·수료율은 종료됨 코호트 기준으로 일관 계산.

## 2. 결함 허용
- 읽기 전용이라 데이터 변조 위험 없음. 조회 실패(DB 오류)는 500으로 정규화(U1 공통 핸들러), 부분 결과 반환 금지(일관된 지표만 노출).
- 소스 데이터 일시 부정합(예: 종료 진행 중 조회)이 있어도 트랜잭션 격리로 커밋된 상태만 읽음(READ_COMMITTED).

## 3. 가용성 & 내구성
- U1 best-effort 가용성 상속. U6은 자체 영속 데이터가 없으므로 별도 백업 대상 없음(소스 유닛 데이터가 U1 백업 정책에 포함).

## 4. 검증
- 지표 산식 정확성(작은 고정 데이터셋으로 완주 수·출석률·수료율·증서 수 기대값 단언), 분모 0 안전 처리, 이력 분리 조회(증빙/보고서), 관리자 인가(비관리자 403)를 통합 테스트로 검증.
