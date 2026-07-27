# Business Rules — U1 foundation (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U1 책임), `unit-of-work-story-map.md`(US-0/1/2), `requirements-analysis/requirements.md`(FR-1, NFR-5/7), `application-design/components.md`(User), `component-methods.md`(AuthService.signup/login), `services.md`(AuthService·FileStorageService)
> 규칙 표기: R-U1-nn. 각 규칙은 검증 가능(수용 기준 대응)해야 한다.

## 1. 회원가입 규칙 (US-1 / FR-1)

| ID | 규칙 | 위반 시 처리 |
|---|---|---|
| R-U1-01 | email은 RFC 5322 기본 형식을 만족해야 한다(정규식 수준 검증) | 400 VALIDATION_ERROR, 필드 email |
| R-U1-02 | email은 **대소문자 무시 유일**해야 한다. email 정규화(소문자 변환)는 **R-U1-02가 단독 소유**하는 규칙이며, 검증·중복조회·저장 전 항상 정규화를 먼저 적용한 뒤 UNIQUE 제약을 건다 | 409 DUPLICATE_EMAIL |
| R-U1-03 | name·nickname은 비어 있지 않아야 하고 각각 최대 100·50자 | 400 VALIDATION_ERROR |
| R-U1-04 | password는 최소 8자, 공백만으로 구성 불가(파일럿 최소 정책) | 400 VALIDATION_ERROR |
| R-U1-05 | password는 **BCrypt로 해싱**하여 저장한다. 평문·가역 암호화 저장 금지 | (불변식 — 코드 리뷰/테스트로 강제) |
| R-U1-06 | 가입 시 isAdmin은 항상 false로 강제한다. `SignupRequest` DTO 스키마에는 **isAdmin 필드를 두지 않는다**(클라이언트가 값을 실을 여지 자체를 제거). 혹여 필드가 유입되어도 서비스가 false로 덮어쓴다(테스트로 강제) | 권한 상승 방지 |
| R-U1-07 | 수집 필드는 email·name·nickname·password 4개로 제한(그 외 필드 무시) | 최소 수집 원칙 |

- R-U1-05는 Mandated("비밀번호는 BCrypt로 해싱") + Forbidden("평문 저장 금지")의 하드 제약. BCrypt cost는 기본(10)로 두되 설정 가능.
- **email 규칙 소유권 정리(리뷰 반영)**: 형식 검증 = R-U1-01, 정규화+유일성 = R-U1-02. business-logic-model.md의 signup/login 절은 정규화를 R-U1-02로 단일 참조한다.

## 2. 로그인 규칙 (US-2 / FR-1)

| ID | 규칙 | 위반 시 처리 |
|---|---|---|
| R-U1-08 | email로 사용자 조회 후 BCrypt `matches`로 비밀번호 검증 | 실패 시 401 INVALID_CREDENTIALS |
| R-U1-09 | 존재하지 않는 email과 틀린 비밀번호는 **동일한 오류 메시지**로 응답(사용자 열거 방지) | 401 INVALID_CREDENTIALS(동일 문구) |
| R-U1-10 | 로그인 성공 시 인증 세션 수립(HttpOnly·SameSite 쿠키). 응답에 UserDto 반환 | — |
| R-U1-11 | 로그인 성공의 클라이언트 후속 목적지는 "내 코호트 대시보드"(FR-1 수용기준, NFR-3) | FE 라우팅 규칙 |

- **미결/잔여 리스크**: (1) 로그인 실패 잠금(브루트포스 방지)은 파일럿 범위 외(보안 위생 보류 결정 `cid:practices-discovery:c3`와 정합). 확장 시 rate-limit 도입. (2) **타이밍 사이드채널**: 미존재 계정(조회만)과 존재 계정(BCrypt 비교) 간 응답 시간 차로 인한 사용자 열거는 파일럿에서 상수시간 방어를 하지 않는다(NFR-5 보안 위생 보류와 정합). 확장 시 존재하지 않는 email에도 더미 BCrypt 비교를 수행해 시간 차를 제거하는 방어 검토.

## 3. 인가(Authorization) 규칙 — RBAC 골격 (US-0)

| ID | 규칙 |
|---|---|
| R-U1-12 | 관리자 전용 엔드포인트는 `isAdmin==true`(ROLE_ADMIN)만 접근 가능. 위반 시 403 FORBIDDEN |
| R-U1-13 | 인증이 필요한 엔드포인트는 유효 세션이 없으면 401 UNAUTHORIZED |
| R-U1-14 | 회원가입·로그인·API 문서(springdoc) 경로는 **비인증 허용**(permitAll) |
| R-U1-15 | 컨텍스트 역할(멘토/멘티) 판정은 각 소유 유닛이 소유 데이터로 수행. U1은 인증됨/관리자 판정만 담당 |
| R-U1-16 | 관리자 권한은 **일반 회원가입으로 부여 불가**. 시드 부트스트랩으로만 부여(`cid:user-stories:c1`) |
| R-U1-16a | 관리자 경로 인가는 **메서드 레벨 애너테이션**(`@PreAuthorize("hasRole('ADMIN')")`)으로 강제한다. U1의 필터체인은 **역할 매핑(ROLE_USER/ROLE_ADMIN)만 확립**하고 관리자 경로를 열거하지 않는다. 후속 유닛(U3/U6)은 자신의 컨트롤러에 애너테이션을 붙이는 것만으로 인가가 걸린다(U1과 충돌·순환 없음) |

