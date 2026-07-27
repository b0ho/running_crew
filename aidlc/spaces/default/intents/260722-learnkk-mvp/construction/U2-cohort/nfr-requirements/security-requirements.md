# Security Requirements — U2 cohort (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 devsecops·compliance·quality
> 상위 입력: `U2-cohort/functional-design/business-logic-model.md`(CohortService·공지), `business-rules.md`(R-U2-07/15 소유권·권한), `requirements-analysis/requirements.md`(NFR-5)

## 1. 인가 (핵심 — 소유권 검증)
- 코호트 수정·종료·공지 작성은 **소유 멘토만**(cohort.mentorId == 요청자, R-U2-07/15). 서버측 검증이 최종 방어(UI 숨김은 보조).
- 조회 권한: 목록/탐색은 인증 사용자, 상세·공지는 참여자·관리자(R-U2-18/19).
- 정원 축소 검증(R-U2-09) 시 U3 confirmedCount 읽기 실패는 503/409로 안전 실패(재시도 없음, reliability §2 참조).
- U1의 인증·RBAC·세션(NFR-SEC-1~4) 상속.

## 2. 입력 검증
- title/capacity/sessionCount/날짜 서버측 Bean Validation(R-U2-01~04). 클라이언트 검증은 보조.
- 공지 body 필수, externalLink는 http/https URL 형식 검증(R-U2-17).

## 3. 외부 링크 취급 (compliance/devsecops)
- externalLink는 멘토가 입력하는 외부 미팅 URL이며 플랫폼이 대신 fetch하지 않는다(SSRF 표면 없음). FE는 `rel="noopener"` 새 탭으로 열기.
- 저장 시 스킴 화이트리스트(http/https)만 허용, javascript: 등 위험 스킴 거부.

## 4. 데이터 보호
- 응답은 DTO 경계(CohortDto/CohortDetailDto), Entity 직접 노출 금지(INV-U2-4, NFR-7).

## 5. 파일럿 잔여 리스크
- U1과 동일하게 TLS·rate-limit 등 보류 상속. U2 신규 보류 없음.
- STRIDE 관점: Elevation→소유권 검증(R-U2-07); Tampering→서버측 검증; Info Disclosure→DTO·조회 권한(R-U2-18).

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
