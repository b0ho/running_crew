# Code Summary — U1 foundation (LearnKK 파일럿, 워킹 스켈레톤)

> Construction · code-generation 단계 산출물 요약 · 유닛 U1-foundation
> 리드 aidlc-developer-agent · 계획: `code-generation-plan.md`(21개 스텝, 체크박스 전부 완료)
> 범위: 실행 가능한 최소 앱 골격 + 인증 + RBAC 시드(US-0/1/2). end-to-end 관통 검증 완료.

## 1. 생성 파일 목록 (경로 · 역할)

애플리케이션 코드는 워크스페이스 루트(`learnkk-api/`·`learnkk-web/`·루트)에 생성했다(record 디렉터리에는 본 요약만).

### 백엔드 `learnkk-api/` (Spring Boot 3.3.5 · Java 17 · Gradle)

| 경로 | 역할 | 트레이스 |
|---|---|---|
| `build.gradle`, `settings.gradle`, `gradlew`(+wrapper) | Gradle 빌드·의존성·spotless(Google Java Format)·JUnit 태그 | Step 1 |
| `Dockerfile`, `.dockerignore` | 멀티스테이지 빌드(gradle→JRE17), 업로드 볼륨 | Step 2 |
| `src/main/resources/application.yml` | env 기반 DB·세션·multipart(10MB)·CORS·bcrypt.cost·admin·actuator | Step 1 |
| `src/main/resources/db/migration/V1__init_users.sql` | users 테이블 + email UNIQUE 인덱스 | Step 3, R-U1-02 |
| `user/User.java` | User JPA 엔티티(newMember/newAdmin 팩토리, isAdmin 강제) | Step 4, INV-2 |
| `user/UserRepository.java` | findByEmail·existsByEmail | Step 4 |
| `auth/dto/SignupRequest.java` | 가입 DTO(Bean Validation, **isAdmin 필드 부재**) | Step 5, R-U1-06 |
| `auth/dto/LoginRequest.java`, `auth/dto/UserDto.java` | 로그인 DTO · 응답 DTO(passwordHash 미포함) | Step 5, INV-1 |
| `common/dto/ErrorResponse.java` | 공통 에러 DTO(code·message·timestamp·path) | Step 5, R-U1-17 |
| `common/exception/*.java` (5 예외 + ErrorCode) | 도메인 예외 · 표준 code 상수 | Step 6 |
| `common/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` 예외→HTTP 완전 매핑(R-U1-17a~17i) | Step 6 |
| `config/SecurityConfig.java` | BCrypt(cost 하한 8) · 세션 · changeSessionId · `@EnableMethodSecurity` · permitAll · CORS(credentials) | Step 7, NFR-SEC-1~3 |
| `config/UserDetailsServiceImpl.java` | email 조회 → ROLE_USER(+ROLE_ADMIN) | Step 7 |
| `config/RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java` | 필터체인 401/403 을 공통 에러 DTO 로 정규화 | Step 6/7, R-U1-17e/f |
| `config/OpenApiConfig.java` | springdoc-openapi 메타 | Step 12 |
| `auth/AuthService.java` | signup·authenticate·currentUser(정규화·검증·중복·BCrypt·동일 401) | Step 8, R-U1-01~09 |
| `auth/AuthController.java` | `/api/auth/signup·login·me·logout`(세션 수립·재발급) | Step 8 |
| `admin/AdminController.java` | 관리자 스텁 `/api/admin/ping` `@PreAuthorize("hasRole('ADMIN')")` | Step 8, R-U1-16a |
| `file/FileStorageService.java`, `FileStorageProperties.java` | store/load/delete 골격(MIME·10MB·UUID·경로이탈 방지·멱등 delete) | Step 9, R-U1-21~24 |
| `seed/AdminSeeder.java`, `AdminSeedProperties.java` | ApplicationRunner 시더(멱등·env·BCrypt·isAdmin=true·fail-fast) | Step 10, R-U1-25~27 |
| `LearnkkApiApplication.java` | 진입점(@ConfigurationPropertiesScan) | Step 1 |
| `README.md` | 빌드·실행·API·규칙 | Step 21 |

