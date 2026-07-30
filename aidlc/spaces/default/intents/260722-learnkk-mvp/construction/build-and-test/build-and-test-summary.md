# 빌드·테스트 종합 (build-and-test-summary) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · 리드 QUALITY + DEVSECOPS(보안 테스트) · Test Strategy: **Comprehensive**
> 입력: U1~U6 `code-generation-plan.md`·`code-summary.md`, nfr-requirements/nfr-design(performance·security), memory/{org,team,project}.md
> 실제 실행 결과 상세는 `build-test-results.md`를 참조한다.

## 1. 전체 빌드 상태 및 전제

- **백엔드**(`learnkk-api`, Spring Boot 3.3.5 · JDK 17 · Gradle): `spotlessJavaCheck compileTestJava test -PexcludeIntegration` → **BUILD SUCCESSFUL**, 단위/구조 테스트 **107건 전부 통과**(0 실패). Google Java Format 통과.
- **프론트엔드**(`learnkk-web`, React 18 · TS 5 · Vite 5): `npm test` **94건 통과(25 suites)**, `npm run build` 성공(82 modules, `dist/` 생성), `npm run lint`(`--max-warnings=0`) 통과.
- **전제**: 로컬 **Docker 미가용**(`docker info` 실패) → 통합 테스트(Testcontainers)는 로컬 미실행, **CI 위임**. project.md Testing Posture대로 로컬은 단위 테스트 + (Docker 가용 시) 실 compose 라이브 스모크로 동등 검증한다.
- **적용한 수정: 없음**(모든 명령 1차 통과).

## 2. 생성된 테스트 유형 인벤토리

| 유형 | 도구 | 위치/범위 | 로컬 실행 |
|---|---|---|---|
| 백엔드 단위 | JUnit 5 + Mockito | 서비스 로직·인가·경계·예외 매핑 (18개 클래스) | ✅ 107건 통과 |
| 백엔드 구조 | ArchUnit | 컨트롤러 Entity 미노출(DTO 경계) | ✅ 포함 통과 |
| 백엔드 통합 | JUnit 5 + Testcontainers(PostgreSQL 16) | 마이그레이션·동시성·원자성·N+1·지표 일치 (`@Tag("integration")`) | ⏸ CI 위임 |
| 백엔드 동시성 | Testcontainers + ExecutorService/CountDownLatch | 선착순/정원·상태 전이·승인 경합 | ⏸ CI 위임(U3/U4 code-gen 시 실행 이력) |
| 프론트 단위/행위 | Jest + React Testing Library | 폼 검증·라우팅 가드·페이지네이션·다운로드 | ✅ 94건 통과 |
| 보안(단위) | JUnit 5 | 매직바이트·경로 이탈·외부 링크·사용자 열거 방지·본인 스코프 | ✅ 107건에 포함·통과 |
| 성능(스모크) | curl 단건 latency | 목표 latency 대비 판정 | ⏸ Docker 부재로 미측정(설계 근거 확인) |

## 3. 유닛별 커버리지 기대 (핵심 도메인 80% 목표)

핵심 도메인 로직에 대한 단위 테스트 커버가 각 유닛에 존재함을 확인했다(상세: `unit-test-instructions.md` §4). 정량 라인 커버리지 리포트(JaCoCo)는 파일럿 필수 산출물로 강제하지 않으며, 아래는 규칙 커버 기준 기대치다.

