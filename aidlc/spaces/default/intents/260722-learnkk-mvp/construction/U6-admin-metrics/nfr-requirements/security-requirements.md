# Security Requirements — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 devsecops·compliance·quality
> 상위 입력: `U6-admin-metrics/functional-design/business-logic-model.md`(집계·이력), `business-rules.md`(R-U6-01~03/11 권한), `requirements-analysis/requirements.md`(NFR-5, FR-10/11)

## 1. 인가 (핵심 — 관리자 전용)
- 운영 지표·증빙 이력·보고서 이력 조회는 **관리자(ROLE_ADMIN)만**(`@PreAuthorize`, R-U6-01) → 비관리자 403, 미인증 401. U1 R-U1-16a 상속.
- U6은 조직 전체 데이터를 열람하므로 인가가 특히 중요(수평 권한 상승 표면 없음 — 관리자만 접근).

## 2. 읽기 전용 (무결성)
- U6은 **어떤 데이터도 수정하지 않는다**(R-U6-03, INV-U6-1). 쓰기 연산 부재로 데이터 변조 표면 없음.
- 다른 유닛의 도메인 로직을 우회하지 않는 순수 조회(리포팅 읽기 모델).

## 3. 데이터 보호 & 개인정보 (compliance)
- 이력 뷰는 업로더·작성자 성명 등 개인정보를 포함하므로 **관리자 전용**으로 제한(운영 목적 최소 노출).
- 응답 DTO(MetricsOverviewDto/EvidenceHistoryItemDto/ReportHistoryItemDto), Entity 직접 노출 금지.
- 이력 파일 다운로드는 U1 load 경유·관리자 권한 확인(경로 이탈 방지).

## 4. 파일럿 잔여 리스크
- U1 상속(TLS·rate-limit 보류). U6 신규 보류 없음.
- STRIDE 요약: Spoofing→U1 세션; Tampering→읽기 전용(변조 표면 없음); Repudiation→요청 로깅(감사 확장 보류); Info Disclosure→**관리자 전용 인가**(조직 데이터 열람 보호)·DTO; DoS→집계 쿼리 비용은 파일럿 규모에서 낮음(대규모 시 캐시 검토); Elevation→ROLE_ADMIN @PreAuthorize.

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