**백엔드 테스트** (`src/test/...`)

| 경로 | 역할 |
|---|---|
| `auth/AuthServiceTest.java` | 단위 8건 — signup(정상·중복·검증·isAdmin 강제 false·password 규칙), authenticate(성공·ROLE_ADMIN·미존재/불일치 동일 401) |
| `seed/AdminSeederTest.java` | 단위 4건 — fail-fast·멱등·BCrypt·isAdmin |
| `file/FileStorageServiceTest.java` | 단위 7건 — MIME/크기/UUID/load/경로이탈/멱등 delete |
| `arch/ArchitectureTest.java` | ArchUnit — 컨트롤러 반환 타입 @Entity 미노출(INV-1) |
| `support/IntegrationTestBase.java` | Testcontainers(postgres:16) 베이스, `@Tag("integration")` |
| `auth/AuthIntegrationTest.java` | 통합 8건 — 관통(가입→로그인→/me→관리자 스텁), 401/403, 401 INVALID_CREDENTIALS, 400, 409(대소문자), 시드 멱등 |

### 프론트엔드 `learnkk-web/` (Vite · React 18 · TS · Tailwind)

| 경로 | 역할 | 트레이스 |
|---|---|---|
| `package.json`, `tsconfig.json`, `vite.config.ts` | 빌드·타입·dev 프록시(/api) | Step 13 |
| `tailwind.config.js`, `postcss.config.js`, `src/index.css` | 경량 커스텀(중립 톤 + accent 1) | Step 13, refined-mockups:c1 |
| `.eslintrc.cjs`, `.prettierrc.json`, `jest.config.cjs`, `src/setupTests.ts` | 린트·포맷·테스트 | Step 13 |
| `src/api/ApiClient.ts` | 중앙 래퍼(credentials:include, 에러 정규화 ApiError) | Step 14 |
| `src/api/authApi.ts`, `src/api/types.ts` | 인증 API · 계약 타입 | Step 14 |
| `src/auth/authContext.ts`, `AuthProvider.tsx` | 인증 컨텍스트·useAuth·부팅 /me 복원 | Step 15 |
| `src/auth/LoginForm.tsx`, `SignupForm.tsx`, `AuthPage.tsx` | 로그인/가입 폼(클라 검증·data-testid·aria) | Step 16 |
| `src/auth/RequireAuth.tsx` | 보호 라우트 가드(미인증→/auth) | Step 17 |
| `src/shell/ResponsiveTabBar.tsx` | 데스크톱 상단/모바일 하단 탭 셸 | Step 17 |
| `src/pages/DashboardPage.tsx` | 내 코호트 대시보드 플레이스홀더(로그인 목적지) | Step 17, R-U1-11 |
| `src/App.tsx`, `src/main.tsx` | 라우팅·부트스트랩 | Step 13 |
| `Dockerfile`, `nginx.conf`, `.dockerignore` | 빌드→nginx 정적 서빙 + /api 프록시 | Step 19 |
| `README.md` | 스크립트·구조·규약 | Step 21 |

**프론트 테스트**: `ApiClient.test.ts`(에러 정규화·credentials·네트워크), `LoginForm.test.tsx`(검증·성공 이동·401 동일 문구), `SignupForm.test.tsx`(검증·확인 불일치·409·성공), `RequireAuth.test.tsx`(리다이렉트·통과) — 총 13건.

### 루트

| 경로 | 역할 |
|---|---|
| `docker-compose.yml` | learnkk-db(내부만)+api+web, 명명 볼륨 pgdata·uploads, 헬스체크, env fail-fast |
| `.env.example` | 환경변수 템플릿(DB·ADMIN_*·BCRYPT_COST·포트·CORS) |
| `.gitignore` | `.env`·build·node_modules·uploads 제외(시크릿 커밋 금지) |
| `README.md` | 스택 기동 절차·관통 경로·규약 |

