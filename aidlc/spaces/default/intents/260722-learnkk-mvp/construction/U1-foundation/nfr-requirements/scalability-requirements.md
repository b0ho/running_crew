# Scalability Requirements — U1 foundation (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 aws-platform(용량)
> 상위 입력: `U1-foundation/functional-design/business-logic-model.md`, `business-rules.md`, `requirements-analysis/requirements.md`(NFR-2 호스팅, NFR-4 규모)
> 전제: 로컬 서버·단일 인스턴스(NFR-2), <100명(NFR-4). 확장은 후속 과제.

## 1. 부하 전망 & 용량
- 사용자 규모 < 100명, 데이터 규모 소량(계정·코호트·참여·증빙 파일). 성장 급증 없음(사내 파일럿).
- User 테이블 수백 행 수준. 인증 조회는 email UNIQUE 인덱스로 O(log n).

## 2. 확장 전략 (파일럿 vs 확장)
- **파일럿**: 수직 확장(단일 인스턴스 리소스 조정)만. 수평 확장·오토스케일 미도입.
- **세션 상태**: 서버 세션(단일 인스턴스이므로 세션 공유 스토어 불필요). **확장 시** 다중 인스턴스가 되면 세션 스토어(예: Redis) 또는 JWT 전환 필요 — project.md ## Tech Stack의 "JWT 확장 시 재검토"와 정합.
- 클라우드/멀티리전은 범위 외(Forbidden: 퍼블릭 클라우드 초기 파일럿 제외).

## 3. 동시성
- U1 자체 동시성 이슈는 회원가입 email 경쟁(UNIQUE 제약이 방어, business-rules R-U1-17c). 고동시성 대상 아님(고동시성은 U3 enrollment).

## 4. 확장 트리거(구체)
- **인스턴스 ≥ 2로 확장하는 순간**(수평 확장 도입 시점): 서버 세션이 인스턴스 로컬이므로 즉시 **세션 스토어 외부화(Redis 등) 또는 JWT 전환** 필요(그 전까지는 단일 인스턴스라 불필요).
- **활성 사용자 100명 초과** 또는 인증 응답 목표 지속 미달 시: DB 커넥션 풀 상향·인스턴스 리소스 상향(수직) 우선 검토.
- 클라우드 이관은 위 트리거와 별개의 조직 결정(확장 후속 과제).
