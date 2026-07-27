# Performance Requirements — U5 completion (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U5-completion
> 리드 architect · 관점 quality
> 상위 입력: `U5-completion/functional-design/business-logic-model.md`(종료 §2), `business-rules.md`(R-U5-05~08), `requirements-analysis/requirements.md`(NFR-4)
> 전제: <100명 파일럿(코호트당 확정 멘티 수십), 로컬 단일 서버(U1 상속).

## 1. 응답 시간 목표 (목표 latency)
| 연산 | 목표 | 비고 |
|---|---|---|
| 코호트 종료(판정+증서 N장 발급) | ≤ 3s | 확정 멘티 N명(수십)만큼 이미지 생성·저장. N에 비례 |
| 최종 보고서 제출(첨부 ≤10MB) | ≤ 2s | 파일 저장 의존(U4와 유사) |
| 보고서 이력 조회 | ≤ 300ms | 페이지네이션 |
| 수료증 조회/다운로드 | ≤ 1s | 이미지 스트리밍 |

- 종료 목표는 확정 멘티 수 N에 비례(멘티별 수료증 이미지 생성). 파일럿 코호트 규모(수십)에서 ≤3s 보수 목표. 통계적 p95는 파일럿 강제 안 함(U1 방침).

## 2. 리소스 & 처리
- 종료는 멘토의 1회 액션(빈도 낮음). N장 이미지 생성은 트랜잭션 내 순차 처리(파일럿 규모 수용). 대규모 코호트(수백 멘티)는 파일럿 밖 — 확장 시 배치/비동기 발급 검토.
- 이미지 생성은 메모리 효율적 렌더링(대형 캔버스 회피).

## 3. 검증
- 종료 경로 스모크(수십 멘티 코호트 1건 종료 시간 관측). 상세 부하 검증은 Operation performance-validation.
