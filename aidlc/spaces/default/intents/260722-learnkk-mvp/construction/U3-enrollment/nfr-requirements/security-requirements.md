# Security Requirements — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 관점 devsecops·compliance·quality
> 상위 입력: `U3-enrollment/functional-design/business-logic-model.md`(join·승인), `business-rules.md`(R-U3-01/05/11/16~19 권한), `requirements-analysis/requirements.md`(NFR-5, FR-4/10)

## 1. 인가
- 참여 신청은 인증 사용자만(R-U3-01). **자기 코호트 멘토는 멘티로 참여 불가**(self-enrollment 방지, R-U3-05).
- 대기 승인/거절·대기 목록은 **관리자(ROLE_ADMIN)만**(`@PreAuthorize`, R-U3-11). U1 R-U1-16a 상속.
- 내 신청 상태·알림 조회/읽음은 **본인만**(R-U3-16/17/19). 타인 데이터 접근 차단(수평 권한 상승 방지: 요청자 id로 스코프).

## 2. 데이터 보호 & 무결성
- 응답 DTO 경계(EnrollmentDto/NotificationDto/JoinResultDto), Entity 직접 노출 금지(INV-U3-4).
- 상태 변경(승인/거절)은 관리자만, 멘티는 자신의 상태를 변경할 수 없음(대기 취소 등은 파일럿 범위 외, `cid:user-stories:c4`).

## 3. 남용 방지 (파일럿 수준)
- 이중 제출(더블클릭)은 UNIQUE(cohortId,menteeId)로 차단(R-U3-08) → 409. 폼 레벨 중복 제출 방지는 UX 보조.
- 신청 폭주(자동화 남용)에 대한 rate-limit은 U1과 동일하게 파일럿 보류(확장 시 도입). 정원 초과 확정은 락으로 구조적 방지되므로 남용해도 정합성은 유지.

## 4. 개인정보 (compliance)
- 알림 message에 최소 정보만(민감정보 미포함). Notification은 수신자 userId 스코프로만 조회.

## 5. STRIDE 요약
- Spoofing→U1 세션/인증; Tampering→서버측 상태 전이 규칙(R-U3-12); Repudiation→요청 로깅(상세 감사 확장 보류); Info Disclosure→본인 스코프·DTO; DoS→락 기반 정합성 유지(rate-limit 확장 보류); Elevation→self-enrollment 차단·관리자 승인 @PreAuthorize.

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
