# learnkk-api

LearnKK 백엔드 — Spring Boot 3.x (Java 17), Gradle, Spring Security(세션), Spring Data JPA, PostgreSQL 16, Flyway, springdoc-openapi.

## 요구 사항

- JDK 17 (Gradle 툴체인). 로컬에 JDK 가 없으면 Docker(`gradle:8.10-jdk17`)로 빌드 가능.
- 통합 테스트(Testcontainers)는 Docker 필요.

## 빌드 & 테스트

```bash
./gradlew spotlessApply build              # 포맷 + 컴파일 + 전체 테스트
./gradlew build -PexcludeIntegration       # Testcontainers(@Tag integration) 제외 (Docker 없이)
./gradlew test --tests "com.learnkk.auth.AuthIntegrationTest"   # 통합 테스트만
```

- 포맷: Google Java Format(spotless). `spotlessCheck` 가 `build` 게이트에 포함.
- 통합 테스트는 `@Tag("integration")` 로 분리되어 `-PexcludeIntegration` 로 제외할 수 있다.

## 실행

환경변수(또는 `.env` → compose)로 주입:

| 변수 | 설명 | 기본 |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL 접속 | localhost:5432/learnkk |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | 관리자 시드(미설정 시 부팅 중단, R-U1-27) | — |
| `BCRYPT_COST` | BCrypt cost(하한 8) | 10 |
| `CORS_ALLOWED_ORIGINS` | FE 오리진(allowCredentials) | http://localhost:5173 |
| `UPLOAD_DIR` | 업로드 볼륨(웹루트 밖) | ./var/uploads |

```bash
./gradlew bootRun
```

## API 개요 (OpenAPI: /v3/api-docs, Swagger UI: /swagger-ui.html)

| 메서드 | 경로 | 인가 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | permitAll | 회원가입(201) |
| POST | `/api/auth/login` | permitAll | 로그인(200 + 세션 쿠키) |
| GET | `/api/auth/me` | permitAll | 현재 세션 사용자(200/401) |
| POST | `/api/auth/logout` | permitAll | 로그아웃(204) |
| GET | `/api/admin/ping` | ROLE_ADMIN | 관리자 스텁(200/403) |
| GET | `/actuator/health` | permitAll | 헬스체크 |

## 아키텍처 규칙(강제)

- 비밀번호는 BCrypt 저장(R-U1-05, INV-2). API 는 DTO 만 노출, passwordHash 미노출(INV-1, ArchUnit 검증).
- 에러는 `@RestControllerAdvice` 로 공통 DTO(code·message·timestamp·path) 정규화(R-U1-17~20).
- 로그인 실패는 미존재/불일치 동일 401(R-U1-09).
- 관리자 인가는 메서드 레벨 `@PreAuthorize("hasRole('ADMIN')")`(R-U1-16a).
