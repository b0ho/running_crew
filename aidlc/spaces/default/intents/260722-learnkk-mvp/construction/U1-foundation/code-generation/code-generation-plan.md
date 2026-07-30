# Code Generation Plan — U1 foundation (LearnKK 파일럿, 워킹 스켈레톤 Bolt)

> Construction · code-generation 단계 계획 · 유닛 U1-foundation
> 리드 aidlc-developer-agent (오케스트레이터 계획 → 개발자 에이전트 실행)
> 상위 입력: functional-design(business-logic-model·business-rules·domain-entities·frontend-components), nfr-design(performance·security), infrastructure-design(deployment), units-generation/unit-of-work(U1), requirements
> 범위: 실행 가능한 최소 앱 골격 + 인증 + RBAC 시드 (US-0/1/2). 워킹 스켈레톤 end-to-end 관통: 가입 → 로그인(세션) → 인증 필요 호출 200 → 관리자 로그인 → 관리자 스텁 200.

## 저장소 레이아웃 (team.md: FE/BE 분리)

- `learnkk-api/` — Spring Boot 3.x (Java 17), Gradle. 워크스페이스 루트 하위 디렉터리.
- `learnkk-web/` — React + Vite + TypeScript + Tailwind. 워크스페이스 루트 하위 디렉터리.
- `docker-compose.yml`, `.env.example` — 워크스페이스 루트(로컬 Docker 스택).

## 테스트 전략 (Comprehensive + team.md 정련)

핵심 도메인(인증·시드) 80% 라인 커버리지 목표, 백엔드 통합은 **Testcontainers**(실 PostgreSQL), 프론트 Jest/RTL, 스모크 E2E 1개(로그인 플로우). DTO·getter/setter·단순 마크업은 커버리지 제외. 테스트 파일은 필수 산출물.

---

## PART A — 백엔드 (learnkk-api)

### Step 1: 프로젝트 구조 & 빌드 설정 (인프라)
- [x] Gradle 프로젝트(`build.gradle`, `settings.gradle`), Spring Boot 3.x, Java 17.
- [x] 의존성: spring-boot-starter-web, -security, -data-jpa, -validation, -actuator, flyway-core, postgresql, springdoc-openapi-starter-webmvc-ui, spring-boot-starter-test, testcontainers(junit-jupiter, postgresql), spring-security-test, archunit.
- [x] Google Java Format(spotless) 플러그인 — 린트/포맷 CI 게이트용.
- [x] `application.yml`: env 기반 DB URL/계정, `security.bcrypt.cost`(기본 10), `server.servlet.session.timeout`, multipart(max-file-size 10MB), CORS 오리진.
- 트레이스: 인프라(W1)

### Step 2: 로컬 Docker 스택 (인프라)
- [x] `learnkk-api/Dockerfile`(JRE 17, fat jar), `docker-compose.yml`(learnkk-db PostgreSQL 16 + learnkk-api + learnkk-web, 명명 볼륨 pgdata·uploads, db 포트 내부만, 헬스체크), `.env.example`(DB·ADMIN_EMAIL·ADMIN_PASSWORD·BCRYPT_COST), 루트 `.gitignore`(.env 제외).
- 트레이스: infrastructure-design(W1)

### Step 3: DB 스키마 & Flyway 마이그레이션 (domain-entities §2)
- [x] `V1__init_users.sql`: users 테이블(id BIGSERIAL PK, email VARCHAR(254) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL, nickname VARCHAR(50) NOT NULL, password_hash VARCHAR(60) NOT NULL, is_admin BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMP NOT NULL DEFAULT now()). email UNIQUE 인덱스.
- 트레이스: US-0/1, R-U1-02(email UNIQUE), domain-entities §2

### Step 4: User 엔티티 & 리포지토리 (domain-entities §2)
- [x] `User` JPA 엔티티(필드·제약), `UserRepository`(findByEmail, existsByEmail).
- 트레이스: US-1/2, INV-2/3

### Step 5: DTO (business-rules §1, domain-entities §2)
- [x] `SignupRequest`(email·name·nickname·password + Bean Validation, **isAdmin 필드 없음** R-U1-06), `LoginRequest`, `UserDto`(id·email·name·nickname·isAdmin — passwordHash 미포함 INV-1), `ErrorResponse`(code·message·timestamp·path R-U1-17).
- 트레이스: US-1/2, R-U1-01~07, INV-1, NFR-7

