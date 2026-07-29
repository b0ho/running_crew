# Security Design — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 관점 devsecops·compliance
> 상위 입력: `nfr-requirements/security-requirements.md`(관리자 전용 인가·읽기 전용·PII), `functional-design/business-logic-model.md`(W-U6-1~3 관리자 전용·읽기 전용), `nfr-requirements/tech-stack-decisions.md`
> 전제: U1 인증·RBAC·세션·DTO 골격 상속.

U6의 핵심 보안 관심사는 **관리자 전용 인가**(조직 전체 데이터 열람 보호)와 **읽기 전용(변조 표면 없음)** 이다.

## 1. 인가 아키텍처(핵심 — 관리자 전용)

`security-requirements.md` §1:
- 운영 지표(overview)·증빙 이력·보고서 이력 조회는 모두 **관리자(ROLE_ADMIN)만** — `@PreAuthorize("hasRole('ADMIN')")`(R-U6-01, U1 R-U1-16a 상속). 비관리자 403, 미인증 401.
- **U6은 조직 전체 데이터를 열람**하므로 인가가 특히 중요하다. 수평 권한 상승 표면은 없다(관리자만 접근하며, 관리자는 전체 데이터 열람이 정당한 역할). 개별 사용자 스코프가 아니라 역할 스코프.

## 2. 읽기 전용 무결성

`security-requirements.md` §2:
- **U6은 어떤 데이터도 수정하지 않는다**(R-U6-03, INV-U6-1). 쓰기 연산 부재 → 데이터 변조 표면 없음.
- U6 `MetricsRepository`/`HistoryService`는 소스 스키마를 **읽기만** 하는 리포팅 읽기 모델. 다른 유닛의 도메인 로직·검증을 우회하는 쓰기 경로가 없다(순수 조회).
- 트랜잭션은 `@Transactional(readOnly = true)`로 표시(쓰기 차단 + 성능).

## 3. 데이터 보호 & 개인정보(compliance)

`security-requirements.md` §3:
- 이력 뷰는 **업로더·작성자 성명** 등 개인정보를 포함하므로 관리자 전용으로 제한(운영 목적 최소 노출). 일반 사용자에게 조직 전체 이력·타인 성명을 노출하지 않음.
- 응답 DTO(MetricsOverviewDto/EvidenceHistoryItemDto/ReportHistoryItemDto), Entity 미노출(U1 Mandated). ArchUnit DTO 경계 검증(타 유닛 일관).
- 이력 파일 다운로드는 U1 `FileStorageService.load` 경유 + 관리자 권한 확인(경로 이탈 방지 상속).

## 4. 위협 모델(STRIDE) & 잔여 리스크

| 위협 | 방어(U6) |
|---|---|
| Spoofing | U1 세션/인증 |
| Tampering | 읽기 전용(변조 표면 없음) |
| Info Disclosure | **관리자 전용 인가**(조직 데이터 열람 보호) + DTO |
| DoS | 집계 쿼리 비용 파일럿 규모에서 낮음(대규모 시 캐시 검토) |
| Elevation | ROLE_ADMIN `@PreAuthorize` |

- **잔여 리스크**: U1 상속(TLS·rate-limit 보류). U6 신규 보류 없음.
