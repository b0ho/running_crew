# Security Design — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 관점 devsecops·compliance
> 상위 입력: `nfr-requirements/security-requirements.md`(인가·본인 스코프·남용 방지), `functional-design/business-logic-model.md`(W-U3-1 self-enrollment·W-U3-5 관리자 승인·W-U3-2/7 본인 스코프)
> 전제: U1의 인증·RBAC·세션·에러·DTO 골격 상속.

U3의 핵심 보안 관심사는 **인가(수직·수평 권한 상승 방지)** 와 **남용해도 정합성이 유지되는 구조**다.

## 1. 인가 아키텍처

`security-requirements.md` §1의 요구를 구체 방어 지점으로 확정한다.

| 연산 | 인가 규칙 | 방어 지점 |
|---|---|---|
| join(참여 신청) | 인증 사용자만(R-U3-01) | `@PreAuthorize("isAuthenticated()")` + 서비스 |
| self-enrollment 차단 | 자기 코호트 멘토는 멘티 참여 불가(R-U3-05) | **서비스 레이어**에서 `cohort.mentorId == 요청자` 검사 → 409 SELF_ENROLLMENT (데이터 의존 규칙) |
| listWaiting/approve/reject | 관리자만(R-U3-11) | `@PreAuthorize("hasRole('ADMIN')")`(U1 R-U1-16a 상속) |
| myApplications | 본인만(R-U3-16/17) | 요청자 세션 id로 쿼리 스코프(파라미터 menteeId 무시·세션 id 강제) |
| notification listFor/markRead | 본인만(R-U3-19) | 요청자 세션 id 스코프 + markRead 시 `notification.userId == 요청자` 검사 |

- **수평 권한 상승 방지(핵심)**: `myApplications`·알림 조회/읽음은 **클라이언트가 보낸 id를 신뢰하지 않고 세션의 인증 사용자 id로 스코프**한다. markRead는 대상 Notification의 소유자가 요청자인지 확인 후 처리(아니면 404/403) — 타인 알림 열람·조작 차단.
- **수직 권한 상승 방지**: 승인/거절은 `@PreAuthorize` coarse-grained(ADMIN) + 상태 전이 규칙. 멘티는 자신의 상태를 변경할 수 없다(승인/거절은 관리자 전용, 대기 취소는 파일럿 범위 외 `cid:user-stories:c4`).

## 2. 데이터 보호 & 무결성

- 응답 DTO 경계(EnrollmentDto/NotificationDto/JoinResultDto), Entity 미노출(INV-U3-4, U1 Mandated 상속). U2와 동일하게 ArchUnit로 컨트롤러 반환 타입이 entity를 노출하지 않음을 검증.
- 상태 변경(승인/거절)은 관리자만. 멘티의 상태 자가 변경 불가.

## 3. 남용 방지(파일럿 수준)

`security-requirements.md` §3:
- **이중 제출(더블클릭)**: UNIQUE(cohortId, menteeId)로 구조적 차단(R-U3-08) → 409 ALREADY_ENROLLED. 폼 레벨 중복 제출 방지(버튼 비활성)는 UX 보조.
- **신청 폭주(자동화 남용)**: rate-limit은 U1과 동일하게 파일럿 보류(확장 시 도입). **단, 정원 초과 확정은 비관적 락으로 구조적 방지되므로, 신청을 남용해도 정합성(INV-U3-1)은 유지**된다 — 남용의 영향이 정합성 붕괴가 아니라 무해한 대기(WAITING) 축적으로 한정됨(`reliability-design.md` §1).

## 4. 개인정보(compliance)

- 알림 message에 최소 정보만 담고 민감정보 미포함(`security-requirements.md` §4). Notification은 수신자 userId 스코프로만 조회.
- Enrollment는 menteeId·cohortId 참조만(추가 PII 없음). U1 최소 수집 원칙 유지.

## 5. 위협 모델(STRIDE) & 잔여 리스크

| 위협 | 방어(U3) |
|---|---|
| Spoofing | U1 세션/인증 |
| Tampering | 서버측 상태 전이 규칙(조건부 UPDATE, R-U3-12) |
| Info Disclosure | 본인 스코프(세션 id) + DTO 경계 |
| DoS | 락 기반 정합성 유지(정원 초과 불가), rate-limit은 확장 보류 |
| Elevation | self-enrollment 차단 + 관리자 승인 `@PreAuthorize` + 수평 스코프 강제 |

- **잔여 리스크**: U1과 동일 TLS·rate-limit 보류 상속. U3 신규 하드 제약: self-enrollment 서비스 검증, 본인 스코프 강제, 관리자 승인 인가.
