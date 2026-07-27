# Tech Stack Decisions — U4 attendance (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 devsecops·quality
> 상위 입력: `U4-attendance/functional-design/business-logic-model.md`(업로드·인증), `business-rules.md`(R-U4-*), `requirements-analysis/requirements.md`(NFR-5)

## 1. 상속 스택
U1-foundation 표준 스택 상속. 파일 저장은 U1 FileStorageService(store/load/delete) 사용.

## 2. U4 고유 기술 선택
| 항목 | 선택 | 근거 |
|---|---|---|
| 파일 업로드 | Spring `MultipartFile`, multipart/form-data | R-U4 증빙 업로드 |
| 파일 제약 | MIME(jpg/png/pdf)·크기 ≤10MB (U1 FileStorage) | R-U4-02/03 |
| 회차 인증 전이 | U2 `SessionService.markVerified` 호출 | R-U4-05(리포지토리 직접접근 금지) |
| 파일+DB 보상 | store→[TX]→롤백 시 delete | R-U4-13 |
| 업로드 크기 제한 | `spring.servlet.multipart.max-file-size=10MB` | R-U4-03 서버 강제 |

## 3. 보류/확장
- U1 보류 상속(TLS·SCA). **파일 바이러스/콘텐츠 스캔 보류**(`cid:practices-discovery:c3`) — 확장 시 스캔 계층 추가.
