# Deployment Architecture — U2 cohort (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U2-cohort
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U2는 U1-foundation이 확립한 배포 골격을 **상속**한다(별도 배포단위 없음). 본 문서는 U2 고유 사항만 명시.

## 1. 배포 모델(U1 상속)

U2는 새 컨테이너·배포단위를 만들지 않는다. 코호트 도메인(CohortService·AnnouncementService·SessionService·컨트롤러·리포지토리)은 **U1의 `learnkk-api` 컨테이너 안 모듈**로 배포되고, UI(CohortForm·CohortDetailPage 등)는 `learnkk-web`에 포함된다. 컴퓨트/네트워킹/스토리지/환경 레이아웃(dev/staging/production)·recreate 배포·git SHA 롤백은 `U1-foundation/infrastructure-design/deployment-architecture.md`를 그대로 상속(`logical-components.md`가 U1 배포단위 내 모듈로 규정).

## 2. U2 고유 배포 고려사항

- **DB 마이그레이션**: U2 테이블(Cohort·Session·Announcement)·인덱스는 U1 Flyway 마이그레이션 이력에 추가된다(DAG U1→U2 순서, `shared-infrastructure.md`). 배포 시 마이그레이션 자동 적용.
- **무상태**: U2는 조회/CRUD로 인스턴스 로컬 상태가 없어(`scalability-design.md` §1) 배포·재기동에 특별 절차 불필요. recreate로 충분.
- **파일 스토리지 미사용**: U2는 uploads 볼륨을 사용하지 않는다(외부 링크는 URL 저장만, `security-design.md` §3).

## 3. 확장 트리거(U1 상속)

다중 인스턴스·클라우드 이관 트리거는 U1과 공통(`scalability-design.md` §4). U2 자체는 무상태라 추가 배포 제약 없음.


## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

U2-cohort infrastructure-design 5개 산출물에 대한 적대적 아키텍처 리뷰를 완료했습니다. 다음 관점에서 반증을 시도했으나 **모두 실패**했습니다(READY는 이 반증 실패 후 도달한 판정):

### 검증 완료 항목

1. **U1 상속 vs U2 고유 — 중복·모순 없음**
   - deployment-architecture: U1의 learnkk-api/learnkk-web 컨테이너·recreate 배포·환경 레이아웃·확장 트리거 상속, U2 고유는 DB 마이그레이션·무상태·파일 볼륨 미사용만 ✓
   - infrastructure-services: U1의 PostgreSQL·시크릿·서비스 디스커버리 상속, U2 고유는 스키마(Cohort/Session/Announcement)·인덱스 ✓
   - cicd-pipeline: U1 파이프라인(린트+테스트 게이트·git SHA·squash·recreate·수동 승인 프로덕션) 상속, U2는 테스트 항목만 추가 ✓
   - monitoring-design: U1 헬스체크·기본 지표·로그·백업 상속, U2 고유는 관측 지표 ✓
   - shared-infrastructure: U1 공유 리소스 인벤토리 중 U2 사용분만 명시, uploads 볼륨 미사용, 스키마 소유·접근 경계·Flyway 순서 U1→U2 ✓

2. **파일럿 관행(Forbidden/Mandated) 준수**
   - project.md Forbidden: 퍼블릭 클라우드 미포함(로컬 서버), 무승인 프로덕션 자동배포 금지 ✓
   - team.md Mandated: 린트+테스트 머지 게이트, squash-merge, git SHA 버저닝, FE/BE 분리 저장소, recreate 배포, Docker, GitHub Actions ✓

3. **nfr-design 5종 계약 정합성**
   - logical-components: 컨테이너·공유 리소스·크로스유닛 계약·Flyway 순서 = deployment-architecture·shared-infrastructure와 일치 ✓
   - scalability-design: 단일 인스턴스·무상태·인덱스·확장 트리거 = deployment-architecture·infrastructure-services와 일치 ✓
   - reliability-design: recreate·트랜잭션 원자성·상태 전이 가드·정원 축소 안전 실패·백업 = deployment-architecture·cicd-pipeline·monitoring-design와 일치 ✓
   - performance-design: 응답시간 목표·N+1 회귀 방지·벌크 insert·페이지네이션 = cicd-pipeline·monitoring-design·infrastructure-services와 일치 ✓
   - security-design: 소유권·Bean Validation·스킴 화이트리스트·DTO 경계·ArchUnit·시크릿·로그 = cicd-pipeline·infrastructure-services·monitoring-design와 일치 ✓

