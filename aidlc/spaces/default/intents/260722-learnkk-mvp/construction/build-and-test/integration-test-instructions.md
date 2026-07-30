# 통합 테스트 지침 (integration-test-instructions) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · 리드 QUALITY · 도구: **JUnit 5 + Testcontainers(PostgreSQL 16)**
> 입력: U1~U6 `code-summary.md`(통합 테스트 목록), team.md/project.md(Testing Posture), `nfr-design/reliability-design.md`(상태 전이 동시성)
> **핵심 전제: 통합 테스트는 Docker를 요구한다. 본 검증 머신은 Docker 미가용(`docker info` 실패) → 로컬 미실행, CI 위임.**

## 1. 통합 테스트 대상 (무엇을 검증하는가)

단위 테스트(목킹)로는 재현할 수 없는 **실 DB 트랜잭션·락 경합·마이그레이션·N+1·원자성**을 실 PostgreSQL 16에 대해 검증한다.

| 검증 축 | 대상 | 소유 유닛 |
|---|---|---|
| 동시성(선착순/정원) | capacity=5에 20 동시 join → CONFIRMED==5·WAITING==15·중복 0; capacity=1에 10 동시 → CONFIRMED==1; 동일 멘티 8 동시 이중 제출 → UNIQUE로 1건만 | U3 |
| 관리자 승인 경합 | 두 관리자 동시 approve → 조건부 UPDATE로 1건만 성공·알림 1건 | U3 |
| 상태 전이 원자성 | 8스레드 동시 `start`(RECRUITING→ONGOING) → 가드 UPDATE로 정확히 1건 성공 | U2 |
| 종료 원자성 | 수료증 다건 insert + 정산 upsert + 상태 전이 + 알림이 단일 트랜잭션으로 커밋/롤백; 롤백 시 누적 이미지 전부 보상 삭제 | U5 |
| 업로드 원자성 | 증빙 저장 + 회차 인증(예정→인증) 동일 트랜잭션; 롤백 시 파일 보상 삭제·회차 미인증·이력 0 | U4 |
| 지표 실데이터 일치 | CLOSED 코호트만 집계(ONGOING 제외)·출석률/수료율 실데이터 매칭·전체 0 안전 | U6 |
| 마이그레이션·N+1 | Flyway V1~V6 적용, 상세 조회 쿼리 카운트 상한(≤4, 회차 수 무관) | U1·U2 |

### 통합 테스트 클래스 인벤토리 (각 유닛 code-summary 근거)

- U1: `auth.AuthIntegrationTest`(8)
- U2: `cohort.CohortIntegrationTest`(5 — 개설·페이지네이션·N+1 회귀·상태 전이 순차/동시)
- U3: `enrollment.EnrollmentConcurrencyIntegrationTest`(3), `enrollment.EnrollmentIntegrationTest`(4)
- U4: `attendance.AttendanceIntegrationTest`(6), `attendance.AttendanceCompensationIntegrationTest`(1, `@MockBean` writer)
- U5: `completion.CompletionIntegrationTest`, `completion.CompletionRollbackCompensationIntegrationTest`
- U6: `metrics.MetricsIntegrationTest`(2), `metrics.HistoryIntegrationTest`(5)

모두 `support/IntegrationTestBase`(Testcontainers `postgres:16`)를 상속하고 `@Tag("integration")`로 표시된다.

## 2. 실행 전제 및 필터 (`@Tag("integration")`)

- **전제**: Docker 데몬 가용(`docker info` 성공). Testcontainers가 임시 PostgreSQL 16 컨테이너를 기동한다.
- **필터 규약**(`cid:code-generation:c1`): 통합 테스트는 `@Tag("integration")`으로 분리되어 있어 로컬에서는 `-PexcludeIntegration`로 배제한다. Docker 가용 환경에서는 플래그 없이 전량 실행한다.

```bash
# CI(Docker 가용): 통합 포함 전량 실행
cd learnkk-api && ./gradlew test        # 또는 ./gradlew build

# 특정 통합 클래스만(Docker 가용 로컬)
./gradlew test --tests "com.learnkk.enrollment.EnrollmentConcurrencyIntegrationTest"
```

> 참고(U3/U4 code-summary): Rancher Desktop 등 일부 소켓 환경에서 여러 통합 클래스를 한 Gradle 실행으로 돌리면 컨테이너 생명주기 타이밍/선행 테스트 격리(`userRepository` 미정리) 이슈로 간헐 실패가 관측됐다. **클래스별 개별 실행 시 각각 그린**이며 표준 CI에서는 정상이다(코드 결함 아님). CI 도입 시 테스트 격리(`setUp`의 `userRepository.deleteAll()`) 보강을 권장한다.

## 3. 실 compose 스택 라이브 스모크 대안 (로컬 Docker 부재 시 동등 검증)

project.md Testing Posture: 로컬 Docker 부재 시 **단위 테스트 + 실 compose 스택 라이브 스모크**로 동등 검증한다. U1 워킹 스켈레톤에서 이 대안이 실제로 수행되어 관통(가입→로그인→/me→관리자 스텁)·401/403/400/409·시드 멱등·fail-fast를 실 PostgreSQL 16에서 확인한 이력이 있다(U1 `code-summary.md` §3).

Docker가 가용한 환경에서 라이브 스모크로 핵심 플로우를 재현하는 절차:

```bash
cp .env.example .env   # DB_PASSWORD·ADMIN_EMAIL·ADMIN_PASSWORD 채움
docker compose up -d --build
# 1) 인증 관통: POST /api/auth/signup → 201, POST /api/auth/login → 200 + JSESSIONID
# 2) 코호트: 개설 → 선착순 참여 → 회차 증빙 업로드 → 진도 조회
# 3) 종료: POST /api/cohorts/{id}/end → 수료/정산 요약, 수료증 다운로드
# 4) 관리자: GET /api/admin/metrics(관리자 세션 200 / 일반 403 / 미인증 401)
docker compose down -v
```

- 라이브 스모크는 통합 테스트가 검증하려는 **동일 시나리오**(관통·권한·에러 매핑·시드)를 실 DB 스택에서 대체 확인한다.
- 단, 진성 **동시성**(선착순 경합·상태 전이 경합)은 라이브 스모크로 재현이 어렵다. 이 부분은 U3/U2 code-generation 단계에서 이미 Testcontainers로 검증된 이력이 있으며, CI에서 통합 테스트로 재확인한다.

## 4. CI 위임 사유 및 후속 (honest note)

- **본 스테이지 로컬 실행 결과**: 통합 테스트 **미실행**(Docker 미가용). build-test-results.md에 "CI 위임(로컬 Docker 미가용)"으로 기록.
- 통합 테스트 코드는 전 유닛에서 작성·컴파일 완료 상태이며, GitHub Actions(Docker 가용)에서 `-PexcludeIntegration` 없이 전량 실행해 마이그레이션·동시성·원자성·N+1·지표 일치를 확인한다(각 유닛 code-summary Next Steps).
- CI 파이프라인 설계: `U1-foundation/infrastructure-design/cicd-pipeline.md`.

## 5. 상위 산출물 참조

- 통합 테스트 상세 시나리오·단언: U2~U6 `code-summary.md` §테스트.
- 동시성 메커니즘(비관적 락·가드 UPDATE·UNIQUE): `nfr-design/reliability-design.md`, U3 `code-summary.md` Key Decisions.
- 원자성 writer 분리 패턴: U4/U5 `code-summary.md` 핵심 결정.
