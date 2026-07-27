# Scalability Requirements — U2 cohort (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 aws-platform
> 상위 입력: `U2-cohort/functional-design/business-logic-model.md`, `business-rules.md`, `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 인스턴스, <100명(U1 확장 방침 상속).

## 1. 데이터 규모 & 성장
- 코호트 수십~수백 건, 코호트당 회차 수~수십, 공지 소량. 성장 완만(사내 파일럿).
- 목록 조회는 페이지네이션(20건)으로 데이터 증가에 견딤.

## 2. 확장 전략
- 파일럿: 수직 확장만(U1 상속). U2는 상태 비저장 조회/CRUD라 인스턴스 로컬 상태 없음 → 다중 인스턴스 확장 시에도 U2 자체는 세션 외 추가 제약 없음(세션은 U1 확장 트리거 참조).
- 대량 목록 대비 인덱스(status, mentorId, createdAt) 사전 설계.

## 3. 확장 트리거
- 코호트/회차 데이터가 페이지네이션·인덱스로 감당 불가한 규모(수만 건+)로 성장 시 검색 최적화(전문 검색·캐시) 검토 — 파일럿 규모에서는 불필요.