| 유닛 | 핵심 도메인 | 단위 테스트 커버 | 기대 |
|---|---|---|---|
| U1 foundation | 인증·RBAC 시드·파일 저장 | Auth(8)·Seeder(4)·FileStorage(7)·Arch(1) | 핵심 규칙 충족(R-U1-01~27) |
| U2 cohort | 상태 전이·정원/회차·공지 권한 | Cohort(14)·Session(3)·Announcement(5)·URL(3) | 충족(R-U2-01~21) |
| U3 enrollment | 선착순/정원·승인 경합·알림 | Enrollment(7)·Approval(5)·Notification(3) + 동시성(통합) | 충족(INV-U3-1, 동시성은 CI) |
| U4 attendance | 업로드 원자성·매직바이트·인가 | Attendance(10)·FileSignature(7) + 원자성(통합) | 충족(INV-U4-1, 원자성은 CI) |
| U5 completion | 수료/정산 판정·종료 원자성·보상 | Completion(11)·Report(7)·Renderer(2) + 원자성(통합) | 충족(INV-U5-3/5) |
| U6 admin-metrics | 집계 정확·0나눗셈·페이지네이션 | Metrics(5)·History(5) + 실데이터 일치(통합) | 충족(INV-U6-3/4) |

- **DTO·getter/setter·설정·단순 UI 마크업**은 커버리지 요구에서 제외(team.md).
- **동시성·원자성·실데이터 일치**의 최종 검증은 Testcontainers 통합 테스트가 담당하며 CI에서 실행된다(로컬 Docker 부재).

## 4. 준비도 평가 (readiness)

| 축 | 상태 | 근거 |
|---|---|---|
| **build-ready** | ✅ 준비 완료 | 백엔드 BUILD SUCCESSFUL, 프론트 build/lint 통과. 재현 절차·환경 설정 문서화(build-instructions.md) |
| **test-ready** | ✅ (단위) 준비 완료 / ⏸ (통합) CI 조건부 | 백엔드 단위 107건·프론트 94건 로컬 통과. 통합 테스트는 작성·컴파일 완료, Docker 가용 CI에서 실행 필요 |
| **deployment-ready** | 🟡 조건부 | 로컬 스택 기동·라이브 스모크는 Docker 가용 환경 필요. CI에서 통합 테스트 그린 + 라이브 스모크 확인 후 배포 권장. 배포 전략: recreate 재배포(team.md), CI: GitHub Actions |

**종합 판정**: 단위·빌드 게이트 기준으로 **build-ready·(단위)test-ready**. 통합 테스트 그린과 배포 준비 최종 확인은 **Docker 가용 CI에서 완료**해야 한다(파일럿 정책 정합).

## 5. 알려진 제약 (known constraints)

- **로컬 Docker 부재 → 통합 테스트 CI 위임**: 마이그레이션·동시성·원자성·N+1·지표 실데이터 일치의 최종 검증은 로컬에서 수행 불가. GitHub Actions(Docker 가용)에서 전량 실행 필요(각 유닛 code-summary Next Steps, cicd-pipeline.md).
- **성능 라이브 스모크 미측정**: Docker 부재로 실 스택 latency 미측정. 통계 p95/부하는 Operation performance-validation 이관.
- **파일럿 보안 위생 보류**: SAST/DAST/SCA/TLS·바이러스 스캔·rate-limit·상세 감사 로그는 파일럿 보류, 확장 시 도입(security-test-instructions.md §4, `cid:practices-discovery:c3`).
- **통합 스위트 격리 권고**: U2/U3 통합 테스트의 `setUp` 사용자 정리(`userRepository.deleteAll()`) 보강 시 전체 통합 스위트 일괄 실행 안정성 향상(U4 code-summary 관측, CI 도입 권장).
- **정보성 경고(비차단)**: Gradle Deprecation, React Router v7 future flag 경고 — 빌드/테스트 실패와 무관.

## 6. 상위 산출물 참조

- 코드 산출물·테스트 목록: U1~U6 `code-generation/code-summary.md`·`code-generation-plan.md`.
- 실행 결과 상세: `build-test-results.md`. 절차: `build-instructions.md`·`unit-test-instructions.md`·`integration-test-instructions.md`·`performance-test-instructions.md`·`security-test-instructions.md`.
- CI/배포: `U1-foundation/infrastructure-design/cicd-pipeline.md`·`deployment-architecture.md`.