### Step 6: 도메인 예외 & 전역 에러 핸들러 (business-rules §4/§4.1)
- [x] 도메인 예외(DuplicateEmailException, InvalidCredentialsException, EntityNotFoundException, FileConstraintViolationException, ValidationException).
- [x] `@RestControllerAdvice GlobalExceptionHandler`: R-U1-17a~17i 완전 매핑(400/401/403/404/409/500), 내부 상세 비노출(R-U1-19). DataIntegrityViolation(23505)→409 DUPLICATE_EMAIL.
- 트레이스: US-0, R-U1-17~20, R-U1-17a~i

### Step 7: Spring Security 설정 (business-rules §3, security-design §1/§2)
- [x] `SecurityConfig`: `BCryptPasswordEncoder`(cost 설정값) 빈, SecurityFilterChain(permitAll: `/api/auth/**`·springdoc·`/actuator/health`; 그 외 authenticated), `@EnableMethodSecurity`, `sessionManagement().sessionFixation().changeSessionId()`, CORS(FE 오리진 + allowCredentials), CSRF는 세션 쿠키 정책에 맞춰 설정(파일럿: SameSite=Lax + 상태변경 보호 방안 명시).
- [x] `UserDetailsService`(email 조회, isAdmin→ROLE_ADMIN + ROLE_USER 매핑).
- 트레이스: US-0, R-U1-12~16a, NFR-SEC-1~4

### Step 8: AuthService & AuthController (business-logic-model §2/§3, frontend §3)
- [x] `AuthService`: signup(정규화 R-U1-02→검증→중복→BCrypt→isAdmin=false 저장→UserDto), login(조회→matches→동일 401 R-U1-09→세션 수립), me(현재 세션 사용자), logout(세션 무효화).
- [x] `AuthController`: POST `/api/auth/signup`(201), POST `/api/auth/login`(200 + Set-Cookie), GET `/api/auth/me`(200/401), POST `/api/auth/logout`. 관리자 전용 스텁 엔드포인트 1개(`@PreAuthorize("hasRole('ADMIN')")`, 스켈레톤 관통 검증용).
- 트레이스: US-1/2, R-U1-01~11, R-U1-16a

### Step 9: FileStorageService 골격 (business-logic-model §7, business-rules §5)
- [x] `FileStorageService`: store(스트리밍·MIME/크기 검증·서버 UUID 파일명·웹루트 밖), load(경로 이탈 방지), delete(멱등). 제약 상수(허용 MIME jpg/png/pdf, 10MB). U1은 계약·검증만(사용처는 U4/U5).
- 트레이스: 인프라(W7), R-U1-21~24

### Step 10: 관리자 시드 (domain-entities §6, business-rules §6)
- [x] 부팅 시드(멱등·env 주입·fail-fast): `SELECT 1 ... WHERE email=:adminEmail` 존재 시 no-op, 미존재 시 env 비밀번호 BCrypt 해싱 후 isAdmin=true 삽입. ADMIN_EMAIL/ADMIN_PASSWORD 미설정 시 부팅 중단(R-U1-27). 평문 커밋 금지(R-U1-26).
  - 구현 방식: Flyway 이후 실행되는 Spring `ApplicationRunner` 시더(BCrypt·env 접근 용이). (대안: Java 기반 Flyway 마이그레이션 — 개발자 판단, 멱등·fail-fast·env 규칙은 동일.)
- 트레이스: US-0, R-U1-25~27, INV-4

### Step 11: 백엔드 테스트 (Comprehensive + team.md)
- [x] 단위: AuthService signup(정상·중복 409·검증 400·isAdmin 강제 false·password<8 400·공백 password), login(성공·미존재/불일치 동일 401).
- [x] 통합(Testcontainers 실 PostgreSQL): 가입→로그인→`/me` 200 관통, 보안 필터(미인증 401·permitAll), 에러 매핑(400/401/403/404/409/500), email UNIQUE 경쟁→409, 시드 멱등·fail-fast.
- [x] ArchUnit: 컨트롤러 반환 타입이 @Entity 미노출(INV-1/DTO 경계).
- 트레이스: 모든 US-0/1/2 규칙, INV-1~4

