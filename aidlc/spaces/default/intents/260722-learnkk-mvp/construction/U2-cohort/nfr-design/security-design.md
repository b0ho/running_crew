# Security Design — U2 cohort (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 devsecops·compliance
> 상위 입력: `nfr-requirements/security-requirements.md`(소유권 인가·입력검증·외부링크), `functional-design/business-logic-model.md`(W-U2-2 소유 확인·W-U2-6 공지), `nfr-requirements/tech-stack-decisions.md`
> 전제: U1의 인증·RBAC·세션·에러·DTO 골격 상속.

U2는 신규 인증 메커니즘을 도입하지 않고 **소유권 기반 인가(리소스 레벨)** 를 핵심 보안 관심사로 설계한다.

## 1. 인가 아키텍처 — 소유권 검증(핵심)

`security-requirements.md` §1의 소유권 요구를 구체 설계로 확정한다.

- **소유 멘토 검증**: 코호트 수정(`update`)·시작(`start`)·공지 작성(`AnnouncementService.create`)은 `cohort.mentorId == 인증 사용자 id` 를 **서버 서비스 레이어에서 검증**한다(R-U2-07/15). 위반 시 403 FORBIDDEN. UI 숨김은 보조 방어일 뿐 신뢰 경계 아님.
- **검증 위치 결정**: 소유권 검증은 컨트롤러가 아닌 **서비스 레이어**에서 수행한다(도메인 규칙이므로). `@PreAuthorize`(U1)는 "인증 사용자/관리자" 수준의 coarse-grained 인가만 담당하고, "이 코호트의 소유자인가"는 데이터 의존 검증이라 서비스에서 처리한다.
- **조회 인가**: 목록/탐색은 인증 사용자(모집중·진행중 노출), 상세·공지는 참여자·관리자, 종료됨 코호트는 참여 이력자·관리자만(R-U2-18/19/20).

### 인가 결정 흐름(update 예)
```
update(userId, cohortId)
  ├─ 코호트 미존재 ─> 404 NOT_FOUND
  ├─ cohort.mentorId != userId ─> 403 FORBIDDEN
  ├─ status == 종료됨 ─> 409 COHORT_CLOSED
  └─ 통과 ─> 도메인 검증 -> save
```
<!-- Text fallback: update는 코호트 미존재 시 404, 소유자가 아니면 403, 종료됨이면 409를 반환하고, 통과 시 도메인 검증 후 저장한다. -->

## 2. 입력 검증

`security-requirements.md` §2:
- title·capacity·sessionCount·날짜에 서버측 Bean Validation(`@Valid`, R-U2-01~04). 클라이언트 검증은 보조.
- 공지 body 필수(R-U2-16). 날짜 논리 검증(시작<종료 등)은 도메인 검증.

## 3. 외부 링크 보안(SSRF/XSS 표면 관리)

`security-requirements.md` §3:
- `externalLink`는 멘토가 입력하는 외부 미팅 URL이며 **플랫폼이 서버에서 대신 fetch하지 않는다** → SSRF 표면 없음.
- 저장 시 **스킴 화이트리스트(http/https)만 허용**, `javascript:`·`data:`·`file:` 등 위험 스킴 거부(R-U2-17).
- **검증 메커니즘(확정)**: `AnnouncementCreateReq.externalLink` 필드에 **커스텀 Bean Validation 애너테이션 `@SafeExternalUrl`** 을 적용한다(서버측 강제). 검증기 로직:
  1. null·빈 문자열은 허용(externalLink는 선택 필드).
  2. `java.net.URI.create(value)`로 파싱(파싱 예외 시 invalid).
  3. `uri.getScheme()`을 소문자화하여 화이트리스트 `{"http","https"}` 포함 여부 확인. 미포함(`javascript`, `data`, `file`, null 스킴 등)이면 invalid.
  4. host가 존재하는지(절대 URL) 확인.
  - 위반 시 Bean Validation이 400 VALIDATION_ERROR로 매핑(공통 에러 핸들러). 별도 URL 검증 라이브러리 없이 표준 `java.net.URI`로 구현하여 의존성 최소화.
```java
// 개념 예시 (@SafeExternalUrl 검증기)
if (value == null || value.isBlank()) return true;      // 선택 필드
try {
  var scheme = java.net.URI.create(value).getScheme();
  return scheme != null
      && java.util.Set.of("http","https").contains(scheme.toLowerCase())
      && java.net.URI.create(value).getHost() != null;
} catch (IllegalArgumentException e) { return false; }
```
- FE는 외부 링크를 `rel="noopener noreferrer"` + `target="_blank"` 새 탭으로 열어 opener 하이재킹·리퍼러 유출 방지.

## 4. 데이터 보호

- 응답은 DTO 경계(CohortDto/CohortDetailDto/AnnouncementDto), JPA Entity 미노출(INV-U2-4, NFR-7, U1 Mandated 상속).
- **DTO 경계 강제 메커니즘**: INV-U2-4(Entity 미노출)를 사람 리뷰에만 의존하지 않도록, 컨트롤러 반환 타입이 `@Entity` 클래스를 직접 노출하지 않음을 **ArchUnit 규칙 테스트**로 검증한다(예: "controller 메서드 반환 타입은 entity 패키지에 속하지 않는다"). code-generation은 이 아키텍처 테스트를 U2 테스트 스위트에 포함한다.
- 코호트·공지에 민감 PII 없음(멘토/참여자 식별은 id 참조). 개인정보 최소 수집 원칙(U1) 유지.

## 5. 위협 모델(STRIDE) & 파일럿 잔여 리스크

| 위협 | 방어(U2) |
|---|---|
| Elevation | 소유권 서비스 검증(R-U2-07/15) + U1 RBAC |
| Tampering | 서버측 Bean Validation |
| Info Disclosure | DTO 경계 + 조회 권한(R-U2-18~20) |
| XSS(외부링크) | 스킴 화이트리스트 + FE noopener |

- **잔여 리스크**: U1과 동일하게 TLS·rate-limit 등 파일럿 보류 상속. U2 신규 보류 없음(`security-requirements.md` §5).
