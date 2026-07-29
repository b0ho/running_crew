# Security Design — U1 foundation (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 devsecops(보안 아키텍처)·compliance(개인정보)
> 상위 입력: `nfr-requirements/security-requirements.md`(NFR-SEC-1~4·인가·PII·파일 보안), `nfr-requirements/tech-stack-decisions.md`(Spring Security 세션), `functional-design/business-logic-model.md`(W3 필터체인·W5 로그인·W6 시드·W7 파일저장)
> 근거 관행: project.md Mandated(BCrypt·DTO·최소 PII), Forbidden(평문 저장·SSO·클라우드), Scope Overrides(`cid:practices-discovery:c3`)

U1은 공통 인프라 유닛으로서 **프로젝트 전체의 보안 골격(인증·인가·에러·파일 보안)** 을 확립한다. 후속 유닛은 이 골격을 상속한다.

## 1. 인증 아키텍처(Authentication)

`security-requirements.md` NFR-SEC-1~4의 요구를 구체 메커니즘으로 설계한다.

- **비밀번호 저장**: Spring Security `BCryptPasswordEncoder`. cost는 `security.bcrypt.cost` 설정값으로 외부화, 기본 10, 하한 8(NFR-SEC-1). `PasswordEncoder` 빈을 U1에서 단일 정의하여 가입(encode)·로그인(matches)이 동일 인코더 사용.
- **세션 관리**: 서버 세션(`HttpSession`). 쿠키 속성 `HttpOnly=true`, `SameSite=Lax`, `Secure`는 운영 TLS 활성 시 켬(NFR-SEC-2). 세션 타임아웃은 설정값(기본 30분 idle) — `server.servlet.session.timeout`.
- **세션 고정 방지(NFR-SEC-3)**: `http.sessionManagement(s -> s.sessionFixation().changeSessionId())`. 로그인 성공 처리에서 세션 ID를 재발급한다. code-generation은 이 설정을 SecurityFilterChain에 반드시 포함한다.
- **사용자 열거 방지(NFR-SEC-4)**: 미존재 계정·비밀번호 불일치를 동일 `InvalidCredentialsException` → 401 INVALID_CREDENTIALS로 매핑(business-logic-model §3). 타이밍 사이드채널은 파일럿 잔여 리스크로 명시.

### 인증 성공/실패 처리 흐름
```
POST /api/auth/login
  ├─ 인증 성공 ─> changeSessionId() -> SecurityContext 저장 -> Set-Cookie(HttpOnly) + 200 UserDto
  └─ 인증 실패(미존재/불일치 동일) ─> 401 {code: INVALID_CREDENTIALS}
```
<!-- Text fallback: 로그인 성공 시 세션 ID를 재발급하고 인증 컨텍스트를 저장한 뒤 HttpOnly 쿠키와 UserDto(200)를 반환한다. 실패는 미존재/불일치 구분 없이 401을 반환한다. -->

## 2. 인가 아키텍처(Authorization) — RBAC

`business-logic-model.md` §4의 필터체인 규칙을 보안 설계로 확정한다.

- **필터체인 경로 규칙**: `permitAll` = `/api/auth/**`, springdoc(`/v3/api-docs/**`, `/swagger-ui/**`), 헬스체크. 그 외 전 경로 `authenticated`.
- **역할 매핑**: 인증 사용자 → `ROLE_USER`; `User.isAdmin==true` → `ROLE_ADMIN` 추가. `UserDetailsService`가 DB의 `isAdmin`을 권한으로 변환.
- **관리자 인가 메커니즘(방어 지점 결정)**: 관리자 경로를 필터체인 URL로 열거하지 않고 **메서드 레벨 `@PreAuthorize("hasRole('ADMIN')")`** 로 강제한다(`@EnableMethodSecurity`). 근거: 후속 유닛(U3/U6)이 자기 컨트롤러 메서드에 애너테이션만 붙이면 인가가 걸려 U1 필터 설정과 충돌하지 않음(business-logic-model §4 정합).
- **권한 상승 차단**: 회원가입 DTO에 `isAdmin` 필드 부재(R-U1-06). 관리자 부트스트랩은 Flyway 시드로만(§4).