## 2. 스토리 → 코드 매핑

| 스토리 | 구현 |
|---|---|
| **US-0** (RBAC·시드·보안 골격) | `SecurityConfig`(세션·changeSessionId·permitAll·`@EnableMethodSecurity`·CORS), `UserDetailsServiceImpl`(역할 매핑), `AdminController`(`@PreAuthorize`), `AdminSeeder`(멱등·fail-fast), `GlobalExceptionHandler`(401/403/…), V1 마이그레이션 |
| **US-1** (회원가입) | `SignupRequest`(isAdmin 부재), `AuthService.signup`(정규화·검증·중복·BCrypt·isAdmin=false), `AuthController` POST signup(201), `SignupForm` |
| **US-2** (로그인·세션) | `AuthService.authenticate`(동일 401), `AuthController` login(세션 재발급)·me·logout, `AuthProvider`·`LoginForm`·`RequireAuth`·`ResponsiveTabBar`·`DashboardPage` |

핵심 규칙 준수: BCrypt 저장(R-U1-05/INV-2, 라이브 `$2a$` 확인), isAdmin 강제 false(R-U1-06), DTO 경계·passwordHash 미노출(INV-1, ArchUnit), 미존재/불일치 동일 401(R-U1-09), email 소문자 정규화·UNIQUE(R-U1-02), 시드 멱등·fail-fast(R-U1-25~27), 전역 에러 정규화(R-U1-17~20).

## 3. 실행 / 검증 결과

환경: 호스트에 **Java/Gradle 미설치**, **Node 24·Docker(Rancher) 가용**. 백엔드 빌드·테스트는 `gradle:8.10-jdk17` Docker 이미지로 수행.

### ✅ 통과 (실행 확인)

- **백엔드 컴파일 + 단위 테스트 + ArchUnit + spotless**: `gradle spotlessApply build -PexcludeIntegration` → **BUILD SUCCESSFUL**, 단위/구조 테스트 **20건 전부 PASSED**(AuthService 8·AdminSeeder 4·FileStorage 7·ArchUnit 1). Google Java Format 적용됨.
- **프론트엔드**: `npm run build`(tsc 타입체크 + Vite 프로덕션 빌드) 성공, `npm run lint`(ESLint, **경고 0** 게이트) 통과, `npm test`(Jest/RTL) **13건 전부 통과**.
- **라이브 워킹 스켈레톤 스모크** (`docker compose up --build` 실제 스택, 실 PostgreSQL 16): 아래 관통 전부 확인 후 스택 파기.
  - Flyway V1 마이그레이션 성공, 관리자 시드 삽입(id=1, isAdmin=true).
  - 가입 `POST /signup` → **201**, email `Alice@Learnkk.local`→`alice@learnkk.local` 정규화 확인.
  - 로그인 `POST /login` → **200 + JSESSIONID 쿠키**.
  - `GET /me`(세션) → **200**, 세션 없음 → **401 UNAUTHORIZED**(정규화 DTO).
  - 관리자 로그인 → **200 isAdmin=true**, `GET /admin/ping`(관리자 세션) → **200**, 일반 사용자 → **403 FORBIDDEN**, 미인증 → **401 UNAUTHORIZED**.
  - 로그인 실패(미존재/불일치) → **동일 401 INVALID_CREDENTIALS**(R-U1-09).
  - 중복 가입(대소문자만 다름) → **409 DUPLICATE_EMAIL**(R-U1-02).
  - 검증 실패 → **400 VALIDATION_ERROR**(필드 요약).
  - DB 확인: is_admin=true 계정 **1건**(시드 멱등), password_hash 접두 **`$2a$`**(INV-2).
  - web 컨테이너 정적 서빙 `GET :8081/` → **200**.
  - **fail-fast 확인**: ADMIN_EMAIL/ADMIN_PASSWORD 미설정으로 API 컨테이너 기동 시 `IllegalStateException: 관리자 시드 실패 …` → "Application run failed" 로 컨텍스트 종료(부팅 중단, R-U1-27).

