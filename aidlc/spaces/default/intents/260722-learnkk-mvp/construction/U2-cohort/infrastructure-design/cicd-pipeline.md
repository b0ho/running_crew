# CI/CD Pipeline — U2 cohort (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U2-cohort
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/security-design.md`·`reliability-design.md`·`performance-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1이 확립한 GitHub Actions 파이프라인(FE/BE 분리·린트+테스트 게이트·git SHA·squash·recreate·수동 승인 프로덕션)을 **상속**. U2는 테스트 항목만 추가.

## 1. 파이프라인(U1 상속)

U2는 U1과 동일 저장소·워크플로에서 빌드·배포된다(별도 파이프라인 없음). CI 단계·아티팩트(git SHA)·CD(staging 자동/production 수동 승인)·롤백·시크릿은 `U1-foundation/infrastructure-design/cicd-pipeline.md`를 상속.

## 2. U2 CI 테스트 추가(머지 게이트)

team.md Mandated(린트+테스트 그린 머지 게이트)에 U2 테스트를 포함:
- **learnkk-api**: 코호트 개설 원자성(회차 벌크 롤백), 상태 전이(허용/역전이 거부), 정원 축소 경계(U3 조회 실패 안전 실패), 소유권 인가, `@SafeExternalUrl` 검증 통합 테스트(Testcontainers). N+1 회귀 방지(쿼리 카운트).
- **learnkk-web**: CohortForm·CohortDetailPage 컴포넌트 테스트(Jest/RTL).
- **ArchUnit**: 컨트롤러 반환 타입이 Entity 미노출(INV-U2-4, `security-design.md` §4).

## 3. 배포·시크릿(U1 상속)

recreate·git SHA 롤백·GitHub Actions Secrets 모두 U1 상속. U2 신규 시크릿 없음. DB 마이그레이션(U2 테이블·인덱스)은 배포 시 Flyway 자동 적용.

## 4. 파일럿 보류(U1 상속)

SCA·스캔·blue-green/canary 보류(U1과 동일). 확장 시 공통 도입.
