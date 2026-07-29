# CI/CD Pipeline — U6 admin-metrics (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/security-design.md`·`reliability-design.md`·`performance-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 GitHub Actions 파이프라인 상속. U6은 지표/이력 테스트를 CI에 추가.

## 1. 파이프라인(U1 상속)

U1과 동일 저장소·워크플로. CI·git SHA 아티팩트·CD(staging 자동/production 수동 승인)·롤백·시크릿은 `U1-foundation/infrastructure-design/cicd-pipeline.md` 상속.

## 2. U6 CI 테스트 추가(머지 게이트)

- **지표 산식 정확성**: 작은 고정 데이터셋으로 완주 수·출석률·수료율·증서 수 기대값 단언(Testcontainers).
- **0 나눗셈 안전**: 종료 코호트 0건 / 확정 멘티 0명 시 0% 반환.
- **이력 분리 조회**: 증빙 이력·보고서 이력 별도 뷰, 페이지네이션(20건).
- **관리자 인가**: 비관리자 403·미인증 401.
- **읽기 전용**: U6 경로가 쓰기 연산을 하지 않음(INV-U6-1) 확인.
- **learnkk-web**: MetricsOverview·이력 탭 컴포넌트 테스트.
- ArchUnit DTO 경계.

## 3. 배포·시크릿(U1 상속)

recreate·git SHA 롤백·Actions Secrets 상속. U6은 신규 스키마·마이그레이션 없음(소스 유닛 인덱스 전제).

## 4. 파일럿 보류(U1 상속)

지표 캐시·머티리얼라이즈드 뷰는 파일럿 미도입(실시간 집계). SCA·스캔·고급 배포 보류. 확장 시 도입.
