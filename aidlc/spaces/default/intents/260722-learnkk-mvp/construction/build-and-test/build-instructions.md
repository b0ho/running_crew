# 빌드 지침 (build-instructions) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · 리드 QUALITY · 입력: U1~U6 `code-generation-plan.md`·`code-summary.md`, 루트 `docker-compose.yml`·`.env.example`
> 저장소 토폴로지: FE(`learnkk-web`)/BE(`learnkk-api`) 분리(team.md). 애플리케이션 코드는 워크스페이스 루트에 위치.
> 모든 산문은 한글(team.md Mandated). 명령·경로·식별자는 원문 유지.

## 1. 사전 요구 도구 (prerequisites)

| 도구 | 용도 | 비고 |
|---|---|---|
| JDK 17 | 백엔드 컴파일·실행(Spring Boot 3.3.5) | `learnkk-api`는 Gradle Wrapper(`./gradlew`) 동봉 — 별도 Gradle 설치 불필요 |
| Node.js 18+ (LTS) | 프론트 빌드/테스트(Vite 5·React 18·TS 5) | `learnkk-web/package.json` 기준. 로컬 검증은 Node 24에서 수행 |
| Docker + docker compose | 로컬 스택 기동·**통합 테스트(Testcontainers)** | **본 검증 머신에는 미가용**(`docker info` 실패) → 통합 테스트는 CI 위임(§5, integration-test-instructions.md 참조) |
| PostgreSQL 16 | 애플리케이션 DB | 로컬은 compose의 `learnkk-db`(postgres:16-alpine)로 제공. 별도 설치도 가능 |

U1 `code-summary.md` §1이 확립한 빌드 골격(Gradle Wrapper·spotless(Google Java Format)·JUnit 태그·Vite/Jest/ESLint)을 그대로 사용한다. 후속 유닛(U2~U6)은 이 골격을 상속하므로 빌드 절차는 유닛 무관하게 동일하다.

## 2. 환경 설정 (.env · docker-compose · PostgreSQL)

루트 `.env.example`를 복사해 `.env`를 만들고 값을 채운다(`.env`는 커밋 금지 — R-U1-26, `.gitignore` 반영).

```bash
cp .env.example .env
# 아래 값을 반드시 채운다:
#   DB_PASSWORD            — DB 비밀번호
#   ADMIN_EMAIL            — 초기 관리자 이메일 (R-U1-27, 미설정 시 API 부팅 중단)
#   ADMIN_PASSWORD         — 초기 관리자 비밀번호 (BCrypt 해싱 후 시드)
#   BCRYPT_COST=10         — 기본 10, 하한 8 (NFR-SEC-1)
#   API_PORT=8080 / WEB_PORT=8081
#   CORS_ALLOWED_ORIGINS=http://localhost:8081  — FE 오리진(세션 쿠키 credentials)
```

`docker-compose.yml`은 3개 서비스로 구성된다(U1 `code-summary.md` §1 루트, deployment-architecture.md §1):
- `learnkk-db`(postgres:16-alpine, 호스트 미노출·내부 네트워크만, `pgdata` 볼륨, `pg_isready` 헬스체크)
- `learnkk-api`(멀티스테이지 빌드 gradle→JRE17, `uploads` 볼륨, `/actuator/health` 헬스체크, DB healthy 조건 후 기동)
- `learnkk-web`(Vite 빌드→nginx 정적 서빙 + `/api` 프록시)

DB 스키마는 애플리케이션 부팅 시 Flyway가 자동 적용한다(`V1__init_users` … `V6__metrics_history_indexes`). 별도 수동 마이그레이션은 불필요하다.

## 3. 빌드 명령 (build commands)

### 3.1 백엔드 (`learnkk-api`)

```bash
cd learnkk-api
# 전체 빌드(포맷 검사 + 컴파일 + 단위 테스트, 통합 테스트는 로컬 배제)
./gradlew build -PexcludeIntegration
# 또는 본 스테이지 검증에 사용한 조합:
./gradlew spotlessJavaCheck compileTestJava test -PexcludeIntegration
```

- `spotlessJavaCheck` — Google Java Format 위반 검사(위반 시 `./gradlew spotlessApply`로 자동 포맷).
- `-PexcludeIntegration` — `@Tag("integration")`가 붙은 Testcontainers 테스트를 제외(로컬 Docker 부재 대응, `cid:code-generation:c1`).
- 통합 테스트 포함 전량 실행(Docker 가용 CI): `./gradlew build` (플래그 없이).

### 3.2 프론트엔드 (`learnkk-web`)

```bash
cd learnkk-web
npm ci          # node_modules 부재 시에만(재현 가능한 설치). 이미 있으면 생략 가능
npm run build   # tsc --noEmit(타입체크) && vite build(프로덕션 번들)
npm run lint    # eslint --max-warnings=0 (경고 0 게이트)
```

### 3.3 로컬 스택 전체 기동(선택 · 라이브 스모크용)

```bash
docker compose up -d --build   # Docker 가용 환경에서만
# 검증 후:
docker compose down -v
```

## 4. 빌드 검증 (build verification)

- 백엔드: `BUILD SUCCESSFUL` 출력 + `learnkk-api/build/libs/*.jar` 생성 + `build/test-results/test/`에 단위 테스트 리포트 XML 생성.
- 프론트: `vite build`가 `dist/`에 `index.html`·`assets/*.js`·`assets/*.css`를 생성하고 `✓ built` 출력. `tsc --noEmit`·ESLint 오류 0.
- 실제 실행 결과(본 스테이지에서 측정)는 `build-test-results.md`를 참조한다.

## 5. 흔한 문제 해결 (troubleshooting)

| 증상 | 원인 | 해결 |
|---|---|---|
| `docker info` 실패 / Testcontainers 오류 | 로컬 Docker 미가용 | 통합 테스트는 `-PexcludeIntegration`으로 배제하고 CI(Docker 가용)에서 실행. 로컬은 단위 테스트 + 실 compose 라이브 스모크로 동등 검증(project.md Testing Posture, integration-test-instructions.md §대안) |
| API 컨테이너가 부팅 직후 종료 | `ADMIN_EMAIL`/`ADMIN_PASSWORD` 미설정 | `.env`에 값 설정(R-U1-27 fail-fast: 관리자 없는 상태 기동 방지). `IllegalStateException: 관리자 시드 실패 …` 로그 확인 |
| `DB_PASSWORD 를 설정하세요` 오류 | compose 필수 env 미설정 | `.env`의 `DB_PASSWORD` 채움 |
| spotless 검사 실패 | Google Java Format 위반 | `./gradlew spotlessApply` 실행 후 재빌드 |
| ESLint 실패(경고 발생) | `--max-warnings=0` 게이트 | 경고 원인 수정(팀 규칙: 경고=실패) |
| Gradle Wrapper 최초 실행 지연 | Gradle 배포판·의존성 다운로드 | 최초 1회만 발생, 재빌드 시 캐시 사용 |
| 세션 쿠키가 FE에서 전송되지 않음 | CORS/credentials 설정 | `CORS_ALLOWED_ORIGINS`가 FE 오리진과 일치하는지 확인(project.md Tech Stack: 분리 저장소 CORS + credentials) |

## 6. 상위 산출물 참조

- 빌드 골격·스크립트 정의: U1 `code-generation-plan.md`(Step 1·2·13), U1 `code-summary.md` §1·§3.
- 유닛별 추가 산출물(마이그레이션 V2~V6·서비스·컨트롤러): U2~U6 `code-summary.md` §1.
- 배포 아키텍처·compose 근거: `U1-foundation/infrastructure-design/deployment-architecture.md`.
