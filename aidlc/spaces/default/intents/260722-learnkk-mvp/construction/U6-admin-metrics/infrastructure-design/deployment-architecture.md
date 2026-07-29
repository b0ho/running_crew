# Deployment Architecture — U6 admin-metrics (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U6은 U1-foundation 배포 골격을 **상속**. 읽기 전용 리포팅 유닛으로 신규 인프라 최소.

## 1. 배포 모델(U1 상속)

U6 리포팅/이력 도메인(MetricsService·HistoryService·MetricsRepository·컨트롤러)은 U1 `learnkk-api` 모듈로, UI(AdminPage 지표·증빙이력·보고서이력 탭)는 `learnkk-web`에 배포된다. 컴퓨트/네트워킹/환경/recreate/git SHA 롤백은 `U1-foundation/infrastructure-design/deployment-architecture.md` 상속.

## 2. U6 고유 배포 고려사항

- **신규 스키마·볼륨 없음**: U6은 읽기 전용 리포팅 모델(INV-U6-1)로 자체 테이블·파일 볼륨을 만들지 않는다. 소스 유닛(U2~U5) 공유 스키마를 조인·집계만 한다.
- **읽기 전용 트랜잭션**: `@Transactional(readOnly=true)`(`security-design.md` §2). 배포·재기동에 특별 절차 불필요(무상태).
- **이력 파일 다운로드**: 증빙/보고서 파일 다운로드 링크는 U1 `FileStorageService.load` 경유(신규 볼륨 아님, 관리자 권한 확인).
- **집계 성능 전제**: 기준 인덱스 5종(`cohort(status)`, `enrollment(cohort_id,status)`, `certificate(cohort_id)`, `attendance_evidence(created_at)`, `final_report(submitted_at)`)이 배포된 소스 스키마에 존재해야 함(각 소유 유닛 마이그레이션, `performance-design.md` §2). 주: `final_report`는 엔티티 필드명 `submittedAt` 기준(U5 nfr-design·infrastructure 정정 반영).

## 3. 확장 트리거

데이터 대규모(수만+) 성장 시 지표 캐시(TTL)·머티리얼라이즈드 뷰·사전 집계 테이블 도입(`scalability-design.md` §3). 이는 애플리케이션/스키마 레벨 변경(인프라 토폴로지 변경 아님). 클라우드·다중 인스턴스는 U1 공통 트리거(U6은 무상태라 추가 제약 없음).

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 재검토 범위 및 BF-1 검증

U6-admin-metrics infrastructure-design 단계 5개 산출물(deployment-architecture, infrastructure-services, monitoring-design, cicd-pipeline, shared-infrastructure)에 대한 2차 적대적 재검토를 완료했습니다. 1차 리뷰에서 지적된 **BF-1(보고서 이력 인덱스 필드명 불일치)** 해소 여부를 중심으로, defect가 여전히 존재한다고 가정하고 반증을 시도하는 관점으로 진행했습니다.

### BF-1 해소 확인 ✅

**BF-1 원인**: U5 FinalReport 엔티티의 필드명은 `submittedAt`인데, U5 infrastructure-design 및 nfr-design에서 인덱스를 `final_report(cohort_id, created_at)`로 잘못 명시했던 문제.

**수정 검증 결과**:
1. **U5 수정 확인** ✅
   - `U5-completion/nfr-design/performance-design.md` §3: `final_report(cohort_id, submitted_at)` 인덱스로 정정, 주석에 "FinalReport 필드명은 `submittedAt` — `components.md`/domain-entities 정합; `created_at` 아님" 명시 확인
   - `U5-completion/infrastructure-design/infrastructure-services.md` §1: `final_report(cohort_id, submitted_at)` 인덱스로 정정, 주석에 "FinalReport 필드는 `submittedAt`" 명시 확인
   - grep 검증: `final_report(cohort_id, created_at)` 패턴 전체 워크스페이스에서 0건 (구버전 제거 확인)
   - grep 검증: `final_report(cohort_id, submitted_at)` 패턴 U5 2개 파일에서만 발견 (정정 확인)

2. **U6 정합성 확인** ✅
   - `deployment-architecture.md` §2: `final_report(submitted_at)` 인덱스 명시, 주석에 "엔티티 필드명 `submittedAt` 기준(U5 nfr-design·infrastructure 정정 반영)" 명시 확인
   - `infrastructure-services.md` §1: `final_report(submitted_at)` 인덱스(U5 소유) 명시, 기준 인덱스 5종 표에 정확히 포함됨
   - U6은 원래부터 단일 필드 인덱스 `final_report(submitted_at)`로 올바르게 명시되어 있었으며, U5 정정 후 상호 정합 확인

**BF-1 최종 판정**: **완전 해소**. U5 소유 인덱스가 올바른 필드명(`submitted_at`)으로 정정되었고, U6 요구 명세와 일치함을 확인했습니다.

### 재검토 검증 항목 (적대적 관점)

