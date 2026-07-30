# 보안 테스트 지침 (security-test-instructions) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · **DEVSECOPS 관점 입력** · 리드 QUALITY
> 입력: `nfr-requirements/security-requirements.md`, `nfr-design/security-design.md`, project.md(Forbidden/Mandated/Scope Overrides), U1/U3/U4 `code-summary.md`
> 원칙: 파일럿 하드 제약(인증·인가·입력검증·파일 매직바이트·BCrypt·최소 PII·시크릿 환경변수화)은 **테스트로 검증**, 보안 위생(SAST/DAST/SCA/TLS)은 **파일럿 보류 + 확장 시 도입 명시**(`cid:practices-discovery:c3`).

## 1. 인증/인가 테스트 (authentication / authorization)

### 1.1 인증(Authentication)
- **BCrypt 저장 검증**: 가입/시드 후 저장 비밀번호가 `$2a$` 접두 해시(평문/가역 금지, NFR-SEC-1, project.md Mandated·Forbidden). 라이브 스모크에서 DB의 `password_hash` 접두 확인(U1 `code-summary.md` §3).
- **사용자 열거 방지**: 미존재 계정·비밀번호 불일치 모두 동일 401 `INVALID_CREDENTIALS`(R-U1-09). 단위: `AuthServiceTest`(미존재/불일치 동일 예외). FE: `LoginForm` 동일 문구 상수.
- **세션 쿠키 속성**: `HttpOnly`·`SameSite=Lax`·(운영 TLS 시)`Secure`(NFR-SEC-2). 로그인 성공 시 **세션 고정 방지**(changeSessionId, NFR-SEC-3) — `SecurityConfig` + `AuthController.login`.

### 1.2 인가(Authorization) — 관리자 전용·수평 권한 상승 방지
- **관리자 전용 `@PreAuthorize`**: 관리자 경로(`/api/admin/**`, `/api/admin/metrics`, `/api/admin/history/**`, 대기 승인)는 메서드/클래스 레벨 `@PreAuthorize("hasRole('ADMIN')")`. 비관리자 403·미인증 401(R-U1-16a, R-U6-01/02, R-U3-11).
  - 검증: 관리자 세션→200, 일반 사용자→403, 미인증→401(U1 라이브 스모크에서 확인, CI 통합 테스트로 재확인).
- **권한 상승 차단**: 회원가입 DTO에 `isAdmin` 필드 부재(R-U1-06), 관리자 부트스트랩은 Flyway 시드로만. `AuthServiceTest`가 isAdmin 강제 false 검증.
- **수평 권한 상승 방지(본인 스코프)**: 
  - 내 신청/알림 조회·읽음은 세션 사용자 id로 스코프(`CurrentUserProvider`). `NotificationService.markRead`는 `findByIdAndUserId`로 소유 확인 → 타인 알림 404(`NotificationServiceTest`, R-U3-19).
  - 수료증 다운로드는 요청자 세션 id 스코프 → 타인 수료증 404(U5 `certificateOf`).
  - 증빙 진도/다운로드는 소유 멘토·확정 멘티(U3 조회)·관리자만(`AttendanceServiceTest`, R-U4-09/11).
  - 코호트 수정/종료/공지 작성은 소유 멘토만 403 가드(`CohortServiceTest`, `CompletionServiceTest`).

## 2. 입력 검증 / 파일 업로드 검증 (input validation)

- **서버측 Bean Validation**: 모든 API 경계 DTO `@Valid`(R-U1-01~04·07 등). 위반 시 400 `VALIDATION_ERROR`. Entity 직접 노출 금지(DTO 경계, ArchUnit 강제).
- **외부 링크 검증**: 공지 `externalLink`는 `@SafeExternalUrl`로 스킴 화이트리스트{http,https} + host 존재 확인. `javascript:`/`data:`/`file:`/상대URL 거부 → 400(`SafeExternalUrlValidatorTest`, security-design §3).
- **파일 업로드 검증(핵심)**:
  - **매직바이트 교차 검증**: 업로드 파일은 store **전에** `FileSignatureValidator`가 JPEG(FF D8 FF)/PNG(89 50 4E 47)/PDF(25 50 44 46) 시그니처를 확인하고 선언 MIME과 교차 검증. 확장자만 위조한 텍스트 파일은 거부(400) — `FileSignatureValidatorTest`(7건, 위조/크기/빈 파일 거부).
  - **크기·MIME 제한**: 10MB 상한, 허용 MIME만(R-U1-22/23).
  - **경로 이탈 방지**: `FileStorageService.load/delete`는 canonical path 검증으로 저장 루트 하위만 허용, 사용자 입력 경로 직접 결합 금지(R-U1-21/24, `FileStorageServiceTest` 경로 이탈 거부).
  - **파일명 안전**: 서버 생성 UUID 파일명·다운로드 시 `Content-Disposition: attachment` + 서버 생성 파일명(헤더 인젝션 방지, U4).

