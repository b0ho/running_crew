# Security Requirements — U5 completion (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U5-completion
> 리드 architect · 관점 devsecops·compliance·quality
> 상위 입력: `U5-completion/functional-design/business-logic-model.md`(종료·보고서), `business-rules.md`(R-U5-01/15~19 권한·검증), `requirements-analysis/requirements.md`(NFR-5, FR-7/8/9)

## 1. 인가
- 코호트 종료(endCohort)는 **소유 멘토만**(R-U5-01) → 403. 진행중 상태만 종료(R-U5-02).
- 최종 보고서 제출은 참여자(멘토·멘티), 조회는 참여자·관리자(R-U5-15/19).
- 수료증 조회/다운로드는 **본인 멘티·관리자만**(certificateOf). 타인 수료증 접근 차단(수평 권한 스코프).
- U1 인증·세션(NFR-SEC-1~4) 상속.

## 2. 파일 보안 (보고서 첨부·수료증)
- 보고서 첨부·수료증 이미지는 U1 FileStorageService(웹루트 밖·서버 파일명·형식/크기 검증, R-U1-21~24). 다운로드는 load 경유·권한 확인.
- 보고서 첨부 형식/크기는 U1 파일 제약 적용(U4와 동일). 바이러스 스캔은 파일럿 보류(U1 상속).

## 3. 데이터 보호 & 개인정보 (compliance)
- 응답 DTO(ReportDto/CertificateDto/CohortEndSummaryDto). Entity 직접 노출 금지.
- 수료증에 임베드하는 개인정보는 성명·코호트명·발급일로 최소화(추가 PII 미포함).
- 종료 판정 결과 알림 message는 최소 정보(민감정보 미포함).

## 4. 무결성
- 수료/정산 판정은 서버측 산술(정수 비교), 클라이언트가 결과를 조작할 수 없음. 종료는 멱등(재종료 시 증서 재발급 없음, INV-U5-1).

## 5. 파일럿 잔여 리스크
- U1 상속(TLS·스캔·rate-limit 보류). U5 신규 보류 없음.
- STRIDE 요약: Spoofing→U1 세션; Tampering→서버 판정·상태전이 검증; Repudiation→요청 로깅(감사 확장 보류); Info Disclosure→본인 스코프·DTO·PII 최소; DoS→종료는 멘토 1회 액션(폭주 표면 낮음); Elevation→멘토 소유권(R-U5-01)·수료증 본인 스코프.

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