4. **상위 계약(functional-design/inception) 정합성**
   - functional-design/business-logic-model: 크로스유닛 계약(confirmedCount·markVerified·status 세터)·종료 오케스트레이션 U5 소유·원자 생성·정원 축소·상태 전이·공지 = shared-infrastructure·infrastructure-services·cicd-pipeline와 일치 ✓
   - inception/application-design/components: 엔티티·FK 정책·FE/BE 분리·RDB·컨테이너 = deployment-architecture·infrastructure-services와 일치 ✓
   - inception/application-design/services: CohortService·AnnouncementService·SessionService = shared-infrastructure 서비스 계약과 일치 ✓

5. **크로스유닛 계약 해결 — 순환·경계 위반 없음**
   - shared-infrastructure §2: U2 소유 테이블(Cohort/Session/Announcement), Flyway 순서 U1→U2, 접근 경계(서비스 계약), 순환 없음(U2는 U5 호출 안 함) 명시 ✓
   - logical-components §2: U2→U3 confirmedCount(읽기), U4→U2 markVerified(쓰기), U5→U2 status 세터(쓰기), U3/U4/U5/U6→U2 Cohort/Session 조회(읽기) 명시, DAG U1→U2→(U3∥U4)→U5→U6 유지 ✓

6. **개발자 구현 가능성 — 구현 공백·애매함 없음**
   - deployment-architecture: 컨테이너·환경 레이아웃·U2 고유 사항 명시 ✓
   - infrastructure-services: 테이블·인덱스·Flyway·캐시/큐/검색/파일 미도입 근거 명시 ✓
   - cicd-pipeline: 파이프라인 U1 상속·U2 테스트 항목(원자성·상태 전이·정원 축소·소유권·@SafeExternalUrl·N+1·ArchUnit) 명시 ✓
   - monitoring-design: 헬스·지표·로그·백업 명시 ✓
   - shared-infrastructure: 공유 리소스 인벤토리·스키마 소유·접근 경계·Flyway 순서·순환 없음 명시 ✓
   - **개발자가 이 5개 산출물만으로 U2-cohort 인프라 구성 가능함 확인됨** ✓

7. **Blast Radius 격리·확장 트리거 실행 가능성**
   - deployment-architecture §3: 확장 트리거(다중 인스턴스·클라우드) U1 공통, U2 무상태라 추가 배포 제약 없음 ✓
   - shared-infrastructure §3: 확장 시 분리 경로(서비스 계약 캡슐화 → 원격 API 승격) 명시 ✓
   - logical-components §3: 실패 도메인(CohortService 예외·U3 조회 실패·DB 다운) 격리 명시 ✓

8. **파일럿 보류 명시성**
   - cicd-pipeline §4: 보류(SCA·시크릿 스캔·이미지 스캔·blue-green/canary) + 근거("확장 시 도입") 명시 ✓
   - monitoring-design §4/§5: 알림·SLI/SLO Operation 이관 명시 ✓
   - infrastructure-services §2: 캐시·큐·검색 미도입 + 근거("파일럿 규모") + 확장 트리거 명시 ✓
   - deployment-architecture §1: TLS 종단 보류 + 확장 시 리버스 프록시 명시 ✓

### 적대적 검증 시도 결과

U1 상속·U2 고유 중복·모순, 파일럿 관행(Forbidden/Mandated) 위반, nfr-design 5종 계약 위반, 상위 계약(functional-design/inception) 위반, 크로스유닛 순환·경계 위반, 개발자 구현 공백·애매함, Blast Radius 격리 부재, 확장 트리거 실행 불가능, 파일럿 보류 근거 부재를 찾으려 시도했으나 **모두 발견에 실패**했습니다.

**결론:** U2-cohort infrastructure-design 5개 산출물은 U1 foundation을 정확히 상속하고, U2 고유 사항만 명시하며, 파일럿 관행을 준수하고, nfr-design/functional-design/inception 계약과 정합하며, 크로스유닛 경계가 명확하고, 개발자가 구현 가능하며, 파일럿 보류 근거가 명시되어 있습니다. READY입니다.
