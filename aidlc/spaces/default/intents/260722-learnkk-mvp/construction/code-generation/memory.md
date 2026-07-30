# Code Generation — 관찰 일지 (memory)

> Construction · code-generation 단계 진행 일지. 유닛별 반복(Bolt).
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-28T02:00:00Z — U1은 워킹 스켈레톤 Bolt(team.md·skeleton stance on). 관통 경로(가입→로그인→인증 호출 200→관리자 스텁 200)를 실제 실행 가능하게 하는 최소 슬라이스로 계획. 관리자 전용 스텁 엔드포인트 1개를 관통 검증용으로 포함.
- 2026-07-28T02:00:00Z — 관리자 시드는 domain-entities §6이 "Flyway 시드"라 하나, env 주입 + BCrypt 해싱 접근 용이성을 위해 Flyway 이후 실행되는 Spring ApplicationRunner 시더로 계획(멱등·fail-fast·env·평문 커밋 금지 규칙은 동일 유지). Java 기반 Flyway 마이그레이션도 허용 대안으로 명시.

## Deviations

- 2026-07-28T02:00:00Z — Construction 질문 라운드는 생략하나, code-generation은 Step 3 Plan Approval이 모든 모드에서 필수 하드 스톱이므로 계획 승인 게이트는 반드시 제시(질문 생략 규칙과 별개).

## Tradeoffs

- 2026-07-28T02:00:00Z — FE/BE 분리 저장소는 워크스페이스 루트 하위 디렉터리(learnkk-api/·learnkk-web/)로 실체화(별도 git 저장소 대신 파일럿 단일 워크스페이스 내 분리). 근거: 파일럿 단일 워크스페이스에서 관리 단순화, 계약 동기화(OpenAPI)는 유지. 실제 분리 저장소 분할은 확장 시.

## Open questions

- 2026-07-28T02:00:00Z — 세션 쿠키 정책에서 CSRF 처리(SameSite=Lax 우선 + 상태변경 보호)의 구체 방식은 개발자 에이전트 구현에서 확정. Testcontainers 실행에 Docker 필요 — CI/로컬 환경 전제.

## Tradeoffs (U1 Bolt 실행 후 추가)

- 2026-07-28T02:30:00Z — Testcontainers 통합 테스트를 gradle로 직접 실행하지 못함(호스트 Rancher Desktop 소켓 `~/.rd/docker.sock`를 빌드 gradle 컨테이너에 bind-mount 불가). 대체로 실 compose 스택(실 PostgreSQL 16) 라이브 스모크로 동일 시나리오(관통·401·403·400·409·시드·fail-fast) 전량 검증. 통합 테스트 코드는 완성되어 CI(Docker 소켓 가용)에서 `./gradlew test`로 실행 가능. 워킹 스켈레톤 게이트는 "관통 동작"을 요구하므로 라이브 스모크로 충족.
- 2026-07-28T02:30:00Z — 관리자 시드는 계획대로 Spring ApplicationRunner(AdminSeeder)로 구현. env 미설정 시 IllegalStateException으로 부팅 중단(R-U1-27) 라이브 확인.

## Open questions (U1 Bolt)

- 2026-07-28T02:30:00Z — CI(GitHub Actions) 워크플로 파일은 U1 코드 범위 밖으로 두었다(설계는 cicd-pipeline.md에 존재, FE/BE 분리 저장소 분할 시 각 저장소 배치). CI에서 통합 테스트를 `-PexcludeIntegration` 없이 전량 실행하는 것이 전제. 언제 실제 GitHub Actions 파일을 생성할지(어느 유닛/Bolt) 결정 필요.

## Interpretations (U2-cohort Bolt)

- 2026-07-30T05:40:00Z — U3(enrollment)가 아직 빌드되지 않았으므로, business-logic-model §8의 크로스유닛 읽기 계약 `EnrollmentService.confirmedCount(cohortId): int`를 U2 안에 포트 인터페이스(`ConfirmedEnrollmentQuery`)로 정의하고, 파일럿 기본 구현(0 반환, @ConditionalOnMissingBean)을 제공해 U2가 단독 컴파일·구동·테스트되게 한다. U3 빌드 시 실제 빈이 이 포트를 구현해 대체한다. 순환 없음(U2→U3 읽기 전용).
- 2026-07-30T05:40:00Z — 현재 인증 사용자 id 해석: U1은 principal.name=email만 노출하므로, `com.learnkk.common.security.CurrentUserProvider`(SecurityContext email → UserRepository 조회 → User id)를 신설한다. 컨트롤러가 이를 통해 mentorId를 얻어 서비스에 전달(소유권 검증 R-U2-07/15).
- 2026-07-30T05:40:00Z — 상태 전이(모집중→진행중, 진행중→종료됨 세터)는 project.md 학습(cid:nfr-design:state-transition-guarded-update)에 따라 @Version 대신 상태 가드 조건 UPDATE(`UPDATE cohort SET status=? WHERE id=? AND status=?`, 영향 행 0이면 409 INVALID_STATE_TRANSITION)로 구현.

