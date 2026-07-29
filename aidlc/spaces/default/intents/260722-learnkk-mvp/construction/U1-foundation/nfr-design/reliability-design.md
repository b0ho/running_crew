# Reliability Design — U1 foundation (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 quality(신뢰성)·aws-platform(운영)
> 상위 입력: `nfr-requirements/reliability-requirements.md`(가용성·결함허용·백업), `nfr-requirements/tech-stack-decisions.md`(Flyway·recreate 배포), `functional-design/business-logic-model.md`(공통 에러 W2·시드 W6·파일 보상 W7)
> 전제: 로컬 단일 서버, recreate 재배포. 파일럿은 고가용(HA) 미요구.

## 1. 가용성 설계(SLO)

`reliability-requirements.md` §1 기준:
- **목표**: 업무시간 best-effort(정형 SLA 없음). 단일 인스턴스·recreate 재배포로 배포 시 짧은 다운타임 허용.
- **HA·무중단 배포**(blue-green/canary)는 확장 후속 과제 — 파일럿 스코프아웃.
- **워킹 스켈레톤 게이트와의 관계**: 스켈레톤 관통 검증(business-logic-model §9)은 특정 시점 실행가능성의 1회 검증이며 상시 가용 SLA와 별개(`reliability-requirements.md` §1 명확화 정합).

## 2. 결함 허용 & 우아한 실패(Graceful Degradation)

`nfr-design-guide.md`의 실패 모드 체크리스트를 U1에 적용:

- **공통 에러 핸들러**: `@RestControllerAdvice`가 모든 예외를 공통 에러 DTO(`code,message,timestamp,path`)로 정규화. 미처리 예외는 500으로, 내부 상세는 비노출(R-U1-19). 예외→HTTP 매핑 전체 표는 business-rules §4.1에 정의됨.
- **시드 실패 처리**: 관리자 계정 미생성(환경변수 미설정)은 **조용히 넘기지 않고 부팅 중단**(R-U1-27) — fail-fast로 "관리자 없는 반쪽 기동"을 방지.
- **파일 저장 실패 보상**: 파일+DB 결합 작업에서 트랜잭션 롤백 시 `FileStorageService.delete`(멱등)로 고아 파일 제거. U1이 이 보상 훅을 제공하고 U4/U5가 사용.
- **의존성 실패 등급(우아한 저하 티어)**:

| 의존성 | 티어 | U1 실패 대응 |
|---|---|---|
| PostgreSQL | Critical(핵심 경로) | 인증 불가 → 500/503, 재기동으로 복구. 파일럿은 자동 페일오버 없음 |
| 파일 볼륨 | Important | 파일 저장 실패는 예외 → 해당 요청 실패, 나머지 기능 지속 |
| springdoc UI | Nice to have | 장애 시 문서만 불가, 서비스 영향 없음 |

- **서킷 브레이커·재시도·벌크헤드 미도입**: U1은 외부 서비스 호출이 없고(로컬 DB만), 단일 인스턴스라 격리 대상이 없음. 이들 패턴은 파일럿 스코프아웃(외부 연동 도입 시 재검토).

## 3. 데이터 내구성 / 백업·복구

`reliability-requirements.md` §3 기준:
- **RDB 영속**: 계정 데이터는 PostgreSQL에 영속.
- **백업(IN scope)**: 로컬 DB 볼륨 **일 1회 스냅샷** — `pg_dump` 크론 또는 볼륨 스냅샷 스크립트로 구현(code-generation/infrastructure-design 단계에서 구체화). 백업 파일은 별도 위치 보관.
- **정량 RPO/RTO는 파일럿 범위 외(명시적 후속 배정, owner=Operation observability-setup/incident-response)**. 파일럿은 "일 1회 스냅샷 존재"만 요구, 수치 SLA 미설정.
- **Flyway 스키마 버전 관리**: 마이그레이션으로 재현 가능한 초기화(시드 포함, 멱등 R-U1-25). 롤백은 파일럿에서 forward-fix 원칙(down 마이그레이션 미강제).

## 4. 헬스체크 & 복구 절차

- **헬스체크 엔드포인트**: Spring Boot Actuator `/actuator/health`(permitAll). 얕은 체크(프로세스 생존) + DB 커넥션 확인(deep) 분리. 상세 지표 수집·모니터링은 Operation 단계 observability-setup에서 구체화(`memory.md` Open questions 정합).
- **재배포 복구(recreate)**: 기존 컨테이너 정지 → 새 이미지 기동 → 헬스체크 통과 확인 → 인증 플로우 스모크(가입→로그인→보호 경로 200). team.md Deployment 정합.
- **부팅 검증**: 기동 시 Flyway 마이그레이션·시드 성공, 필수 환경변수(관리자 계정) 존재를 확인. 실패 시 부팅 중단(§2).

## 5. 검증(quality)

`reliability-requirements.md` §4 기준:
- 시드 **멱등성**(재실행 시 no-op)과 **부팅 실패 조건**(환경변수 미설정 → 기동 중단)을 통합 테스트로 검증(Testcontainers 실 DB).
- 공통 에러 매핑(예외→HTTP)을 통합 테스트로 검증.
- 재배포(recreate) 후 인증 플로우 스모크로 관통 경로 확인(워킹 스켈레톤).
