# Scalability Requirements — U5 completion (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U5-completion
> 리드 architect · 관점 aws-platform·quality
> 상위 입력: `U5-completion/functional-design/business-logic-model.md`, `business-rules.md`, `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 1. 데이터·처리 규모
- 코호트당 확정 멘티 수십, 종료 시 증서 N장·정산 1건. 데이터 소량. 보고서·수료증 파일은 로컬 볼륨(U4와 동일 스토리지 특성).
- 종료 트랜잭션의 작업량은 확정 멘티 수 N에 선형.

## 2. 확장 전략
- **파일럿**: 종료 트랜잭션 내 순차 증서 발급(수십 규모 수용). 단일 인스턴스.
- **확장 시**: 대규모 코호트(수백+ 멘티) 종료의 긴 트랜잭션을 피하려면 증서 발급을 **비동기 배치**로 분리(종료 판정은 즉시 커밋, 증서 생성은 후속 잡)하는 전환 검토. 파일 스토리지는 U4와 동일하게 오브젝트 스토리지 이관(FileStorageService 백엔드 교체).
- U5 자체는 인스턴스 로컬 상태 없음(DB·파일 기반) → 다중 인스턴스 안전(세션은 U1 트리거).

## 3. 확장 트리거
- 코호트당 멘티 수백+ 또는 종료 시간이 요청 타임아웃 접근 시: 증서 발급 비동기화. 파일럿 규모(<100명, 코호트당 수십)에서는 동기 발급으로 충분.
