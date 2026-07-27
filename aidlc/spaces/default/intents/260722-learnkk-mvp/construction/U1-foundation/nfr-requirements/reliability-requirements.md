# Reliability Requirements — U1 foundation (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 quality(신뢰성)·aws-platform(운영)
> 상위 입력: `U1-foundation/functional-design/business-logic-model.md`(시드·에러 처리), `business-rules.md`(R-U1-17~20 공통 에러), `requirements-analysis/requirements.md`(NFR-2 호스팅, NFR-4)
> 전제: 로컬 단일 서버(NFR-2), recreate 재배포(team.md Deployment). 파일럿은 고가용 미요구.

## 1. 가용성 목표 (SLO)
- 파일럿 목표 가용성: 업무시간 기준 best-effort(정형 SLA 없음). 단일 인스턴스이므로 재배포 시 짧은 다운타임 허용(recreate 전략).
- 고가용(HA)·무중단 배포는 확장 후속 과제(blue-green/canary 보류, team.md).
- **명확화(워킹 스켈레톤과의 관계)**: 워킹 스켈레톤 게이트(business-logic-model §9)는 특정 시점에 시스템이 **실행 가능함을 1회 검증**하는 것으로, 지속 가용성 SLA와는 별개다. best-effort 가용성은 이 1회 검증과 모순되지 않는다(검증 시점에 기동해 관통 경로 확인 → 이후 상시 가용 보장은 파일럿에서 요구하지 않음).

## 2. 결함 허용 & 우아한 실패
- 공통 에러 핸들러(@RestControllerAdvice)로 예외를 일관 처리, 내부 상세 비노출(R-U1-19). 미처리 예외는 500으로 정규화.
- 시드 실패(관리자 계정 미생성)는 조용히 넘기지 않고 부팅 중단(R-U1-27) — 관리자 없는 상태 기동 방지.
- 파일 저장 실패·트랜잭션 롤백 시 일관성 우선(상위 유닛 U4/U5 보상 패턴의 기반).

## 3. 데이터 내구성 / 백업·복구
- RDB 영속(계정 데이터). **파일럿 백업 요구(IN scope)**: 로컬 DB 볼륨의 **일 1회 스냅샷**(기본 정성 목표) — code-generation/인프라 단계에서 볼륨 스냅샷 스크립트 또는 `pg_dump` 크론으로 구현.
- **정량 RPO/RTO 목표는 파일럿 범위 외(deferred, owner=Operation 단계 observability-setup/incident-response)**: 구체 RPO(예: ≤24h)·RTO는 그 단계에서 확정한다. 본 파일럿에서는 "일 1회 스냅샷 존재"만 요구하고 수치 SLA는 두지 않는다(추적: 이 항목이 미결이 아니라 명시적 후속 배정임).
- Flyway 마이그레이션으로 스키마 버전 관리, 재현 가능한 초기화(시드 포함, 멱등 R-U1-25).

## 4. 검증 (quality)
- 시드 멱등·부팅 실패 조건, 공통 에러 매핑을 통합 테스트로 검증. 재배포(recreate) 후 인증 플로우 스모크 확인(워킹 스켈레톤 관통 경로).
