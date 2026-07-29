# Monitoring Design — U5 completion (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U5-completion
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/reliability-design.md`·`performance-design.md`·`security-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 모니터링 골격 상속. 상세 관측은 Operation observability-setup.

## 1. 헬스·지표(U1 상속 + U5 관측)

U1 `/actuator/health`·Micrometer 공유. U5 고유 관측: 종료 트랜잭션 시간(확정 멘티 N 선형, `performance-design.md` §1 ≤3s 대비), 수료증 생성 시간, 보고서 제출 시간.

## 2. 로그(다건 보상 핵심)

- **수료증 다건 보상 실패**: 고정 토큰 `ORPHAN_FILE_COMPENSATION_FAILED path={} error={}`로 각 실패 imagePath 로깅(`reliability-design.md` §2) — grep 수동 정리.
- 종료 소유권 위반(403)·상태 위반(409)·totalSessions==0 정합오류(500)는 애플리케이션 로그. PII·판정 원문 최소.

## 3. 백업(파일 볼륨 포함)

`reliability-design.md` §5: 보고서 첨부·수료증 이미지는 uploads 볼륨에 있어 DB와 함께 일 1회 스냅샷 대상(증서·보고서 유실 방지).

## 4. 알림·SLO

파일럿 정형 알림 미설정(U1 상속). 종료 시간이 목표(≤3s) 초과 추세면 증서 비동기화 확장 트리거 신호 — Operation observability-setup에서 지표 승격 권장.