### ⚠️ 실행하지 못한 검증 (정직한 기록)

- **Testcontainers 통합 테스트(`AuthIntegrationTest`)를 gradle 로 직접 실행하지 못함**. 이유: 이 환경은 Rancher Desktop 이며 docker 소켓 경로(`~/.rd/docker.sock`)를 빌드용 gradle 컨테이너에 bind-mount 할 수 없어(“operation not supported”) Docker-in-Docker 로 Testcontainers 를 띄우지 못했다. 코드/테스트는 완성되어 있으며, **JDK 17 + Docker 소켓이 있는 호스트/CI 에서 `./gradlew test` 로 그대로 실행 가능**하다. 대신 그 통합 테스트가 검증하려던 **동일 시나리오(관통·401·403·400·409·시드·에러 매핑)를 실제 compose 스택 + 실 PostgreSQL 에 대한 라이브 스모크로 모두 대체 검증**했다(위 참조).
- email UNIQUE **동시 삽입 경쟁**(DataIntegrityViolation→409) 은 핸들러 매핑으로 구현했으나 동시성 부하 재현 테스트는 U1 범위에서 수행하지 않았다(사전 조회 중복은 라이브 409 확인). 진성 동시성 테스트는 U3 정원 경합에서 다룬다(team.md).

## 4. 관통 경로 확인 방법 (재현)

```bash
cp .env.example .env         # DB_PASSWORD·ADMIN_EMAIL·ADMIN_PASSWORD 채우기
docker compose up -d --build
# README.md "워킹 스켈레톤 관통 경로" 의 curl 시퀀스로 가입→로그인→/me→관리자 스텁 확인
docker compose down -v
```

백엔드 단위 테스트: `cd learnkk-api && ./gradlew build -PexcludeIntegration`
통합 테스트(Docker 소켓 있는 호스트): `cd learnkk-api && ./gradlew test --tests "com.learnkk.auth.AuthIntegrationTest"`
프론트: `cd learnkk-web && npm install && npm run build && npm run lint && npm test`

## 5. 잔여 사항 / 후속

- **파일럿 보류(설계 정합)**: TLS 종단, CSRF 토큰(현재 세션 쿠키 SameSite=Lax + REST 정책으로 비활성), 파일 콘텐츠/바이러스 스캔, SCA·시크릿 스캔, 로그인 rate-limit/계정 잠금, 타이밍 사이드채널 상수시간 방어 — 확장 시 재검토(`cid:practices-discovery:c3`).
- **FileStorageService** 는 계약·검증 골격만 확립. 실제 업로드 사용처는 U4(증빙)/U5(보고서·증서)에서 호출.
- **CI 워크플로(GitHub Actions)** 파일은 U1 코드 범위에서 생성하지 않았다(파이프라인 설계는 `infrastructure-design/cicd-pipeline.md` 에 명시, FE/BE 분리 저장소 분할 시 각 저장소에 배치 예정). 로컬 게이트(spotless·build·lint·test)는 위와 같이 통과 확인.
- **OpenAPI 계약**: springdoc 로 런타임 노출(`/v3/api-docs`, `/swagger-ui.html`). FE/BE 계약 동기화 시 이 스펙을 산출·공유(team.md).
- 통합 테스트는 CI(Docker 가용)에서 `-PexcludeIntegration` 없이 전량 실행할 것.


---

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

워킹 스켈레톤 U1-foundation 코드 생성(21개 스텝)을 엄격히 반증 시도했으나, 계획·설계·규칙과의 정합성을 깨뜨리는 defect를 발견하지 못했습니다. 실제 코드는 계획이 주장한 체크박스 전부를 구현했으며, business-rules·security-design가 명시한 11개 핵심 제약을 코드 수준에서 만족합니다. 라이브 compose 스모크 검증(요약 §3)이 Testcontainers 통합 테스트를 대체한 검증 전략은 타당합니다(동일 시나리오·실 PostgreSQL·관통 확인). 개발자는 이 코드를 아키텍처 가이던스 없이 빌드·배포할 수 있습니다.

