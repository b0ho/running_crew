# Monitoring Design — U1 foundation (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U1-foundation
> 리드 aws-platform · 관점 devsecops(보안 로깅)·compliance
> 상위 입력: `nfr-design/reliability-design.md`(헬스체크·백업), `nfr-design/performance-design.md`(목표 latency), `nfr-design/security-design.md`(로깅·감사 보류), `logical-components.md`(실패 도메인), `inception/application-design/components.md`, `services.md`, `functional-design/business-logic-model.md`
> 전제: 로컬 Docker 파일럿. 상세 관측(APM·분산 트레이싱)은 Operation observability-setup에서 구체화 — 본 단계는 설계 수준.

## 1. 헬스체크 & 가용성 모니터링

`reliability-design.md` §4:
- **헬스체크 엔드포인트**: Spring Boot Actuator `/actuator/health`(permitAll). 얕은 체크(프로세스) + DB 커넥션 확인(deep). Docker `healthcheck`로 컨테이너 상태 판정(recreate 배포 시 기동 확인에 사용, `deployment-architecture.md` §3).
- **가용성**: 파일럿 best-effort(정형 SLA 없음). 업타임 상시 모니터링·알림은 확장 후속(Operation).

## 2. 메트릭 수집

- **애플리케이션 메트릭**: Actuator + Micrometer로 JVM·요청 지표 노출(`/actuator/metrics`). 파일럿은 노출 수준까지 확립하고, 수집·시각화(Prometheus/Grafana)는 Operation observability-setup에서 구체화.
- **성능 관측 지표**: 인증 경로 응답시간(`performance-requirements`/`performance-design` 목표 대비), 커넥션 풀 사용률(HikariCP), DB 커넥션 수.

## 3. 로그 전략

`security-design.md`(로깅) + construction 가드레일:
- **애플리케이션 로그**: 표준 출력(stdout)으로 구조적 로그(JSON 권장), Docker 로그 드라이버가 수집. 프레임워크 기본 액세스 로그 수준(부인방지용 상세 감사 로그는 파일럿 보류).
- **에러 로그**: 공통 에러 핸들러가 500 등 내부 오류 로깅(내부 상세 비노출은 응답에만, 로그엔 상세). **고아 파일 보상 실패**는 고정 토큰 `ORPHAN_FILE_COMPENSATION_FAILED`로 로깅(U4/U5 — grep 수동 정리).
- **민감정보 로깅 금지**: 비밀번호·passwordHash·시크릿·PII 원문을 로그에 남기지 않는다(devsecops).
- **로그 보존/집계**: 중앙 로그 집계(ELK 등)는 확장 후속. 파일럿은 Docker 로그 + 필요 시 파일.

## 4. 알림(Alerting)

- 파일럿은 정형 알림 룰 미설정(단일 서버·수동 운영). 배포 실패·헬스체크 실패는 배포 스크립트/CI 로그로 확인.
- 알림 룰·온콜·에스컬레이션은 Operation incident-response/observability-setup에서 확정.

## 5. 대시보드 & SLI/SLO

- SLI/SLO 정형화·대시보드는 Operation observability-setup으로 이관(`reliability-design.md` §1의 정량 RPO/RTO 이관과 정합). 파일럿은 헬스체크·기본 지표 노출까지.

## 6. 백업 모니터링

`reliability-design.md` §3:
- **일 1회 스냅샷**(DB 볼륨 + uploads 볼륨) 크론 잡의 성공/실패를 로그로 확인. 백업 파일 존재·크기 점검(수동). 정량 RPO/RTO는 Operation에서 확정.
