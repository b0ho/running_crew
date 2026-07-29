# Security Design — U4 attendance (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 devsecops·compliance
> 상위 입력: `nfr-requirements/security-requirements.md`(인가·파일 업로드 보안·다운로드), `functional-design/business-logic-model.md`(W-U4-1 업로드·W-U4-3 다운로드·§6 계약), `nfr-requirements/tech-stack-decisions.md`
> 전제: U1의 인증·세션·FileStorageService(경로 이탈 방지) 골격 상속.

U4의 핵심 보안 관심사는 **파일 업로드/다운로드 보안**과 **소유권/권한 인가**다.

## 1. 인가 아키텍처

`security-requirements.md` §1:
- **증빙 업로드**: 회차 소속 코호트의 **소유 멘토만**(R-U4-01). 서비스 레이어에서 `cohort.mentorId == 요청자` 검증 → 아니면 403. 코호트 종료됨이면 409 COHORT_CLOSED.
- **진도·증빙 조회/다운로드**: 참여자(멘토·확정 멘티)·관리자만(R-U4-09/11). 다운로드는 권한 확인 후 스트리밍.
- U1 인증·세션(NFR-SEC-1~4)·`@PreAuthorize` 골격 상속.

## 2. 파일 업로드 보안(핵심)

`security-requirements.md` §2를 구체 검증 절차로 확정한다. 검증은 **파일 저장(트랜잭션) 전에** 수행(business-logic-model §2 step 1~2).

- **형식 화이트리스트**: `image/jpeg`, `image/png`, `application/pdf`만 허용(R-U4-02).
  - **이중 검증(확정)**: (a) 확장자 화이트리스트 + (b) **매직 바이트(콘텐츠 시그니처) 검증**을 병행한다. 선언 MIME(Content-Type 헤더)은 클라이언트가 위조 가능하므로 신뢰하지 않고, 파일 앞부분 시그니처(JPEG `FF D8 FF`, PNG `89 50 4E 47`, PDF `25 50 44 46`)를 실제 확인한다. 불일치 시 400 FILE_CONSTRAINT_VIOLATION.
  - **검증 책임 경계(확정, Finding 해소)**: **매직바이트 검증은 `AttendanceService.uploadEvidence` step 1(FileStorageService.store 호출 전)에서 U4가 수행**한다. U1 `FileStorageService.store`는 계약(R-U1-22) 그대로 **기본 검증(확장자 + 선언 MIME + 크기 ≤10MB)** 만 담당한다. 근거: 매직바이트 검증은 증빙 업로드에 특화된 강화 규칙이라 공통 FileStorage보다 도메인 서비스(U4)에 두는 것이 응집도가 높고, U1 계약을 변경하지 않아도 된다. 따라서 검증 순서는 `AttendanceService`(확장자+매직바이트+크기 선검사) → `FileStorageService.store`(기본 검증 재확인 + 저장)이며, 중복은 얕은 재확인 수준으로 허용(방어 심층화).
- **크기 제한**: 파일당 ≤10MB — Spring multipart 설정(`max-file-size=10MB`)과 서비스 검증 이중 강제(R-U4-03).
- **저장 위치·파일명**: U1 FileStorageService가 웹루트 밖 + 서버 생성 UUID 파일명으로 저장(R-U1-21/24 상속). **사용자 제공 파일명·경로를 저장 경로에 사용 금지**(path traversal 방지 — U1 canonical path 검증).
- **다운로드**: U1 `FileStorageService.load`가 저장 시 발급 경로만 허용(경로 이탈 방지). 원본 파일명은 `Content-Disposition`에 안전하게 인코딩(헤더 인젝션 방지), 브라우저 인라인 실행 억제를 위해 `Content-Type`을 정확히 지정하고 필요 시 `Content-Disposition: attachment`.

## 3. 파일럿 잔여 리스크(devsecops)

`security-requirements.md` §3:
- **바이러스/콘텐츠 심층 스캔 보류**(`cid:practices-discovery:c3`, NFR-5): MIME(매직바이트)·크기·확장자 기본 검증만. 근거: 업로드 파일은 **실행되지 않고 저장·다운로드만** 되며, 저장 경로는 웹루트 밖이라 서버에서 직접 서빙/실행되지 않음 → 파일럿 리스크 수용. 확장 시 스캔 계층(예: ClamAV) 추가.
- TLS·rate-limit U1 상속 보류.

## 4. 데이터 보호

- 응답 DTO(EvidenceDto), Entity 미노출(U1 Mandated). U2/U3와 동일 ArchUnit DTO 경계 검증.
- 증빙 파일은 민감할 수 있으므로 다운로드 권한을 엄격히 적용(참여자·관리자만). 파일 본체는 인증된 API 경유로만 접근(직접 URL 없음).

## 5. 위협 모델(STRIDE) & 잔여 리스크

| 위협 | 방어(U4) |
|---|---|
| Spoofing | U1 세션/인증 |
| Tampering | 파일 형식(매직바이트)/크기 검증 + 서버 생성 파일명 |
| Info Disclosure | 다운로드 권한 + 웹루트 밖 저장 + 정확한 Content-Type/Disposition |
| DoS | 크기 상한(10MB) + multipart 서버 강제 |
| Elevation | 멘토 소유권 서비스 검증(R-U4-01) |

- **잔여 리스크**: 파일 콘텐츠 심층 스캔 보류(확장). U4 신규 하드 제약: 매직바이트 형식 검증, 크기 상한 서버 강제, 다운로드 권한.
