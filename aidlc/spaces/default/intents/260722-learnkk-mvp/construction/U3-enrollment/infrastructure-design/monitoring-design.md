# Monitoring Design — U3 enrollment (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U3-enrollment
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/reliability-design.md`·`performance-design.md`·`security-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 모니터링 골격 상속. 상세 관측은 Operation observability-setup.

## 1. 헬스·지표(U1 상속 + U3 관측)

U1 `/actuator/health`·Micrometer 공유. U3 고유 관측 지표(설계 수준): join 응답시간(`performance-design.md` 목표 대비), **락 경합/타임아웃 빈도**(락 타임아웃 409 발생률 — 확장 트리거 판단 근거), 대기(WAITING) 적체 추이.

## 2. 로그

- 락 타임아웃(409 ENROLLMENT_BUSY)·중복 신청(409)·self-enrollment 차단(409)·관리자 승인 경합(조건부 UPDATE 영향행 0)·정원 초과 수동 승인(감사)은 애플리케이션 로그로 관측. 민감정보 미포함.
- 정원 초과 수동 승인은 감사 로그로 남김(`business-logic-model.md` §4, R-U3-13).

## 3. 백업

Enrollment/Notification은 공유 PostgreSQL에 있어 U1 일 1회 스냅샷 포함.

## 4. 알림·SLO

파일럿 정형 알림 미설정(U1 상속). 단, 락 타임아웃 발생률은 확장 시점 판단의 핵심 지표이므로 Operation observability-setup에서 대시보드·알림 룰로 승격 권장(설계 메모).