### 검증 결과 (11개 핵심 제약 vs 실제 코드)

| 규칙 ID | 제약 | 실제 코드 | 판정 |
|---------|------|----------|------|
| **R-U1-05/INV-2** | 비밀번호 BCrypt 해싱 저장(평문 금지), cost 설정값 | `SecurityConfig.passwordEncoder()` — BCryptPasswordEncoder(bcrypt.cost), 하한 8 검증 명시. `AuthService.signup/authenticate`·`AdminSeeder` 전부 passwordEncoder 사용. application.yml 기본 10. 라이브 DB 확인 `$2a$` 접두(요약 §3). | ✅ PASS |
| **R-U1-06/INV-4** | 가입 시 isAdmin 강제 false, SignupRequest에 isAdmin 필드 부재 | `SignupRequest.java` — email·name·nickname·password 4필드만, isAdmin 부재(주석 명시). `User.newMember()` — 팩토리가 isAdmin=false 하드코딩. `AuthServiceTest.signup_정상_가입시_UserDto_반환하고_isAdmin_false` 검증. | ✅ PASS |
| **R-U1-02/INV-3** | email 소문자 정규화·UNIQUE | `AuthService.normalizeEmail()` — trim().toLowerCase(), signup/authenticate/currentUser 전 호출. `V1__init_users.sql` — email VARCHAR(254) NOT NULL + UNIQUE INDEX. 라이브 스모크 `Alice@`→`alice@` 확인, 대소문자 중복→409(요약 §3). | ✅ PASS |
| **R-U1-09** | 로그인 실패(미존재/불일치) 동일 401 INVALID_CREDENTIALS | `AuthService.authenticate()` — 미존재 조회 실패·불일치 모두 `InvalidCredentialsException` throw(메시지 동일). `GlobalExceptionHandler.handleInvalidCredentials` → 401. `AuthServiceTest` 두 케이스 검증. 라이브 스모크 확인(요약 §3). `LoginForm.tsx` 동일 문구 상수. | ✅ PASS |
| **INV-1/NFR-7** | 응답 DTO만, passwordHash·Entity 미노출 | `UserDto` — id·email·name·nickname·isAdmin만, passwordHash 부재. 컨트롤러 전부 UserDto/ErrorResponse. `ArchitectureTest.java` — `@RestController` 메서드가 `@Entity` 반환하지 않음 검증. 빌드 통과(요약 §3). | ✅ PASS |
| **R-U1-17a~17i** | 전역 에러 핸들러, 예외→HTTP 완전 매핑(400/401/403/404/409/500), 내부 상세 비노출 | `GlobalExceptionHandler.java` — 9개 `@ExceptionHandler`가 R-U1-17a~17i 전부 커버(Bean Validation, domain 예외, DataIntegrity→409, InvalidCredentials→401, AuthN/AccessDenied→401/403, NotFound→404, FileConstraint→400, 그 외→500 내부 비노출). `AuthIntegrationTest` 400/401/403/409 확인. | ✅ PASS |
| **R-U1-25~27** | 관리자 시드: 멱등·env 주입·BCrypt·isAdmin=true·fail-fast | `AdminSeeder.seed()` — existsByEmail 멱등, env 비밀번호 BCrypt 해싱, User.newAdmin(isAdmin=true), email/password 비어 있으면 IllegalStateException(부팅 중단). `AdminSeederTest` 4건 검증. 라이브 스모크 fail-fast 확인(요약 §3). docker-compose.yml `ADMIN_EMAIL:?` 검증. | ✅ PASS |
| **R-U1-21~24** | FileStorageService: 경로 이탈 방지·UUID 파일명·MIME/크기 검증·멱등 delete | `FileStorageService.java` — `ensureWithinRoot()` canonical path 검증, `store()` UUID 파일명, MIME/확장자/10MB 검증(R-U1-22/23), `delete()` deleteIfExists 멱등. `FileStorageServiceTest` 7건 검증(경로 이탈 거부·크기 초과·멱등 delete). | ✅ PASS |
| **NFR-SEC-3** | 세션 고정 방지: changeSessionId | `SecurityConfig.filterChain()` — `sessionFixation().changeSessionId()` 명시. `AuthController.login()` — `httpRequest.changeSessionId()` 추가 재발급. | ✅ PASS |
| **R-U1-16a** | 관리자 경로 @PreAuthorize, @EnableMethodSecurity | `SecurityConfig` — `@EnableMethodSecurity` 애너테이션. `AdminController.ping()` — `@PreAuthorize("hasRole('ADMIN')")`. 라이브 스모크: 관리자 세션→200, 일반 사용자→403, 미인증→401(요약 §3). | ✅ PASS |
| **frontend credentials/isAdmin** | ApiClient credentials include·에러 정규화, SignupForm isAdmin 입력 없음 | `ApiClient.request()` — `credentials: 'include'` 명시, ApiError 정규화. `SignupForm.tsx` — email·name·nickname·password·passwordConfirm 5필드만, isAdmin 입력 없음. | ✅ PASS |

