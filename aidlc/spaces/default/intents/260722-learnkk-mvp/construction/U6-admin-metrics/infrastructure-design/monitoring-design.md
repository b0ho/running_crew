# Monitoring Design — U6 admin-metrics (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/reliability-design.md`·`performance-design.md`·`security-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 모니터링 골격 상속. 상세 관측은 Operation observability-setup.

## 1. 헬스·지표(U1 상속 + U6 관측)

U1 `/actuator/health`·Micrometer 공유. U6 고유 관측: 지표 집계(overview) 응답시간(`performance-design.md` §1 ≤500ms 대비), 이력 조회 시간(≤350ms). 집계 응답 지속 초과는 캐시 도입 확장 신호(`scalability-design.md` §3).

## 2. 로그

- 관리자 인가 실패(비관리자 403·미인증 401)·집계 조회 실패(500)는 애플리케이션 로그. **U6은 읽기 전용이라 데이터 변조 로그 없음**(INV-U6-1).
- 이력 뷰는 PII(성명)를 다루므로 접근 로그는 남기되 PII 원문 로깅 최소화(compliance).

## 3. 백업

U6은 자체 영속 데이터 없음(읽기 전용) → 별도 백업 대상 없음. 소스 데이터는 U1 백업 정책(DB + uploads 볼륨)에 포함(`reliability-design.md` §3).

## 4. 알림·SLO

파일럿 정형 알림 미설정(U1 상속). 지표 응답시간·집계 비용은 확장 시점(캐시 도입) 판단 지표로 Operation observability-setup에서 관측 승격 권장.
