# Performance Requirements — U1 foundation (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 quality(검증 가능한 SLO)
> 상위 입력: `U1-foundation/functional-design/business-logic-model.md`(인증 흐름), `business-rules.md`(R-U1-08 로그인), `requirements-analysis/requirements.md`(NFR-4 규모/성능)
> 전제: 소규모 파일럿(<100명), 로컬 단일 서버(NFR-4). 고성능·고가용 요구 낮음.

## 1. 응답 시간 목표 (목표 latency, BCrypt cost=10 기준)
| 연산 | 목표 | 비고 |
|---|---|---|
| 로그인(BCrypt 검증 포함) | ≤ 500ms | BCrypt cost=10(NFR-SEC-1) 비용 포함. cost 상향 시 목표 재산정 |
| 회원가입 | ≤ 600ms | 해싱 + DB insert |
| 인증 세션 확인(/me) | ≤ 150ms | |
| 정적/헬스체크 | ≤ 100ms | |

- 위 수치는 **목표 latency**이며 BCrypt cost=10 기준. cost는 설정값(NFR-SEC-1)이라 상향 시 로그인 목표를 재산정한다.
- 파일럿에서는 통계적 p95를 강제하지 않는다(표본 부족). 아래 §4 검증 참조.

## 2. 처리량 / 부하
- 동시 활성 사용자 < 100명 전제. 로그인 피크 동시성 수십 건 수준.
- 목표 처리량: 인증 엔드포인트 ≥ 20 req/s(파일럿 충분). 부하 테스트는 파일럿에서 경량(스모크 수준).

## 3. 리소스 제약
- 로컬 Docker 단일 인스턴스. 메모리/CPU는 소규모 인스턴스 기준. 커넥션 풀 기본값(HikariCP) 소규모 설정.

## 4. 검증 방법 (quality)
- **파일럿**: 핵심 인증 경로의 응답시간을 단건 스모크로 목표 대비 확인(목표 latency 초과 여부만 판정, 통계적 p95 아님).
- **통계적 p95/부하 검증**: 표본 ≥100 요청의 부하 테스트로 p95를 산출하는 엄밀 검증은 **Operation 단계 performance-validation**에서 수행(파일럿은 목표치·스모크로 갈음). 이로써 "스모크로 p95 측정" 모순을 제거한다.