### 계획 대비 구현 검증 (21개 스텝)

계획(`code-generation-plan.md`)의 21개 스텝을 실제 생성 파일과 대조했습니다. 요약 §1 파일 목록이 계획 PART A(백엔드 12스텝)·PART B(프론트 7스텝)·PART C(관통 2스텝)의 산출물을 정확히 커버합니다. 누락·불일치 없음.

| PART | 스텝 | 산출물 (샘플) | 판정 |
|------|------|---------------|------|
| A | Step 1~12 | build.gradle(의존성·spotless), application.yml(env·bcrypt·session·multipart·CORS), V1 마이그레이션, User·UserRepository, SignupRequest·UserDto·ErrorResponse, GlobalExceptionHandler, SecurityConfig·UserDetailsServiceImpl, AuthService·AuthController, AdminController, FileStorageService·FileStorageProperties, AdminSeeder·AdminSeedProperties, OpenApiConfig, 단위·통합·ArchUnit 테스트 | ✅ 전부 존재·규칙 정합 |
| B | Step 13~19 | package.json(Vite·React·TS·Tailwind·Jest), tailwind.config.js, ApiClient·authApi, AuthProvider·authContext, LoginForm·SignupForm·AuthPage, RequireAuth·ResponsiveTabBar·DashboardPage, App·main, Dockerfile·nginx.conf, 테스트(ApiClient·LoginForm·SignupForm·RequireAuth) | ✅ 전부 존재·규칙 정합 |
| C | Step 20~21 | 라이브 compose 스모크 검증(요약 §3), README 3개, .env.example | ✅ 검증 완료·문서 존재 |

### 워킹 스켈레톤 관통 경로 (business-logic-model §9)

요약 §3 "라이브 워킹 스켈레톤 스모크" 섹션이 다음 관통을 실제 PostgreSQL 16 스택에서 확인했음을 기록합니다:
1. 가입 `POST /signup` → 201 + email 정규화
2. 로그인 `POST /login` → 200 + JSESSIONID 쿠키
3. `GET /me`(세션) → 200 / 세션 없음 → 401
4. 관리자 로그인 → 200 isAdmin=true, `GET /admin/ping`(관리자 세션) → 200 / 일반 사용자 → 403 / 미인증 → 401
5. 실패(미존재/불일치) → 동일 401 INVALID_CREDENTIALS
6. 중복 가입(대소문자) → 409 DUPLICATE_EMAIL
7. 검증 실패 → 400 VALIDATION_ERROR
8. DB 확인: password_hash `$2a$` 접두, is_admin=true 1건(시드 멱등)
9. fail-fast: ADMIN_EMAIL/ADMIN_PASSWORD 미설정→부팅 중단

