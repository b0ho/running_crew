# Performance Requirements — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 관점 quality
> 상위 입력: `U3-enrollment/functional-design/business-logic-model.md`(join 락 §2), `business-rules.md`(R-U3-07 비관적 락), `requirements-analysis/requirements.md`(NFR-4)
> 전제: <100명 파일럿, 로컬 단일 서버(U1 상속).

## 1. 응답 시간 목표 (목표 latency)
| 연산 | 목표 | 비고 |
|---|---|---|
| 참여 신청(join, 락 포함) | ≤ 400ms | 비관적 락 구간 짧게 유지 |
| 내 신청 목록 | ≤ 250ms | menteeId 인덱스 |
| 대기 목록(관리자) | ≤ 300ms | cohortId+status 인덱스 |
| 승인/거절 | ≤ 300ms | |
| 알림 목록/읽음 | ≤ 200ms | userId 인덱스 |

- 통계적 p95는 파일럿 강제 안 함(U1 방침): 목표 대비 스모크. 엄밀 검증은 Operation performance-validation.

## 2. 동시성 성능 (락 경합)
- 비관적 락은 **동일 코호트** 단위로만 직렬화(코호트별 행 락). 서로 다른 코호트 join은 병렬 처리 → 락 경합 범위 최소화.
- 락 보유 구간을 "정원 조회 + insert"로 짧게 유지해 대기 시간 최소화. 락 타임아웃 설정으로 무한 대기 방지.
- 파일럿 규모(코호트당 동시 신청 수십 건)에서 직렬화 지연은 수용 가능. 대규모 인기 코호트 폭주는 파일럿 범위 밖(확장 시 큐/낙관적 재시도 검토).

## 3. 검증
- join 경로 스모크 + 동시성 부하(N+5 스레드)에서 정원 정확성과 함께 응답 시간 관측(정확성 우선, 성능은 관측 수준).