## 4. 공통 에러 응답 규칙 (공통 인프라)

`team.md` Code Style(Spring)의 공통 에러 DTO 규약을 U1에서 확립한다.

| ID | 규칙 |
|---|---|
| R-U1-17 | 모든 오류 응답은 공통 에러 DTO 형식: `{ code, message, timestamp, path }` |
| R-U1-18 | 서비스 레이어는 **도메인 예외**를 던지고, `@RestControllerAdvice`가 HTTP 상태로 매핑한다. `Result<T,E>` 커스텀 패턴은 도입하지 않는다 |
| R-U1-19 | 예상치 못한 예외는 500 INTERNAL_ERROR로 정규화하며 스택트레이스·내부 메시지를 클라이언트에 노출하지 않는다 |
| R-U1-20 | 검증 오류(Bean Validation)는 400으로 매핑하고 위반 필드를 message에 요약한다 |

- 표준 code 값(파일럿): `VALIDATION_ERROR`, `DUPLICATE_EMAIL`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `FILE_CONSTRAINT_VIOLATION`, `INTERNAL_ERROR`.

### 4.1 예외 → HTTP 매핑 표 (U1 도메인 예외 — W2가 확립하는 @RestControllerAdvice 핸들러의 완전한 대상)

U1은 공통 예외 핸들러를 확립하므로, U1 범위의 모든 도메인/프레임워크 예외와 매핑을 아래로 확정한다. 신규 예외는 후속 유닛이 이 표에 추가한다(누락 시 500으로 새는 것을 방지).

| ID | 예외(Java) | HTTP | code |
|---|---|---|---|
| R-U1-17a | `ValidationException` / Bean Validation `MethodArgumentNotValidException` | 400 | VALIDATION_ERROR |
| R-U1-17b | `DuplicateEmailException` | 409 | DUPLICATE_EMAIL |
| R-U1-17c | `DataIntegrityViolationException`(email UNIQUE 위반, SQLState 23505) | 409 | DUPLICATE_EMAIL |
| R-U1-17d | `InvalidCredentialsException` | 401 | INVALID_CREDENTIALS |
| R-U1-17e | Spring Security `AuthenticationException`(미인증 세션) | 401 | UNAUTHORIZED |
| R-U1-17f | Spring Security `AccessDeniedException`(권한 부족) | 403 | FORBIDDEN |
| R-U1-17g | `EntityNotFoundException` | 404 | NOT_FOUND |
| R-U1-17h | `FileConstraintViolationException`(MIME/크기/확장자) | 400 | FILE_CONSTRAINT_VIOLATION |
| R-U1-17i | 그 외 미처리 `Exception` | 500 | INTERNAL_ERROR(내부 상세 비노출) |

- R-U1-17c는 R-U1-02 동시 가입 경쟁의 최종 방어선(business-logic-model §2). 사전 조회를 통과한 경쟁 삽입이 UNIQUE 위반으로 실패하면 이 매핑으로 DUPLICATE_EMAIL을 반환한다.

## 5. 파일 저장 제약 규칙 (공통 FileStorageService)

U1은 FileStorageService 골격과 제약을 확립한다(실제 업로드 사용처는 U4/U5).

| ID | 규칙 |
|---|---|
| R-U1-21 | 저장 파일은 **웹루트 밖** 볼륨에 저장한다(직접 URL 접근 불가) |
| R-U1-22 | 허용 MIME: 이미지(jpg/png) + 문서(pdf). 그 외 거부(FILE_CONSTRAINT_VIOLATION) |
| R-U1-23 | 파일당 최대 크기 **10MB** 초과 거부 |
| R-U1-24 | 저장 파일명은 서버 생성 식별자로 대체(원본 파일명 경로 주입 방지) |

- 파일럿 잔여 리스크: 바이러스 스캔·심층 콘텐츠 검증은 보류(`cid:practices-discovery:c3`). MIME·크기·확장자 기본 검증만 강제.

## 6. 관리자 시드 규칙 (US-0, Flyway 부트스트랩)

| ID | 규칙 |
|---|---|
| R-U1-25 | 시드는 **멱등**이어야 한다. 판정 쿼리는 **`SELECT 1 FROM users WHERE email = :adminEmail LIMIT 1`**(정규화된 관리자 email 기준). 결과 존재 시 no-op, 미존재 시에만 삽입한다. (email 기준으로 판정하는 이유: `isAdmin=true` 계정이 향후 복수가 될 수 있어 `WHERE isAdmin=true`는 부정확) |
| R-U1-26 | 시드 비밀번호는 환경변수에서 주입한다. 마이그레이션/시드 파일에 평문·해시를 **커밋하지 않는다**(Forbidden: 평문 저장, secret 하드코딩 금지 — construction 가드레일) |
| R-U1-27 | 시드 실행 시 환경변수(관리자 email·비밀번호)가 비어 있으면 **명시적 실패**로 부팅을 중단한다(조용한 스킵 금지 — 관리자 없는 상태로 기동 방지) |

## 7. 불변식 (Invariants)

- INV-1: 어떤 경로로도 User.passwordHash는 API 응답에 포함되지 않는다.
- INV-2: DB의 User.passwordHash는 항상 BCrypt 형식(`$2[aby]$`) 문자열이다.
- INV-3: email 컬럼은 항상 소문자 정규화된 유일 값이다.
- INV-4: 시드 관리자 외 isAdmin==true 계정은 시드/명시적 운영 조치로만 존재한다(가입 경로로는 생성 불가).
