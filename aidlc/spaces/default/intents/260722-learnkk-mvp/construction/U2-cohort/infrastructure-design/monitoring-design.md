# Monitoring Design — U2 cohort (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U2-cohort
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/reliability-design.md`·`performance-design.md`·`security-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 모니터링 골격(헬스체크·기본 지표·로그·백업) 상속. 상세 관측은 Operation observability-setup.

## 1. 헬스·지표(U1 상속)

U2는 `learnkk-api` 내 모듈이므로 U1의 `/actuator/health`·Micrometer 지표를 공유(`U1-foundation/infrastructure-design/monitoring-design.md`). U2 고유 관측 지표: 코호트 목록/상세 응답시간(`performance-design.md` 목표 대비), 코호트 개설 트랜잭션 지표.

## 2. 로그

- 코호트 상태 전이(조건부 UPDATE 실패 409)·소유권 위반(403)·정원 축소 경고는 애플리케이션 로그로 관측. 민감정보 미포함(U1 방침).
- 공통 에러 핸들러가 U2 예외→에러 DTO 로깅(`reliability-design.md` §1).

## 3. 백업

U2 데이터(Cohort/Session/Announcement)는 공유 PostgreSQL에 있어 U1 일 1회 스냅샷에 포함(별도 백업 대상 아님, `reliability-design.md` §4).

## 4. 알림·SLI/SLO

파일럿 정형 알림·SLO 미설정(U1 상속). Operation 단계로 이관.