### Step 12: API 문서 & 헬스 (인프라)
- [x] springdoc-openapi 설정(permitAll), `/actuator/health`(shallow + DB). OpenAPI 스펙 산출(FE 계약 동기화용).
- 트레이스: 인프라(W8)

---

## PART B — 프론트엔드 (learnkk-web)

### Step 13: 프로젝트 구조 & 도구 (frontend-components, team.md React)
- [x] Vite + React + TypeScript, Tailwind(경량 커스텀, 미니멀·중립 톤 + 강조색 1), ESLint + Prettier, Jest + React Testing Library 설정. Named export 규약.
- 트레이스: 인프라, `cid:refined-mockups:c1`

### Step 14: ApiClient (frontend §2.1)
- [x] 중앙 API 래퍼(fetch/axios): `credentials: 'include'`, 공통 에러 DTO 정규화 throw. 컴포넌트 산발 try-catch 지양.
- 트레이스: US-1/2, NFR(FE/BE 분리 CORS)

### Step 15: AuthProvider (frontend §2.2)
- [x] 인증 컨텍스트: currentUser·status(loading/authed/anon), signup/login/logout, 부팅 시 `GET /api/auth/me`로 복원.
- 트레이스: US-1/2, R-U1-11

### Step 16: 인증 화면 (frontend §2.3/2.4)
- [x] `AuthPage`, `LoginForm`(email/password 클라 검증, 실패 시 동일 문구, 성공→대시보드), `SignupForm`(email·name·nickname·password + 확인, isAdmin 없음, 409/400 처리). data-testid 부여.
- 트레이스: US-1/2, R-U1-01~11, R-U1-09

### Step 17: 보호 라우트 & 공통 셸 (frontend §2.5/2.6)
- [x] `RequireAuth`(미인증→/auth 리다이렉트), `ResponsiveTabBar`(데스크톱 상단/모바일 하단, 사이드바 미사용), 최소 "내 코호트 대시보드" 플레이스홀더(스켈레톤 관통 목적지).
- 트레이스: US-2, NFR-3, `cid:rough-mockups:c1`

### Step 18: 프론트엔드 테스트 (Jest/RTL)
- [x] LoginForm(검증·실패 문구·성공 이동), SignupForm(검증·409 처리), ApiClient(에러 정규화), RequireAuth(리다이렉트). data-testid 기반.
- 트레이스: US-1/2

### Step 19: 프론트 Docker & 서빙
- [x] `learnkk-web/Dockerfile`(빌드 → nginx 정적 서빙 + /api 프록시 옵션). 프로덕션 빌드.
- 트레이스: infrastructure-design

---

## PART C — 관통 검증 & 문서

### Step 20: 워킹 스켈레톤 스모크 (business-logic-model §9)
- [x] compose up → Flyway·시드 성공 → 가입 → 로그인(세션 쿠키) → `/me` 200 → 관리자 로그인 → 관리자 스텁 200 관통. 스모크 스크립트 또는 통합 스모크 테스트 1개.
- 트레이스: 워킹 스켈레톤 게이트

### Step 21: 문서 & 환경 템플릿
- [x] `learnkk-api/README`, `learnkk-web/README`, 루트 README(compose 기동 절차), `.env.example`, OpenAPI 계약 위치 명시.
- 트레이스: team.md(OpenAPI 계약 동기화)

---

## 스토리 → 코드 스텝 추적 요약

| 스토리 | 커버 스텝 |
|---|---|
| US-0 (RBAC·시드·보안 골격) | Step 3·6·7·10·11 |
| US-1 (회원가입) | Step 5·8·11·16·18 |
| US-2 (로그인·세션) | Step 7·8·11·15·16·17·18·20 |
| 공통 인프라 (에러·파일·문서·Docker) | Step 1·2·6·9·12·13·14·19·21 |

## 실행 방식

Step 4(PART 2 생성)에서 오케스트레이터가 `aidlc-developer-agent`에 위임하여 위 스텝을 순차 실행하고 각 체크박스를 완료 표시한다. 애플리케이션 코드는 워크스페이스 루트(`learnkk-api/`·`learnkk-web/`)에 생성하며, 본 계획·요약 문서만 record 디렉터리에 둔다.
