# Performance Requirements — U4 attendance (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 quality
> 상위 입력: `U4-attendance/functional-design/business-logic-model.md`(업로드·조회), `business-rules.md`(R-U4-03 크기), `requirements-analysis/requirements.md`(NFR-4)
> 전제: <100명 파일럿, 로컬 단일 서버(U1 상속).

## 1. 응답 시간 목표 (목표 latency)
| 연산 | 목표 | 비고 |
|---|---|---|
| 증빙 업로드(≤10MB, 저장+인증) | ≤ 2s | 파일 크기·디스크 I/O 의존. 10MB 상한 기준 |
| 진도·출석 조회 | ≤ 300ms | 회차 status 집계 |
| 증빙 다운로드(≤10MB) | ≤ 2s | 스트리밍, 네트워크 의존 |

- 업로드/다운로드 목표는 파일 크기·로컬 디스크·네트워크에 의존하므로 상한(10MB) 기준 보수 목표. 통계적 p95는 파일럿 강제 안 함(U1 방침).

## 2. 리소스
- 업로드는 스트리밍 저장(전체 메모리 로딩 지양)으로 메모리 사용 억제. multipart 임시 파일 정리.
- 10MB 상한이 대용량 업로드로 인한 리소스 고갈을 1차 방지(DoS 완화).

## 3. 검증
- 업로드/다운로드 경로 스모크(상한 근처 파일 1건). 상세 부하 검증은 Operation performance-validation.