## 3. 비밀번호·시크릿·PII

- **BCrypt(하드 제약)**: cost 외부화(`security.bcrypt.cost`, 기본 10, 하한 8). 가입·로그인·시드 모두 동일 인코더(§1.1).
- **시크릿 환경변수화**: 시드 비밀번호·DB 비밀번호는 환경변수 주입, 코드/마이그레이션 커밋 금지(construction 가드레일). `.env`는 `.gitignore`.
- **최소 PII**: 이메일·성명·닉네임만 수집(사번 등 미수집, project.md Mandated). `passwordHash`는 어떤 응답 DTO에도 미포함(INV-1). 지표/이력의 파일 원경로(filePath/imagePath) 미노출(U6, security-design §3).

## 4. 파일럿 보류 항목 + 확장 시 도입 (devsecops 명시)

파일럿에서는 아래 보안 위생 항목을 **보류**한다(`cid:practices-discovery:c3`, project.md Scope Overrides, security-design §5/§7). 확장(클라우드 이관·전사 도입) 시 도입한다.

| 항목 | 파일럿 상태 | 확장 시 도입 |
|---|---|---|
| **SAST**(정적 분석) | 보류 | CI에 정적 분석 통합(코드 취약점 스캔) |
| **DAST**(동적 분석) | 보류 | 스테이징 대상 동적 취약점 스캔 |
| **SCA**(의존성 취약점) | 보류 | 의존성 스캔 + 시크릿 스캔 CI 게이트 |
| **TLS 종단** | 보류(로컬) | 역방향 프록시에서 TLS 종단, 쿠키 `Secure` 활성 |
| 파일 바이러스/콘텐츠 심층 스캔 | 보류(매직바이트만) | ClamAV 등 콘텐츠 스캔 |
| 로그인 rate-limit / 계정 잠금 | 보류 | rate-limit·lockout 도입 |
| 상세 감사 로그(부인방지) | 보류(기본 액세스 로그) | 누가·언제·무엇을 감사 로그 |
| 타이밍 사이드채널 상수시간 방어 | 잔여 리스크 | 상수시간 비교 도입 |

> STRIDE 요약(security-design §7): Spoofing→세션 재발급·BCrypt / Tampering→Bean Validation·DTO 경계 / Info Disclosure→DTO·PII 최소화·에러 상세 비노출 / Elevation→RBAC·`@PreAuthorize`·isAdmin 시드 격리. DoS(rate-limit)는 파일럿 보류.

## 5. 실행 방법 (보안 관련 테스트)

```bash
# 파일 매직바이트·경로 이탈·외부 링크·인증/인가 단위 검증(로컬)
cd learnkk-api && ./gradlew test -PexcludeIntegration \
  --tests "com.learnkk.attendance.FileSignatureValidatorTest" \
  --tests "com.learnkk.file.FileStorageServiceTest" \
  --tests "com.learnkk.common.validation.SafeExternalUrlValidatorTest" \
  --tests "com.learnkk.auth.AuthServiceTest" \
  --tests "com.learnkk.enrollment.NotificationServiceTest"
```

- 인가 경계(403/401)의 end-to-end 검증은 통합 테스트/라이브 스모크(Docker 가용 CI)에서 수행한다.
- 본 스테이지 로컬 결과: 위 보안 단위 테스트는 전체 백엔드 단위 스위트(107건)에 포함되어 통과(build-test-results.md). end-to-end 인가는 CI 위임.

## 6. 상위 산출물 참조

- 보안 요구·위협 모델: `nfr-requirements/security-requirements.md`, `nfr-design/security-design.md`.
- 파일 검증·경로 이탈·매직바이트 구현: U1 `code-summary.md`(FileStorageService), U4 `code-summary.md`(FileSignatureValidator).
- 인가·본인 스코프: U3/U4/U5/U6 `code-summary.md` 보안 섹션.