## Deviations (U2-cohort Bolt)

- 2026-07-30T05:40:00Z — U2는 서브에이전트(aidlc-developer-agent) 디스패치 모드. 리뷰어(aidlc-architecture-reviewer-agent)·산출물·## Review는 team.md Mandated(모든 산출물 한글)에 따라 한글로 작성하도록 브리핑에 명시(project.md cid:nfr-design:lang-subagent).

## Tradeoffs (U2-cohort Bolt)

- 2026-07-30T05:40:00Z — `SessionService.markVerified(sessionId)`(U4가 호출할 회차 인증 전이 경로)는 U4 미빌드 상태에서도 U2가 지금 제공한다(계약 선노출). U2 테스트는 이 전이를 직접 검증하고, U4는 이후 이 메서드를 호출한다(리포지토리 직접 접근 금지 — 캡슐화).

## Deviations (U2-cohort Bolt — 실행 후)

- 2026-07-30T06:20:00Z — 상세 조회를 nfr-design의 "fetch join 또는 @BatchSize" 대신 Session 스칼라 cohortId + 상수 3쿼리 분리로 실체화(회차/최근공지5). N+1 보증 동등, 통합 테스트가 쿼리 카운트 ≤4 단언. 근거: Session을 스칼라 cohortId로 모델링(양방향 연관 회피)이 파일럿에 단순.
- 2026-07-30T06:20:00Z — 회차 batch insert는 IDENTITY(BIGSERIAL) 전략이라 Hibernate JDBC 배치 비활성. sessionCount ≤100 상한으로 파일럿 무해, 확장 시 SEQUENCE 재검토. (반복 학습 후보: IDENTITY vs SEQUENCE batch insert 트레이드오프.)
- 2026-07-30T06:20:00Z — 공지 조회 참여자 필터(R-U2-18)는 확정 멘티 판정이 U3 데이터 의존이라 파일럿에서 소유 멘토/관리자 + 진행중·모집중 공개로 완화. U3 빌드 시 멤버십 필터 강화(javadoc 명시). (크로스유닛 미빌드 의존의 파일럿 완화 패턴.)

## Open questions (U2-cohort Bolt)

- 2026-07-30T06:20:00Z — Testcontainers 통합 테스트는 Docker 필요로 본 세션 미실행(코드 완성, 단위 45건·FE 28건 green). U1과 동일하게 CI(Docker 가용)에서 전량 실행 전제. GitHub Actions 워크플로 파일 생성 시점(어느 유닛)은 여전히 미결.
- 2026-07-30T06:20:00Z — Bolt 게이트 정책: team.md는 "매 Bolt 게이트"이나 엔진은 code-generation을 per-unit 단일 게이트(마지막 유닛)로 구동. U2는 gate:false로 게이트 없이 진행. 엔진 라우팅을 따름(재도출 금지).

## Interpretations (U3-enrollment Bolt)

- 2026-07-30T06:35:00Z — U3 join의 비관적 락 대상은 U2 소유 Cohort 행이다. business-logic-model §2 `findByIdForUpdate(cohortId)`를 U2 CohortRepository에 `@Lock(PESSIMISTIC_WRITE)` + `@QueryHints(jakarta.persistence.lock.timeout=3000)` 메서드로 추가(가산적 수정)하고, U3 EnrollmentService가 이를 주입해 사용. Cohort는 읽기 전용(U3→U2 읽기), 수정 없음.
- 2026-07-30T06:35:00Z — U3는 U2가 포트로 선언한 `ConfirmedEnrollmentQuery`의 실제 빈을 제공한다(어댑터가 EnrollmentRepository.countByCohortIdAndStatus(CONFIRMED) 위임). U2의 @ConditionalOnMissingBean 기본 빈(0 반환)이 자동으로 대체됨. 이로써 U2 정원 축소 검증(R-U2-09)이 실제 확정 인원으로 동작.
- 2026-07-30T06:35:00Z — 관리자 승인 경합은 nfr-design 확정대로 조건부 UPDATE(`UPDATE enrollment SET status=CONFIRMED WHERE id=? AND status=WAITING`, 영향 행 0이면 409 INVALID_STATE_TRANSITION)로 알림 중복을 방지. Enrollment.version(@Version) 컬럼은 스키마·엔티티에 두되 승인 경합의 1차 방어는 조건부 UPDATE.
- 2026-07-30T06:35:00Z — 락 타임아웃(3000ms) 초과 시 락 획득 실패 → 롤백 후 409 ENROLLMENT_BUSY 신규 예외/코드 추가(performance-design §2). Pessimistic lock 예외(LockTimeoutException/PessimisticLockException) → 409 매핑.

## Interpretations (U4-attendance Bolt)

