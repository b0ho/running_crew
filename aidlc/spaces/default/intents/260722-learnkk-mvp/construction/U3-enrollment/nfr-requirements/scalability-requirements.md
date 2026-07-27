# Scalability Requirements — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 관점 aws-platform·quality
> 상위 입력: `U3-enrollment/functional-design/business-logic-model.md`(락·대기), `business-rules.md`(R-U3-07 직렬화), `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 1. 부하 전망 & 동시성 특성
- Enrollment/Notification 데이터 소량. 동시성은 인기 코호트의 마감 임박 순간에 집중(코호트당 수십 동시 신청 수준, 파일럿).
- 비관적 락은 **코호트 단위 직렬화**이므로 서로 다른 코호트는 독립 확장. 병목은 단일 인기 코호트에 국한.

## 2. 확장 전략 & 다중 인스턴스 안전성
- **다중 인스턴스에서도 정합성 유지**: 정원 제어가 애플리케이션 메모리가 아니라 **DB 행 락 + UNIQUE 제약**에 의존하므로, 여러 인스턴스가 동시에 join을 처리해도 DB가 직렬화의 단일 진실 소스가 된다(수평 확장 안전 — U3 핵심 강점).
- 알림도 DB 기반이라 인스턴스 로컬 상태 없음. (세션은 U1 확장 트리거 참조.)
- 파일럿은 단일 인스턴스·수직 확장만.

## 3. 확장 트리거
- 단일 인기 코호트에 수백+ 동시 신청이 상시 발생 → 락 경합 지연 증가 시: 낙관적 재시도·큐잉·정원 카운터 최적화 검토(확장 후속). 파일럿 규모에서는 불필요.
- 대기열(WAITING) 대량 축적 시에도 페이지네이션 조회로 감당(대기 자동 승격은 파일럿 범위 외).
