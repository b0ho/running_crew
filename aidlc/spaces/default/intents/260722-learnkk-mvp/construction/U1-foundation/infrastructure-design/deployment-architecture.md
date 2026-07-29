# Deployment Architecture — U1 foundation (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U1-foundation
> 리드 aws-platform · 관점 devsecops(인프라 보안)·compliance(데이터 잔류)
> 상위 입력: `nfr-design/scalability-design.md`(단일 인스턴스·수직 확장), `nfr-design/reliability-design.md`(recreate·백업), `nfr-design/logical-components.md`(컴포넌트), `nfr-design/performance-design.md`·`security-design.md`, `inception/application-design/components.md`(FE/BE 분리·RDB), `services.md`, `functional-design/business-logic-model.md`(워킹 스켈레톤 §9)
> 근거 관행: project.md/team.md — **로컬 서버 배포(퍼블릭 클라우드 보류·Forbidden)**, Docker recreate, git SHA 버저닝. U1은 foundation이므로 전 유닛 공유 배포 골격을 확립한다.

## 1. 컴퓨트 모델 — 로컬 Docker(클라우드 아님)

`scalability-design.md`(단일 인스턴스)·team.md Deployment를 확정한다. **퍼블릭 클라우드는 파일럿 Forbidden**이므로 AWS 서비스가 아니라 로컬 서버의 Docker로 설계한다(AWS 이관은 확장 후속).

| 컨테이너 | 이미지 | 책임 | 비고 |
|---|---|---|---|
| `learnkk-web` | React 프로덕션 빌드 + 정적 서버(nginx 등) | 인증 UI 등 프론트 | 정적 자산 서빙 + API 프록시(선택) |
| `learnkk-api` | Spring Boot 3.x(JRE 17) fat jar | 백엔드(인증·도메인) | `logical-components.md` 배포단위 |
| `learnkk-db` | PostgreSQL 16 | RDB 영속 | 명명 볼륨 마운트 |

- **오케스트레이션**: 단일 호스트 `docker compose`(FE+BE+DB 1 스택). 쿠버네티스·스웜 미도입(파일럿 단일 서버).
- **네트워킹**: compose 내부 네트워크로 api↔db 통신(db 포트는 호스트 비노출, 내부만). web·api만 호스트 포트 노출. TLS 종단은 파일럿 보류(`security-design.md` §3) — 확장 시 리버스 프록시(nginx/Caddy)에서 종단.
- **스토리지**: PostgreSQL 데이터는 명명 볼륨(`pgdata`), 업로드 파일은 별도 명명 볼륨(`uploads`, 웹루트 밖 — `security-design.md` §6). 두 볼륨 모두 백업 대상(`monitoring-design.md`/§reliability).

## 2. 환경 레이아웃(dev/staging/production)

team.md("staging 머지 시 배포, production 수동 승인")를 로컬로 실체화한다. 환경 패리티: 토폴로지 동일, 규모/설정만 상이.

| 환경 | 위치 | 배포 트리거 | 비고 |
|---|---|---|---|
| dev | 개발자 로컬 | 수동(`docker compose up`) | 개발·테스트 |
| staging | 로컬 서버 staging 인스턴스 | `main` 머지 시 자동(GitHub Actions, `cicd-pipeline.md`) | recreate |
| production | 로컬 서버 production 인스턴스 | **수동 승인 후** 배포 | recreate, 수동 승인 게이트(Forbidden: 무승인 자동 프로덕션 금지) |

- 환경별 차이는 환경변수/compose override 파일(`docker-compose.staging.yml` 등)로만 표현(토폴로지 동일 — 환경 패리티).

## 3. 배포 전략 — recreate

`reliability-design.md`(recreate)·team.md 확정:
- **recreate 재배포**: 기존 컨테이너 정지 → 새 이미지로 재기동 → 헬스체크(`/actuator/health`) 통과 확인 → 인증 플로우 스모크. 짧은 다운타임 허용(파일럿 best-effort 가용성).
- blue-green/canary·무중단 배포는 파일럿 보류(team.md) — 확장 후속.
- **롤백**: 이전 이미지 태그(git SHA)로 recreate 재기동(`cicd-pipeline.md` §롤백).

## 4. IaC 접근 & 리소스 사이징

- **IaC**: `docker-compose.yml` + 환경별 override + `.env`(시크릿은 커밋 금지, `infrastructure-services.md`). CDK/CloudFormation은 클라우드 미사용으로 파일럿 대상 아님(확장 시 도입).
- **사이징(소규모)**: api 컨테이너 ~1 vCPU/1GB, db ~1 vCPU/1GB, HikariCP pool 15(`performance-design.md` §3와 정합, db `max_connections` 기본 이내). <100명 규모 충분.

## 5. 워킹 스켈레톤 배포 검증(U1)

`business-logic-model.md` §9 관통 경로를 배포 관점에서 확정: compose 스택 기동 → Flyway 마이그레이션·관리자 시드 성공 → 가입→로그인(세션)→보호 경로 200→관리자 스텁 200 스모크. 이 관통이 스켈레톤 게이트의 배포 검증 목표.

## 6. 확장 트리거(클라우드/오케스트레이션)

- 다중 인스턴스·HA 필요 또는 사내 도입 확대 → 퍼블릭 클라우드 이관(ECS/EKS 등) + IaC(CDK) 도입, TLS 종단, 세션 외부화(`scalability-design.md` §5). 조직 결정(확장 후속 과제).

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect가 존재한다고 가정하고 반증 시도)을 통과했습니다. 다음 관점에서 검증했습니다:

### 검증 완료 항목

