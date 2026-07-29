# CI/CD Pipeline — U5 completion (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U5-completion
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/security-design.md`·`reliability-design.md`·`performance-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 GitHub Actions 파이프라인 상속. U5는 종료 오케스트레이션 테스트를 CI에 추가.

## 1. 파이프라인(U1 상속)

U1과 동일 저장소·워크플로. CI·git SHA 아티팩트·CD(staging 자동/production 수동 승인)·롤백·시크릿은 `U1-foundation/infrastructure-design/cicd-pipeline.md` 상속. 컨테이너 이미지에 수료증 렌더링 라이브러리·폰트 포함(빌드 의존성).

## 2. U5 CI 테스트 추가(머지 게이트)

- **종료 트랜잭션 원자성**: 강제 롤백 시 증서 0·상태 미전이·정산 미기록(Testcontainers).
- **수료 80% 경계**: 79%(미수료)/80%(수료) 정수 산술 경계 테스트.
- **멱등**: 재종료 시 증서 재발급 없음(UNIQUE), 정산 upsert 1건.
- **다건 파일 보상**: N개 이미지 store 후 롤백 → N개 전부 delete 확인.
- **인가**: 소유 멘토 종료·수료증 본인 스코프·참여자 보고서 제출.
- **learnkk-web**: ReportForm·CompletionResult 컴포넌트 테스트.
- ArchUnit DTO 경계.

## 3. 배포·시크릿(U1 상속)

recreate·git SHA 롤백·Actions Secrets 상속. U5 테이블·인덱스·제약 Flyway 자동 적용. uploads 볼륨 백업 잡은 U1/U4 공통.

## 4. 파일럿 보류(U1 상속)

SCA·스캔·blue-green/canary·증서 비동기 발급 보류. 확장 시 도입.
