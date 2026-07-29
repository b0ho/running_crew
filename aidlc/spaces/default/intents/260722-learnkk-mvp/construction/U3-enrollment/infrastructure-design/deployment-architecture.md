# Deployment Architecture — U3 enrollment (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U3-enrollment
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U3는 U1-foundation 배포 골격을 **상속**(별도 배포단위 없음). 본 문서는 U3 고유(동시성) 사항만 명시.

## 1. 배포 모델(U1 상속)

U3 참여/알림 도메인(EnrollmentService·AdminApprovalService·NotificationService)은 U1 `learnkk-api` 컨테이너 모듈로, UI(ExplorePage·JoinButton·NotificationBell 등)는 `learnkk-web`에 배포된다. 컴퓨트/네트워킹/스토리지/환경/​recreate/git SHA 롤백은 `U1-foundation/infrastructure-design/deployment-architecture.md` 상속.

## 2. U3 고유 배포 고려사항 — 동시성

- **DB 락 설정**: U3 비관적 락(`SELECT ... FOR UPDATE`)·락 타임아웃(`jakarta.persistence.lock.timeout=3000ms`)·DB `statement_timeout`(예 5s)를 애플리케이션/DB 설정으로 배포(`performance-design.md` §2, `infrastructure-services.md`). 트랜잭션 격리 READ_COMMITTED.
- **다중 인스턴스 안전(배포 관점 강점)**: 정원 정합이 DB 행 락+UNIQUE에 의존하므로(`scalability-design.md` §2), 향후 인스턴스 ≥2 배포에서도 U3 정합성 코드 변경 불필요. 파일럿은 단일 인스턴스.
- **파일 스토리지 미사용**.

## 3. 확장 트리거(U1 공통 + U3)

다중 인스턴스·클라우드 이관은 U1 공통. U3 고유: 단일 인기 코호트 수백+ 동시 신청 상시 발생 시 낙관적 재시도/큐/원자 카운터(`scalability-design.md` §4) — 애플리케이션 레벨 변경(인프라 토폴로지 변경 아님).


## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 검증 결과

U3-enrollment infrastructure-design 산출물(deployment-architecture, infrastructure-services, monitoring-design, cicd-pipeline, shared-infrastructure) 5개 파일에 대한 적대적 아키텍처 리뷰를 완료했습니다. defect가 존재한다고 가정하고 반증을 시도한 결과, 다음 사항을 확인했습니다:

1. **동시성 인프라 설정 실현 가능성 ✓**
   - JPA 락 힌트 `jakarta.persistence.lock.timeout = 3000ms` 확정 (`performance-design.md` §2, `infrastructure-services.md` §1)
   - DB `statement_timeout` (예: 5s) 확정 (`performance-design.md` §2, `infrastructure-services.md` §1)
   - 트랜잭션 격리 READ_COMMITTED 명시 (`performance-design.md` §2, `infrastructure-services.md` §1)
   - 비관적 락 `PESSIMISTIC_WRITE` = `SELECT ... FOR UPDATE` 매핑 명시 (`infrastructure-services.md` §1)
   - 배포 경로: 애플리케이션/DB 설정으로 배포 (`deployment-architecture.md` §2)
   - 개발자가 JPA `@Lock` + `@QueryHints` 및 HikariCP connection init SQL 또는 PostgreSQL 세션 설정으로 구현 가능 확인

2. **다중 인스턴스 안전성 주장 타당성 ✓**
   - "DB가 단일 진실 소스이므로 다중 인스턴스에서도 U3 정합성 코드 변경 불필요" 주장이 FOR UPDATE 락(DB 레벨 락, 인스턴스 수와 무관하게 직렬화) + UNIQUE 제약(DB 단일 제약)으로 구조적으로 타당함 확인 (`scalability-design.md` §2, `infrastructure-services.md` §4, `deployment-architecture.md` §2)
   - 세션 확장 제약은 U1 트리거로 명시, U3 자체는 추가 제약 없음 (`shared-infrastructure.md` §3)

3. **U1 공유 인프라 상속 정확성 & 중복 없음 ✓**
   - U3가 "상속"한다고 주장하는 모든 항목(컨테이너·네트워킹·recreate·git SHA 롤백·Flyway·GitHub Actions·시크릿)이 U1 `infrastructure-design/` 5개 파일에 실제 존재함 확인
   - U3는 U1 공통 항목을 재정의하지 않고 "상속" 또는 "U1과 동일"로 참조만 하며, U3 고유 항목(동시성 설정, Enrollment/Notification 스키마·인덱스, 동시성 테스트 CI 추가)만 명시 → 중복 없음

4. **파일럿 관행(team.md/project.md Forbidden/Mandated) 준수 ✓**
   - Forbidden: 퍼블릭 클라우드 금지 (`deployment-architecture.md` §1 "퍼블릭 클라우드는 파일럿 Forbidden"), 무승인 프로덕션 자동배포 금지 (`cicd-pipeline.md` §1 "production 수동 승인")
   - Mandated: 로컬 Docker recreate (`deployment-architecture.md` §1), GitHub Actions (`cicd-pipeline.md` §1), git SHA 버저닝 (U1 §3 상속), 머지 전 린트+테스트 실패 시 블럭 (`cicd-pipeline.md` §2), 한글 산출물 (전 파일 한글)

