# CI/CD Pipeline — U3 enrollment (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U3-enrollment
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/security-design.md`·`reliability-design.md`·`performance-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 GitHub Actions 파이프라인 상속. U3는 동시성 테스트를 CI에 추가.

## 1. 파이프라인(U1 상속)

U1과 동일 저장소·워크플로. CI 단계·git SHA 아티팩트·CD(staging 자동/production 수동 승인)·롤백·시크릿은 `U1-foundation/infrastructure-design/cicd-pipeline.md` 상속.

## 2. U3 CI 테스트 추가(머지 게이트 — 핵심)

team.md Mandated + team.md Testing Posture(동시성 테스트):
- **동시성 정원 경계(필수 게이팅)**: Testcontainers 실 DB + ExecutorService + CountDownLatch로 N+k 동시 join → CONFIRMED==min(N,요청수)·중복 0 단언(N/N+1/N+5 경계). `reliability-design.md` §1의 게이팅 조건.
- 중복 신청(UNIQUE 409), 관리자 승인 경합(조건부 UPDATE 이중승인→1건), 상태 전이(WAITING→CONFIRMED/REJECTED만), 락 타임아웃 경로 통합 테스트.
- self-enrollment 차단·본인 스코프·관리자 인가 테스트.
- **learnkk-web**: MyApplicationsPage·NotificationBell 컴포넌트 테스트.
- ArchUnit DTO 경계.

## 3. 배포·시크릿(U1 상속)

recreate·git SHA 롤백·Actions Secrets 상속. U3 테이블·인덱스는 배포 시 Flyway 자동 적용.

## 4. 파일럿 보류(U1 상속)

SCA·스캔·고급 배포 보류. 확장 시 공통 도입.
