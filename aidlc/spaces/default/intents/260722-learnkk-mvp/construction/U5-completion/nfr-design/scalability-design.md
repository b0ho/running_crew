# Scalability Design — U5 completion (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U5-completion
> 리드 architect · 서포트 aws-platform·quality
> 상위 입력: `nfr-requirements/scalability-requirements.md`(규모·확장 전략·트리거), `functional-design/business-logic-model.md`(종료 트랜잭션 N 선형), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 1. 확장 아키텍처(파일럿)

`scalability-requirements.md` §1~2:
- 파일럿은 단일 인스턴스·수직 확장만(U1 상속).
- 코호트당 확정 멘티 수십, 종료 시 증서 N장·정산 1건. 데이터 소량. 보고서·수료증 파일은 로컬 볼륨(U4와 동일 스토리지 특성).
- **종료 트랜잭션 작업량은 확정 멘티 수 N에 선형**(증서 생성·저장·알림).

## 2. 확장 전략 & 다중 인스턴스 안전성

`scalability-requirements.md` §2:
- **파일럿**: 종료 트랜잭션 내 증서 **순차 발급**(수십 규모 수용). 
- **긴 트랜잭션 리스크 & 전환 경로**: 대규모 코호트(수백+ 멘티) 종료는 트랜잭션 보유 시간이 길어져 락·타임아웃 리스크가 커진다. 확장 시 **증서 발급을 비동기 배치로 분리**한다: 종료 판정·상태 전이·정산은 즉시 커밋하고, 증서 생성은 후속 잡(큐)에서 멱등 발급(UNIQUE로 재발급 방지)한다. 이 분리는 파일럿 스코프아웃이며 `performance-design.md` §4 트리거와 연동.
- **U5 자체 무상태**: 인스턴스 로컬 상태 없음(DB·파일 기반) → 다중 인스턴스 안전. 파일 스토리지는 U4와 동일하게 오브젝트 스토리지 이관 시 FileStorageService 백엔드 교체(인터페이스 불변). 세션 제약만 U1 트리거.

## 3. 데이터 확장

- FinalReport/Certificate/SettlementStatus 데이터 소량. 보고서 이력 조회는 페이지네이션(`performance-design.md` §3).
- 수료증·보고서 파일 볼륨은 U4와 동일 스토리지 특성(파일럿 로컬, 확장 오브젝트 스토리지).

## 4. 확장 트리거

`scalability-requirements.md` §3:
- 코호트당 멘티 **수백+** 또는 종료 시간이 요청 타임아웃 접근 시 → 증서 발급 비동기화(§2). 파일럿 규모(코호트당 수십)에서는 동기 발급 충분.
- 파일 볼륨 용량 임계 또는 다중 인스턴스 필요 시 → 오브젝트 스토리지 이관(U4·U1 트리거와 공통).