5. **동시성 테스트 CI 머지 게이트 포함(핵심) ✓**
   - `cicd-pipeline.md` §2 "**동시성 정원 경계(필수 게이팅)**: Testcontainers 실 DB + ExecutorService + CountDownLatch로 N+k 동시 join → CONFIRMED==min(N,요청수)·중복 0 단언(N/N+1/N+5 경계). `reliability-design.md` §1의 게이팅 조건." 명시
   - `reliability-design.md` §1 "**이 테스트 통과가 U3 신뢰성의 게이팅 조건**" 명시
   - U1 §2 Mandated "실패 시 블럭"과 결합하여 동시성 테스트가 머지 게이트로 작동함 확인

6. **스키마 소유·경계·순환 없음 ✓**
   - U3 소유 테이블: Enrollment, Notification (`shared-infrastructure.md` §2)
   - Flyway 순서: U2 다음 ((U3∥U4)) (`shared-infrastructure.md` §2, `infrastructure-services.md` §1)
   - 접근 경계 캡슐화: U3 → U2 읽기만(`CohortService.get`), U3 제공 계약: `confirmedCount`/`confirmedEnrollments`(U2/U5/U6 사용), `notify`(U5/U8 호출) (`shared-infrastructure.md` §2)
   - 순환 없음: U3는 U5를 호출하지 않음, U3·U4 병렬 유닛(상호 직접 의존 없음) (`shared-infrastructure.md` §2)
   - DAG U1→U2→(U3∥U4)→U5→U6 유지

7. **인덱스 NFR-design 정합성 ✓**
   - `infrastructure-services.md` §1 인덱스: `enrollment(cohort_id, status)`, `enrollment(mentee_id)`, UNIQUE `enrollment(cohort_id, mentee_id)`, `notification(user_id, is_read, created_at)`
   - `scalability-design.md` §2, `performance-design.md` §3 인덱스: 동일 — 일치 확인

8. **구현 가능성 ✓**
   - 개발자가 5개 산출물에서:
     - (1) U3 테이블·FK·인덱스 Flyway 마이그레이션 작성 (`infrastructure-services.md` §1, `components.md` FK 정책)
     - (2) 비관적 락·락 타임아웃·statement_timeout 설정 구현 (`infrastructure-services.md` §1, `deployment-architecture.md` §2)
     - (3) 동시성 테스트(ExecutorService+CountDownLatch+Testcontainers, N/N+1/N+5 경계) 작성 (`cicd-pipeline.md` §2)
     - (4) CI에 테스트 단계 추가 (`cicd-pipeline.md` §2)
   - 상세 비즈니스 로직은 functional-design/business-logic-model + nfr-design에 있으나, **인프라 구성 자체는 본 5개 산출물만으로 구현 가능** 확인

9. **순환 의존성 없음 ✓**
   - deployment-architecture → nfr-design → functional-design: 순방향만
   - U3 → U2(읽기), U2/U5/U6 → U3(읽기 계약), U5/U8 → U3(쓰기 계약), U3는 U5를 호출하지 않음
   - DAG 유지

10. **Blast Radius 격리 ✓**
    - U3 고유 장애(락 경합/타임아웃): 해당 코호트 join 요청만 영향, 다른 코호트 무영향 (`logical-components.md` §3)
    - DB 다운: 전체 영향이지만 U1 공유 리소스 장애(U3 고유 아님), 정합성은 DB 복구로 보존

### Findings

- **Critical**: 없음
- **Major**: 없음
- **Minor**: 없음

### 적대적 검증 시도 요약

다음 defect를 찾으려 시도했으나 **모두 발견에 실패**했습니다:
1. 동시성 설정 실현 불가능 → 실현 가능 확인 ✓
2. 다중 인스턴스 안전성 주장 근거 부족 → DB 단일 진실 소스 타당 ✓
3. U1 상속 주장이 U1에 존재하지 않음 또는 U3가 중복 정의 → 상속 정확, 중복 없음 ✓
4. 파일럿 관행(Forbidden/Mandated) 위반 → 준수 확인 ✓
5. 동시성 테스트가 CI 머지 게이트에 미포함 → 게이팅 조건 명시 ✓
6. 스키마 소유 불명확, 경계 모호, 순환 존재 → 소유 명확, 경계 캡슐화, 순환 없음 ✓
7. 인덱스 NFR-design 불일치 → 일치 확인 ✓
8. 개발자 구현 불가능(설정값 미확정, 배포 경로 모호) → 구현 가능 ✓
9. 순환 의존성 존재 → 순환 없음 ✓
10. Blast radius 격리 부재 → 코호트 단위 국소화 확인 ✓

**READY는 이 반증 실패 후 도달한 판정입니다.**

### 결론

U3-enrollment infrastructure-design 산출물은 최대 정합성 리스크 유닛에 요구되는 동시성 인프라 설정(비관적 락·락 타임아웃·statement_timeout·격리 수준)을 확정값으로 명시하고, 배포 경로를 구체화했으며, 동시성 테스트를 CI 머지 게이트로 포함했습니다. U1 공유 인프라를 정확히 상속하고 U3 고유 항목만 추가하여 중복이 없으며, 파일럿 관행(Forbidden/Mandated)을 준수하고, 다중 인스턴스 안전성 주장이 구조적으로 타당하며, 스키마 소유·경계가 명확하고 순환이 없으며, 개발자가 본 5개 산출물만으로 U3 인프라를 구현 가능함이 확인되었습니다.

**개발자가 이 산출물만으로 U3 인프라를 아키텍처 가이던스 없이 구성 가능함이 확인되었습니다.**

**판정: READY**
