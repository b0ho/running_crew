# Security Requirements — U1 foundation (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 devsecops(보안)·compliance(개인정보)·quality(검증)
> 상위 입력: `U1-foundation/functional-design/business-logic-model.md`(인증·시드·에러 흐름), `business-rules.md`(R-U1-05/09/12~16 인증·인가), `requirements-analysis/requirements.md`(NFR-5 보안, FR-1)
> 근거 관행: project.md Mandated(BCrypt·DTO·최소 PII), Forbidden(평문 저장·SSO·클라우드), Scope Overrides(`cid:practices-discovery:c3` 보안 위생 보류)

## 1. 인증 (Authentication) — NFR 보안 요구

| ID | 요구 |
|---|---|
| NFR-SEC-1 | 비밀번호는 **BCrypt 해싱** 저장(평문·가역 암호화 금지, business-rules R-U1-05). **cost는 application 설정값으로 외부화**(예: `security.bcrypt.cost`), **기본값 10**. cost는 배포 환경에서 조정 가능하되 8 미만 금지. 성능 SLO(performance-requirements)는 기본값 10 기준으로 측정 |
| NFR-SEC-2 | 로그인 세션은 **HttpOnly·SameSite=Lax·(운영 TLS 시)Secure 쿠키**. 무상태 JWT 미강제(project.md ## Tech Stack) |
| NFR-SEC-3 | **세션 고정 방지**: 로그인 성공 시 세션 ID를 재발급한다(Spring Security `sessionFixation().newSession()` 또는 `changeSessionId()`). 이는 본 NFR 단계에서 도입하는 보안 요구이며 code-generation이 인증 성공 처리에 포함해야 한다 |
| NFR-SEC-4 | 사용자 열거 방지: 미존재 계정·비밀번호 불일치를 동일 401 응답(business-rules R-U1-09). 타이밍 사이드채널은 파일럿 잔여 리스크(확장 시 상수시간 방어) |

## 2. 인가 (Authorization)
- RBAC: 인증 사용자 ROLE_USER, `isAdmin` ROLE_ADMIN. 관리자 경로는 `@PreAuthorize`(R-U1-16a).
- 관리자 권한은 회원가입으로 부여 불가, Flyway 시드로만 부트스트랩(R-U1-16).
- 회원가입 요청에 isAdmin 필드 부재(DTO 스키마 배제, R-U1-06) — 권한 상승 차단.

## 3. 데이터 보호 & 개인정보 (compliance 관점)
- 수집 PII 최소화: 이메일·성명·닉네임만(사번 등 미수집, project.md Mandated). 사내 도구로 외부 판매 없음.
- User.passwordHash는 어떤 API 응답에도 미포함(INV-1). 응답은 DTO 경계(NFR-7).
- 시드 비밀번호 등 시크릿은 환경변수 주입, 코드/마이그레이션에 커밋 금지(construction 가드레일).

## 4. 파일 저장 보안 (공통 인프라)
- 저장 파일은 웹루트 밖, 서버 생성 파일명(R-U1-21/24). MIME·크기 검증(R-U1-22/23).
- `FileStorageService.load/delete`는 **경로 이탈(path traversal) 방지**를 강제한다(저장 시 발급한 서버 경로만 허용, 사용자 입력 경로 직접 사용 금지). delete는 멱등(business-logic-model §7).

## 5. 파일럿 잔여 리스크 & 확장 계획 (devsecops)
- **보류(확장 시 재검토)**: 전송 구간 TLS, 파일 바이러스/콘텐츠 스캔, 의존성 SCA·시크릿 스캔, 로그인 rate-limit/lockout(`cid:practices-discovery:c3`, NFR-5).
- **파일럿 하드 유지**: BCrypt, 세션 쿠키 보안 속성, 입력 검증(Bean Validation), 최소 PII, DTO 경계, 시크릿 환경변수화.
- 위협 모델(STRIDE 요약): Spoofing→세션 재발급·BCrypt(NFR-SEC-1~3); Tampering→서버측 Bean Validation·DTO 경계; Repudiation→**기본 애플리케이션 요청 로깅 수준**(상세 감사 로그는 확장 보류 — 파일럿에 앱 감사 로그 요구 없음); Info Disclosure→DTO·PII 최소화·에러 상세 비노출; DoS→파일럿 보류(rate-limit은 확장); Elevation→RBAC·isAdmin 시드 격리(R-U1-06/16).
- 명확화: 위 "요청 로깅"은 프레임워크 기본 액세스 로그 수준이며, 부인방지용 상세 감사 로그(누가 언제 무엇을)는 파일럿 범위 외(확장 시 도입).

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