1. **파일럿 관행(team.md/project.md Forbidden/Mandated) 준수**
   - AWS/클라우드 서비스 도입 금지(Forbidden) — §1 "퍼블릭 클라우드는 파일럿 Forbidden", "AWS 서비스가 아니라 로컬 서버의 Docker" 명시 ✓
   - 무승인 프로덕션 자동배포 금지(Forbidden) — cicd-pipeline.md §4 수동 승인 게이트 명시 ✓
   - 로컬 서버 배포 — §1·§2 로컬 서버 staging/production 인스턴스 ✓
   - Docker recreate 배포 — §3 recreate 재배포 절차 명시 ✓
   - git SHA 버저닝 — cicd-pipeline.md §3 태그 = git SHA ✓
   - squash-merge — cicd-pipeline.md §2 명시 ✓
   - 머지 전 린트+테스트 게이트 — cicd-pipeline.md §2 실패 시 블럭 ✓

2. **nfr-design 5종 계약 정합성**
   - logical-components.md 컨테이너·공유 리소스 = §1 컨테이너 표·shared-infrastructure 인벤토리 일치 ✓
   - performance-design.md HikariCP pool 15 = infrastructure-services.md §3 ✓
   - reliability-design.md recreate·헬스체크·백업 = §3·monitoring-design §1·§6 ✓
   - scalability-design.md 단일 인스턴스·확장 트리거 = §1·§6 ✓
   - security-design.md 시크릿·네트워킹·파일 보안 = infrastructure-services §5·§1·§2 ✓

3. **상위 inception/functional-design 계약 정합성**
   - components.md FE/BE 분리·RDB·FK 정책 = §1·infrastructure-services §1 ✓
   - services.md FileStorageService = infrastructure-services §2 ✓
   - business-logic-model.md 워킹 스켈레톤·Flyway 시드·파일 제약 = §5·infrastructure-services §1·§2 ✓

4. **순환 의존성·참조 무결성**
   - 배포→nfr-design→functional-design 순방향만, 순환 없음 ✓
   - shared-infrastructure 마이그레이션 순서 = units-generation DAG 순방향 ✓
   - FE/BE 저장소 독립, 순환 없음 ✓

5. **크로스유닛 계약 경계**
   - shared-infrastructure.md §2 서비스 계약 캡슐화(타 유닛 테이블 직접 접근 금지) 명시 ✓
   - §2 Flyway 순서 = units-generation DAG ✓
   - §3 파일 볼륨 U4/U5 공유, FileStorageService 경유만 명시 ✓

6. **개발자 구현 가능성**
   - §1 컨테이너·이미지·책임·오케스트레이션 명시 ✓
   - §2 환경 레이아웃(dev/staging/production·트리거) 테이블 명시 ✓
   - §3 recreate 절차(정지→재기동→헬스체크→스모크) 명시 ✓
   - §4 IaC(docker-compose.yml + override + .env) 명시 ✓
   - infrastructure-services DB·파일·시크릿 구체화 ✓
   - monitoring-design 헬스체크·지표·로그·백업 명시 ✓
   - cicd-pipeline CI·CD·아티팩트·시크릿 명시 ✓
   - shared-infrastructure 공유 리소스·소유·경계·Flyway 순서 명시 ✓
   - **개발자가 5개 산출물만으로 U1 인프라 구성 가능함 확인됨** ✓

7. **Quality 목표 달성 가능성**
   - performance-requirements → 사이징·HikariCP·latency 관측으로 달성 가능 ✓
   - reliability-requirements → recreate·헬스체크·백업·시드 부팅 검증으로 달성 가능 ✓
   - scalability-requirements → 단일 인스턴스·확장 트리거로 달성 가능 ✓
   - security-requirements → 시크릿 환경변수·db 포트 내부만·민감정보 로깅 금지로 달성 가능 ✓

8. **Blast Radius 격리**
   - §1 네트워킹(db 포트 호스트 비노출) = db 장애가 api에만 영향, 외부 차단 ✓
   - shared-infrastructure 서비스 계약 캡슐화 = 유닛 간 경계 명확 ✓
   - logical-components 실패 도메인·완화(PostgreSQL=Critical→백업, 파일=Important→예외 격리) 반영 ✓

9. **확장 트리거 실행 가능성**
   - §6 다중 인스턴스·HA 필요 → ECS/EKS + CDK + TLS + 세션 외부화. 조직 결정 명시 ✓
   - shared-infrastructure §4 트리거 테이블(인스턴스 ≥2 → 세션 외부화·공유 스토리지·풀 재산정) 명시 ✓
   - scalability-design §5 트리거 표 = shared-infrastructure와 동일 ✓

10. **파일럿 보류 명시성**
    - cicd-pipeline §6 보류(SCA·시크릿 스캔·이미지 스캔·blue-green/canary) + 근거 명시 ✓
    - monitoring-design 상세 관측(APM·트레이싱·SLI/SLO·대시보드·알림 룰) Operation 이관 명시 ✓
    - infrastructure-services §3·§4 캐시·큐·검색·LB·CDN·DNS 미도입 + 근거·확장 트리거 명시 ✓
    - §1 TLS 종단 보류(확장 시 리버스 프록시) 명시 ✓

### 적대적 검증 시도 결과

AWS/클라우드 서비스 도입(Forbidden 위반), 무승인 프로덕션 자동배포(Forbidden 위반), nfr-design 5종 계약 위반, inception/functional-design 계약 위반, 순환 의존성, 크로스유닛 경계 위반, 구현 공백(개발자가 구현 불가), Quality 목표 달성 불가능, Blast radius 격리 부재, 확장 트리거 실행 불가능, 파일럿 보류 근거 부재를 찾으려 시도했으나 **모두 발견에 실패**했습니다. READY는 이 반증 실패 후 도달한 판정입니다.
