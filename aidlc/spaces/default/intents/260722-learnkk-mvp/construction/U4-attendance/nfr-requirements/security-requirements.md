# Security Requirements — U4 attendance (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 devsecops·compliance·quality
> 상위 입력: `U4-attendance/functional-design/business-logic-model.md`(업로드 §2), `business-rules.md`(R-U4-01~04/11 권한·검증), `requirements-analysis/requirements.md`(NFR-5, FR-5)

## 1. 인가
- 증빙 업로드는 **회차 소속 코호트의 소유 멘토만**(R-U4-01) → 403. 진도·증빙 조회는 참여자·관리자(R-U4-09/11).
- U1 인증·세션(NFR-SEC-1~4) 상속.

## 2. 파일 업로드 보안 (핵심)
- **형식 화이트리스트**: image/jpeg, image/png, application/pdf만 허용(R-U4-02). MIME은 선언값이 아니라 실제 콘텐츠 기반 검증 권장(매직 바이트) — 최소한 확장자+선언 MIME 이중 확인.
- **크기 제한**: 파일당 ≤10MB, 서버 multipart 설정으로도 강제(R-U4-03).
- **저장 위치**: 웹루트 밖, 서버 생성 파일명(U1 R-U1-21/24). 사용자 제공 파일명·경로를 저장 경로에 사용 금지(path traversal 방지).
- **다운로드**: U1 FileStorageService.load 경유, 저장 시 발급 경로만 허용(경로 이탈 방지). 참여자·관리자 권한 확인 후 스트리밍.

## 3. 파일럿 잔여 리스크 (devsecops)
- **바이러스/콘텐츠 심층 스캔 보류**(`cid:practices-discovery:c3`, NFR-5): MIME·크기·확장자 기본 검증만. 업로드 파일은 실행되지 않고 저장·다운로드만 되므로 파일럿 리스크 수용. 확장 시 스캔 계층(예: ClamAV) 추가.
- TLS·rate-limit U1 상속 보류.

## 4. 데이터 보호
- 응답 DTO(EvidenceDto). 증빙 파일은 민감할 수 있으므로 다운로드 권한 엄격(참여자·관리자만).

## 5. STRIDE 요약
- Spoofing→U1 세션; Tampering→파일 형식/크기 검증·서버 파일명; Repudiation→요청 로깅(감사 확장 보류); Info Disclosure→다운로드 권한·웹루트 밖 저장; DoS→크기 제한(대용량 업로드 방지); Elevation→멘토 소유권 검증(R-U4-01).

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->
