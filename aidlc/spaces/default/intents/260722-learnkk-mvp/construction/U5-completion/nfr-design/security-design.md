# Security Design — U5 completion (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U5-completion
> 리드 architect · 관점 devsecops·compliance
> 상위 입력: `nfr-requirements/security-requirements.md`(인가·파일·무결성·PII), `functional-design/business-logic-model.md`(W-U5-1 종료·W-U5-2 보고서·W-U5-4 수료증), `nfr-requirements/tech-stack-decisions.md`
> 전제: U1의 인증·세션·FileStorageService·DTO 골격 상속.

U5의 핵심 보안 관심사는 **판정 무결성**과 **소유권/본인 스코프 인가**, **파일(보고서·수료증) 보안**이다.

## 1. 인가 아키텍처

`security-requirements.md` §1:
| 연산 | 인가 규칙 | 방어 지점 |
|---|---|---|
| endCohort(종료) | 소유 멘토만(R-U5-01), 진행중 상태만(R-U5-02) | 서비스 레이어 `cohort.mentorId == 요청자` → 403; status 검증 → 409 |
| ReportService.submit | 참여자(멘토·멘티)만(R-U5-15) | 참여 여부 서비스 검증 |
| historyOf | 참여자·관리자(R-U5-19) | 조회 권한 스코프 |
| certificateOf(조회/다운로드) | **본인 멘티·관리자만** | 요청자 세션 id로 스코프 + `certificate.menteeId == 요청자 or ADMIN` |

- **수평 권한 상승 방지**: 수료증 조회/다운로드는 클라이언트 id를 신뢰하지 않고 세션 id로 스코프. 본인 아닌 멘티 수료증 접근은 404/403(타인 수료증 열람 차단).
- **수직 권한**: 종료·정산 판정은 소유 멘토 액션 + 서버측 판정(멘티는 결과 조작 불가).

## 2. 판정 무결성(핵심)

`security-requirements.md` §4:
- **서버측 산술 판정**: 수료(출석률)·정산 판정은 전적으로 서버에서 정수 비교로 수행(`reliability-design.md` §3). 클라이언트는 판정 결과를 조작할 수 없다(요청에 판정 결과를 받지 않음).
- **멱등 종료**: 재종료 시 수료증 재발급 없음(INV-U5-1). 종료 상태 전이는 진행중→종료됨만 허용.

## 3. 파일 보안(보고서 첨부·수료증 이미지)

`security-requirements.md` §2:
- 보고서 첨부·수료증 이미지는 U1 FileStorageService(웹루트 밖·서버 UUID 파일명·경로 이탈 방지, R-U1-21~24) 사용.
- **보고서 첨부 검증**: U4와 동일하게 형식(허용 MIME)·크기(≤10MB) 검증. 매직바이트 강화가 필요하면 U4와 동일 패턴(서비스 step에서 수행) 적용 — 파일럿 기본은 U1 FileStorageService 기본 검증 + 형식 화이트리스트.
- **수료증 이미지**: 서버가 생성하는 신뢰 파일(사용자 업로드 아님)이라 업로드 검증 대상 아님. 저장·다운로드는 동일하게 웹루트 밖·권한 확인.
- **다운로드**: U1 `load` 경유, 저장 시 발급 경로만 허용. 권한 확인(§1) 후 스트리밍. `Content-Disposition`/`Content-Type` 정확 지정.

## 4. 데이터 보호 & 개인정보(compliance)

`security-requirements.md` §3:
- 응답 DTO(ReportDto/CertificateDto/CohortEndSummaryDto), Entity 미노출(U1 Mandated). ArchUnit DTO 경계 검증(U2~U4 일관).
- **수료증 임베드 PII 최소화**: 성명·코호트명·발급일만(추가 PII 미포함). 사내 도구·최소 수집 원칙 유지.
- 종료 결과 알림 message는 최소 정보(수료/미수료 사실), 민감정보 미포함.

## 5. 위협 모델(STRIDE) & 잔여 리스크

| 위협 | 방어(U5) |
|---|---|
| Spoofing | U1 세션/인증 |
| Tampering | 서버측 판정·상태 전이 검증(클라이언트 결과 조작 불가) |
| Info Disclosure | 본인 스코프(수료증)·DTO·PII 최소화 |
| DoS | 종료는 멘토 1회 액션(폭주 표면 낮음) |
| Elevation | 멘토 소유권(R-U5-01)·수료증 본인 스코프 |

- **잔여 리스크**: U1 상속(TLS·스캔·rate-limit 보류). U5 신규 보류 없음.