## 3. 방어 심층화(Defense in Depth) 계층

| 계층 | 방어 | 구현 |
|---|---|---|
| 전송 | (파일럿 보류) TLS 종단 | 확장 시 역방향 프록시에서 종단 |
| 인증 | 세션 쿠키 + 세션 고정 방지 | Spring Security(§1) |
| 인가 | 경로 규칙 + 메서드 애너테이션 | SecurityFilterChain + `@PreAuthorize`(§2) |
| 입력 | 서버측 Bean Validation | DTO `@Valid`(R-U1-01~04·07) |
| 출력 | DTO 경계 | JPA Entity 미노출, passwordHash 미포함(INV-1) |
| 저장 | 비밀번호 해싱 | BCrypt(§1) |

## 4. RBAC 관리자 시드 보안(W6)

`business-logic-model.md` §5 기준:
- 초기 관리자 email·비밀번호는 **환경변수 주입**(코드/마이그레이션 커밋 금지, construction 가드레일). 시드 시 BCrypt 해싱 후 `isAdmin=true` 삽입.
- 멱등: 이미 존재 시 no-op(R-U1-25).
- **환경변수 미설정 시 부팅 중단**(R-U1-27) — 관리자 없는 상태 기동 방지(신뢰성·보안 결합). `reliability-design.md` §2와 정합.

## 5. 데이터 보호 & 개인정보(compliance)

- **최소 수집 PII**: 이메일·성명·닉네임만(사번 등 미수집, project.md Mandated). 사내 도구로 외부 판매 없음.
- `User.passwordHash`는 어떤 API 응답 DTO에도 포함하지 않음(INV-1, NFR-7 DTO 경계).
- 개인정보 파기·보존기간 정책은 파일럿 범위 외(확장 시 compliance 재검토).

## 6. 파일 저장 보안(W7 · 공통 인프라)

`security-requirements.md` §4 + `business-logic-model.md` §7:
- 저장 파일은 **웹루트 밖 로컬 볼륨**, 서버 생성 UUID 파일명(R-U1-21/24) — 직접 URL 추측·실행 방지.
- **MIME·크기·확장자 검증**(R-U1-22/23) 위반 시 FILE_CONSTRAINT_VIOLATION → 400.
- **경로 이탈(path traversal) 방지**: `FileStorageService.load/delete`는 저장 시 발급한 서버 경로만 허용하고 사용자 입력 경로를 직접 결합하지 않는다. 정규화(canonical path) 후 저장 루트 하위인지 검증.
- `delete`는 멱등(대상 없으면 no-op) — 상위 유닛(U4/U5) 트랜잭션 보상용.

## 7. 위협 모델(STRIDE) 요약 & 파일럿 잔여 리스크

| 위협 | 방어(파일럿) | 잔여/확장 |
|---|---|---|
| Spoofing | 세션 재발급·BCrypt(NFR-SEC-1~3) | 타이밍 사이드채널(확장 상수시간) |
| Tampering | 서버측 Bean Validation·DTO 경계 | — |
| Repudiation | 프레임워크 기본 액세스 로그 | 상세 감사 로그(확장) |
| Info Disclosure | DTO·PII 최소화·에러 상세 비노출(R-U1-19) | — |
| DoS | (파일럿 보류) | 로그인 rate-limit/lockout(확장) |
| Elevation | RBAC·`@PreAuthorize`·isAdmin 시드 격리(R-U1-06/16) | — |

- **파일럿 하드 유지**: BCrypt, 세션 쿠키 보안 속성, 세션 고정 방지, 입력 검증, 최소 PII, DTO 경계, 시크릿 환경변수화, 파일 경로 이탈 방지.
- **보류(확장 시 재검토)**: TLS 종단, 파일 콘텐츠/바이러스 스캔, 의존성 SCA·시크릿 스캔, 로그인 rate-limit/계정 잠금(`cid:practices-discovery:c3`, NFR-5).