- 2026-07-30T06:55:00Z — U1 FileStorageService는 store/load/delete를 이미 노출(delete는 멱등, 보상용). U2 SessionService.markVerified도 존재(멱등). 따라서 U4는 신규 계약 없이 기존 계약을 호출만 한다.
- 2026-07-30T06:55:00Z — 파일 저장(비TX) 후 DB 트랜잭션 롤백 시 보상 delete를 하려면 오케스트레이션 메서드가 @Transactional이 아니어야 하고, 원자 파트(Evidence 저장 + markVerified)는 별도 @Transactional 빈이어야 한다(동일 클래스 self-invocation 시 @Transactional 프록시 미적용 회피). 계획: AttendanceService.uploadEvidence(비TX 오케스트레이션: 사전검증→store→try{writer.persistAndVerify}catch{fileStorage.delete}) + AttendanceEvidenceWriter(@Transactional: 이력 저장 + sessionService.markVerified). INV-U4-1(인증↔증빙 원자성) 보장.
- 2026-07-30T06:55:00Z — 매직바이트 검증(JPEG FF D8 FF, PNG 89 50 4E 47, PDF 25 50 44 46)은 U4 AttendanceService에서 store 호출 전 수행(security-design §2 — U1 store는 확장자+선언MIME+크기만). 위반 시 FileConstraintViolationException(U1 400 핸들러 재사용).
- 2026-07-30T06:55:00Z — U4는 Session·Cohort를 읽기 위해 U2 SessionRepository/CohortRepository를 주입(U3가 CohortRepository 주입한 것과 동일 패턴, 읽기 전용). 회차 인증 전이는 SessionService.markVerified로만(리포 직접 수정 금지, INV-U4-4). 멀티파트 업로드용 ApiClient.postForm(멀티파트) 프론트 추가 필요.

## Open questions (U4-attendance Bolt)

- 2026-07-30T07:20:00Z — 전체 Testcontainers 통합 스위트를 한 Gradle 실행으로 돌리면 U2 CohortIntegrationTest·U3 EnrollmentIntegrationTest 등 12건이 실패(setUp이 userRepository를 정리하지 않아 지속형 컨테이너에서 email UNIQUE 위반). U4 회귀 아님(U4 통합은 격리 실행 시 그린). **Build and Test(3.6)에서 U2/U3 통합 테스트 setUp에 userRepository.deleteAll() 보강 필요** — 유닛 소유 경계상 U4 code-gen에서는 미수정, 3.6 전체 검증 단계에서 처리.

## Interpretations (U5-completion Bolt)

- 2026-07-30T07:40:00Z — 종료 오케스트레이션 원자성 + N건 이미지 보상: endCohort(비TX 오케스트레이션)가 사전검증 → U2/U3 읽기 → 수료 판정 → (수료 시)각 확정 멘티 수료증 이미지 생성+U1 store(imagePath 누적) → try{ completionWriter.finalizeEnd(@Transactional: Certificate insert(사전조회 후 없으면 insert — UNIQUE 예외 롤백 회피, 리뷰 Finding1)·SettlementStatus upsert·U2 closeByCompletion 상태전이·각 멘티 U3 notify) }catch{ 누적 imagePath 전부 FileStorageService.delete, 실패 경로 ORPHAN_FILE_COMPENSATION_FAILED 로그 }. self-invocation 회피 위해 writer 별도 빈(U4 패턴의 다건 확장).
- 2026-07-30T07:40:00Z — 크로스유닛 계약 모두 기존 제공: U2 closeByCompletion(종료됨 가드 UPDATE)·Session/Cohort 읽기, U3 confirmedEnrollments(확정 멘티 목록)·notify(COMPLETION_RESULT), U1 store/load/delete. U5는 신규 계약 요구 없음. 멘토 보고서 존재는 U5 자체 ReportService.mentorReportExists(cohortId, cohort.mentorId).
- 2026-07-30T07:40:00Z — 수료 판정은 정수 비교 verifiedSessions*100 >= totalSessions*80(부동소수 경계 오차 회피, INV-U5-5). 79/80% 경계 테스트 필수. totalSessions==0이면 500 정합 오류.
- 2026-07-30T07:40:00Z — 수료증 이미지: Java2D(BufferedImage+Graphics2D, java.awt.headless=true) PNG 생성. 한글(멘티 성명·코호트명) 렌더링 위해 Hangul 지원 폰트를 리소스에 번들(예: NotoSansKR-Regular 서브셋)하고 Font.createFont로 로드; 폰트 미가용 시 논리 폰트로 degrade하되 테스트는 번들 폰트 전제. 외부 이미지 라이브러리 의존 없이 표준 javax.imageio로 PNG 인코딩.

## Open questions (U5-completion Bolt)

- 2026-07-30T07:40:00Z — 수료증 폰트 번들 크기(NotoSansKR 전체는 큼) — 파일럿은 필요한 글리프만 담은 서브셋 또는 경량 Hangul 폰트 사용 검토. code-generation에서 실제 폰트 파일 확보/라이선스(OFL) 확인.