이는 계획 Step 20(관통 검증)의 요구를 완전히 충족합니다.

### Testcontainers 대체 검증 전략의 타당성

요약 §3 "실행하지 못한 검증"이 정직하게 기록한 대로, Rancher Desktop 소켓 제약으로 gradle 컨테이너 내 Testcontainers를 띄우지 못했습니다. 그러나:
- **코드·테스트 완성**: `AuthIntegrationTest`·`IntegrationTestBase`·`build.gradle` testcontainers 의존성·`@Tag("integration")` 전부 구현되어 있으며, JDK+Docker 소켓 환경(CI)에서 `./gradlew test` 그대로 실행 가능합니다.
- **동등 검증**: 라이브 compose 스모크가 그 통합 테스트가 검증하려던 **동일 시나리오**(관통·401·403·400·409·시드·에러 매핑)를 **실 PostgreSQL 16** 스택에서 대체했습니다. 테스트 코드의 assertion 논리와 라이브 확인 내용은 일대일 대응합니다.
- **판정**: 워킹 스켈레톤 게이트(계획 Step 20)는 "관통 경로가 동작함"을 요구하지, "gradle로 테스트를 실행했는가"를 요구하지 않습니다. 라이브 스모크가 그 게이트를 통과시켰으므로, 이 대체는 타당합니다. CI는 통합 테스트를 `-PexcludeIntegration` 없이 실행할 것입니다(요약 §5).

### 발견하지 못한 잠재 defect (한계 고지)

이 리뷰는 코드·설계·규칙의 **정합성**을 검증했으나, 다음 항목은 검증 범위 밖입니다:
- **타이밍 사이드채널**: business-rules §2가 파일럿 잔여 리스크로 명시. 미존재 계정 조회와 BCrypt 비교 간 시간 차는 상수시간 방어 없음(설계 정합).
- **동시성 부하**: email UNIQUE 경쟁 핸들러 매핑은 완성했으나, 진성 동시 부하 재현 테스트는 U1 범위에서 수행하지 않음(team.md 정합, U3에서 다룸).
- **바이러스·콘텐츠 스캔**: FileStorageService가 MIME·크기만 검증. security-design §6/§7이 파일럿 잔여 리스크로 명시.
- **로그인 rate-limit/계정 잠금**: security-design §2가 파일럿 보류로 명시(`cid:practices-discovery:c3`).

이들은 코드 defect가 아니라 **설계가 명시한 파일럿 범위 결정**이며, 확장 시 재검토 계획이 문서화되어 있습니다.

### 최종 판정 근거

READY 판정은 다음 조건을 만족했기 때문입니다:
1. **계획 완전 구현**: 21개 스텝의 체크박스 주장이 실제 코드로 존재함.
2. **규칙 준수**: 11개 핵심 제약(BCrypt·isAdmin 강제·동일 401·DTO 경계·시드 멱등/fail-fast·경로 이탈·세션·@PreAuthorize·credentials·전역 에러 매핑)을 코드가 만족함.
3. **관통 검증**: 워킹 스켈레톤 게이트(가입→로그인→/me→관리자 스텁)를 라이브 스택에서 확인함.
4. **테스트 존재**: 단위(20건)·ArchUnit(1건)·통합(8건, CI 실행 가능)·프론트(13건) 테스트가 규칙을 검증함.
5. **문서 정합**: 요약·계획·설계 간 불일치가 없으며, 잔여 리스크를 정직하게 명시함.

개발자는 이 코드·테스트·문서만으로 U1을 빌드·배포하고 후속 유닛(U2~U6)에 이 골격을 상속할 수 있습니다. 아키텍처 가이던스는 더 이상 필요하지 않습니다.

**판정: READY**