#### 1. 크로스유닛 인덱스 계약 정합성 ✅
- **기준 인덱스 5종 일관성**: deployment-architecture §2, infrastructure-services §1, nfr-design/performance-design §2, nfr-design/scalability-design §2 모두 동일한 5개 인덱스 목록 명시 (`cohort(status)`, `enrollment(cohort_id,status)`, `certificate(cohort_id)`, `attendance_evidence(created_at)`, `final_report(submitted_at)`)
- **소유 유닛 명확성**: 각 인덱스의 소유 유닛(U2~U5) infrastructure-services §1 표에 명시, "U6가 요구로 명시하되 생성은 각 소유 유닛 마이그레이션" 책임 분리 명확
- **U5 인덱스 정합**: BF-1 수정으로 U5 `final_report(cohort_id, submitted_at)` ← U6 요구 `final_report(submitted_at)` 정합 (복합 인덱스는 단일 필드 조회도 커버)
- **필드명 정합**: FinalReport 엔티티 `submittedAt` ← 인덱스 `submitted_at` (snake_case DB 컬럼명) 정합 명시됨

#### 2. U1 상속 계약 정합성 ✅
- **배포 모델 상속**: deployment-architecture §1 "U1 `learnkk-api`/`learnkk-web` 모듈, U1 컴퓨트/네트워킹/환경/recreate/git SHA 롤백 상속" 명시
- **인프라 서비스 상속**: infrastructure-services 前文 "U1 인프라 서비스 상속. U6은 신규 스키마·저장소 없음"
- **모니터링 상속**: monitoring-design §1 "U1 `/actuator/health`·Micrometer 공유"
- **CI/CD 상속**: cicd-pipeline §1 "U1과 동일 저장소·워크플로, CI·git SHA 아티팩트·CD·롤백·시크릿 상속"
- **공유 인프라 상속**: shared-infrastructure §1 표에 U1 확립 리소스(PostgreSQL, `learnkk-api`/`learnkk-web`, uploads 볼륨, GitHub Actions) 명시
- U1-foundation/infrastructure-design/deployment-architecture 검증: U6이 상속 주장한 모든 항목(컨테이너, recreate, git SHA, 환경 레이아웃) 존재 확인 ✅

#### 3. U6 고유 설계 정합성 ✅
- **읽기 전용 무상태**: deployment-architecture §2 "읽기 전용 트랜잭션, 배포·재기동 특별 절차 불필요" = nfr-design INV-U6-1 정합
- **신규 스키마·볼륨 없음**: deployment-architecture §2 + infrastructure-services §1 "신규 테이블 없음, 소스 유닛 테이블 조인·집계만" = functional-design 읽기 전용 모델 정합
- **이력 파일 다운로드**: deployment-architecture §2 "U1 `FileStorageService.load` 경유, 관리자 권한 확인" = functional-design 계약 정합
- **말단 소비자**: shared-infrastructure §2 "어떤 유닛도 U6 호출 안함, U6은 아무것도 쓰지 않음, DAG 종착점" = functional-design §5·§7 정합

#### 4. 성능 목표 달성 조건 명확성 ✅
- **목표 전제**: deployment-architecture §2 "집계 성능 전제: 기준 인덱스 5종이 배포된 소스 스키마에 존재해야 함" 명시
- **인덱스-목표 연결**: nfr-design/performance-design §1 목표(≤500ms/350ms) + §2 기준 인덱스 + "파일럿 규모 <100명 전제, 이 인덱스 없이는 목표 보장 안됨" 명시
- **확장 트리거**: deployment-architecture §3 + nfr-design/scalability-design §3 "데이터 수만+ 성장 시 지표 캐시·머티리얼라이즈드 뷰·사전 집계 도입" 명시
- 기준 인덱스 5개가 code-generation 전제 조건임을 명확히 했으므로 목표 달성 가능성 확보 ✅

#### 5. 보안·컴플라이언스 정합성 ✅
- **관리자 전용 인가**: monitoring-design §2 "관리자 인가 실패 로그, 비관리자 403·미인증 401" = nfr-design/security-design §1 `@PreAuthorize("hasRole('ADMIN')")` 정합
- **PII 보호**: monitoring-design §2 "이력 뷰 PII 접근 로그 남기되 원문 로깅 최소화(compliance)" = nfr-design/security-design §3 정합
- **읽기 전용 무결성**: infrastructure-services §1 "READ_COMMITTED, `readOnly=true`" = nfr-design/security-design §2 정합

#### 6. 백업·내구성 정합성 ✅
- **U6 자체 백업 없음**: monitoring-design §3 "U6은 자체 영속 데이터 없음, 별도 백업 대상 없음, 소스 데이터는 U1 백업 정책에 포함" = nfr-design/reliability-design §3 정합
- **소스 백업 의존**: U1-foundation/infrastructure-design/monitoring-design에서 DB + uploads 볼륨 백업 명시 확인, U6 이력 다운로드 파일이 모두 커버됨 ✅

#### 7. CI/CD 테스트 완전성 ✅
- **U6 테스트 추가**: cicd-pipeline §2 "지표 산식 정확성, 0 나눗셈 안전, 이력 분리 조회, 관리자 인가, 읽기 전용, learnkk-web 컴포넌트, ArchUnit DTO 경계" = nfr-design/reliability-design §4 테스트 항목 정합
- **신규 스키마 없음**: cicd-pipeline §3 "U6은 신규 스키마·마이그레이션 없음(소스 유닛 인덱스 전제)" = infrastructure-services §1 정합

#### 8. 확장 아키텍처 실행 가능성 ✅
- **무상태 수평 확장**: deployment-architecture §3 + shared-infrastructure §3 "U6은 무상태 읽기 전용이라 다중 인스턴스 확장 시 추가 제약 없음(세션은 U1 트리거)" = nfr-design/scalability-design §2 정합
- **집계 성능 확장**: deployment-architecture §3 + nfr-design/scalability-design §3 "지표 캐시·머티리얼라이즈드 뷰·사전 집계(애플리케이션/스키마 레벨 변경, 인프라 토폴로지 변경 아님)" 명시
- **클라우드 이관**: shared-infrastructure §3 "클라우드·스토리지 확장은 U1 shared-infrastructure §4 공통" 참조 확인 ✅

#### 9. 크로스레퍼런스 유효성 ✅
- 모든 상위 계약 참조(nfr-design 5개, functional-design/business-logic-model, U1 infrastructure-design) 존재 확인
- 규칙 ID(R-U6-01~11) 모두 functional-design/business-rules.md 해소
- 불변식 ID(INV-U6-1~4) 모두 functional-design/business-rules.md 해소
- 컴포넌트/서비스 참조(MetricsService, HistoryService, MetricsRepository, FileStorageService.load) 일관
- §n 내부 참조(§1, §2, §3) 모두 해소

#### 10. 개발자 구현 가능성 ✅
- **배포 토폴로지**: deployment-architecture §1~2에서 U6 코드가 어느 컨테이너에 배포되는지(`learnkk-api`/`learnkk-web`), 신규 리소스 없음, 상속 항목 명확
- **인프라 서비스**: infrastructure-services §1~4에서 DB 접근 모델(읽기 전용, 기준 인덱스 전제), 캐시/큐 미도입, 파일 읽기만, 시크릿 상속 명시
- **모니터링**: monitoring-design §1~4에서 헬스체크 상속, 로그(인가 실패·조회 실패·PII 최소화), 백업 대상 없음, 알림 미설정 명시
- **CI/CD**: cicd-pipeline §1~4에서 파이프라인 상속, U6 테스트 추가, 신규 시크릿 없음, 보류 항목 명시
- **공유 인프라**: shared-infrastructure §1~3에서 U6이 사용하는 공유 리소스 표, 접근 경계(읽기 전용 말단 소비자), 확장 참조 명시
- **개발자가 5개 산출물만으로 U6 인프라 구성 가능함 확인** ✅

### 적대적 검증 시도 결과

다음 결함이 존재한다고 가정하고 반증을 시도했으나 **모두 발견에 실패**했습니다:

1. **BF-1 미해소**: `final_report(cohort_id, created_at)` 구버전 존재 → 전체 워크스페이스 grep 0건, BF-1 **완전 해소 확인** ✅
2. **인덱스 필드명 불일치**: 엔티티 `submittedAt` vs 인덱스 `created_at` → 모두 `submitted_at`(snake_case DB 컬럼명)로 정합 ✅
3. **크로스유닛 인덱스 불일치**: U5 소유 vs U6 요구 불일치 → U5 `final_report(cohort_id, submitted_at)` = U6 요구 `final_report(submitted_at)` 정합 (복합 인덱스가 단일 필드 커버) ✅
4. **U1 상속 주장 미검증**: U6이 상속 주장한 항목이 U1에 없음 → U1 deployment-architecture에 모든 상속 항목 존재 확인 ✅
5. **성능 목표 달성 조건 불명확**: 인덱스 없이도 목표 달성 가능하다고 주장 → "인덱스 없으면 목표 보장 안됨" 명시, 전제 조건 명확 ✅
6. **순환 의존**: U6 ← 다른 유닛 호출 → shared-infrastructure §2 "어떤 유닛도 U6 호출 안함, DAG 종착점" 명시, 순환 불가능 ✅
7. **백업 공백**: U6 이력 파일 백업 안됨 → U1 uploads 볼륨 백업에 포함, U6 읽기만 하는 파일이므로 커버됨 ✅
8. **구현 공백**: 개발자가 5개 산출물로 구성 불가능 → 배포 토폴로지·인프라 서비스·모니터링·CI/CD·공유 인프라 모두 명시, 구현 가능 ✅

### 결론

U6-admin-metrics infrastructure-design는 **BF-1이 완전히 해소**되었으며, U1 상속 계약, U2~U5 소스 유닛 인덱스 계약, nfr-design 5종 정합성, functional-design 읽기 전용 모델 정합성을 모두 충족했습니다. 개발자가 5개 infrastructure-design 산출물만으로 U6 인프라를 구성할 수 있음을 확인했습니다.

파일럿 보류 항목(지표 캐시·머티리얼라이즈드 뷰·사전 집계)은 명시적으로 스코프아웃되었고 확장 트리거(데이터 수만+ 건 또는 목표 지속 초과)가 문서화되어 있습니다.

**판정: READY**
