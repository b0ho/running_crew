# Code Generation Plan — U5 completion (LearnKK 파일럿, Bolt 5)

> Construction · code-generation 단계 계획 · 유닛 U5-completion (복잡도 M)
> 리드 aidlc-developer-agent, 리뷰어 aidlc-architecture-reviewer-agent
> 상위 입력: functional-design(business-logic-model·business-rules·domain-entities·frontend-components), nfr-design(performance·security), infrastructure-design(deployment), units-generation/unit-of-work(U5), requirements(FR-7/8/9)
> 기반: U1(FileStorageService)·U2(Cohort/Session·closeByCompletion·읽기)·U3(confirmedEnrollments·notify)·U4(회차 인증 상태) 재사용.
> 핵심: 코호트 종료 오케스트레이션의 원자성(INV-U5-3) — 수료 판정+수료증 N장+정산+상태전이+알림이 모두 커밋되거나 모두 롤백.

## 크로스유닛 계약 (모두 기존 제공 — 호출만)

| 방향 | 계약 | 상태 |
|---|---|---|
| U5 → U2 (읽기) | Cohort·Session(전체/인증 회차 수) | 기존 제공(SessionRepository/CohortRepository) |
| U5 → U2 (호출) | `CohortService.closeByCompletion(cohortId)` 종료됨 가드 UPDATE | 기존 제공(U2) |
| U5 → U3 (읽기) | `EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>` | 기존 제공(U3) |
| U5 → U3 (호출) | `NotificationService.notify(userId, type, message)`(COMPLETION_RESULT) | 기존 제공(U3, NotificationType에 COMPLETION_RESULT 존재) |
| U5 → U1 (호출) | `FileStorageService.store/load/delete` | 기존 제공(U1) |
| U6 → U5 (읽기) | 수료율·증서 수·보고서 이력 | U5 제공 |

## 원자성/보상 설계 (INV-U5-3 — self-invocation 회피, N건 보상)

- `CompletionService.endCohort`(**비 @Transactional 오케스트레이션**): 사전검증(404·403·409) → U2 회차 집계 + U3 확정 멘티 목록 → 수료 판정(정수 비교) → (수료 시)멘티별 수료증 이미지 생성 + `FileStorageService.store`(imagePath 누적) → try{ `completionWriter.finalizeEnd(...)` }catch{ 누적 imagePath 전부 delete + 실패 시 ORPHAN 로그; throw }
- `CompletionWriter`(**@Transactional 별도 빈**): Certificate insert(사전조회 후 없으면 — 재종료 skip) + SettlementStatus upsert + `cohortService.closeByCompletion(cohortId)`(상태전이) + 각 확정 멘티 `notificationService.notify`(COMPLETION_RESULT). 동일 트랜잭션 원자.

## 테스트 전략 (Comprehensive + team.md 정련)

핵심 도메인(종료 원자성·80% 경계·정산 판정·멱등·보상) 80% 목표. 단위(Mockito): 종료 사전검증, **수료 79/80% 경계**(정수 비교), 정산 조건, 재종료 멱등. 통합(Testcontainers): 종료 트랜잭션 원자성(수료증 N장+정산+상태전이+알림 함께 커밋), 롤백 시 N건 이미지 보상, 보고서 첨부 보상. FE Jest/RTL.

---

## PART A — 백엔드 (learnkk-api)

### Step 1: DB 스키마 & Flyway 마이그레이션 (domain-entities §2~4)
- [ ] `V5__completion.sql`:
  - `final_report`(id, cohort_id FK CASCADE, author_id FK→users RESTRICT, body TEXT NOT NULL, file_path VARCHAR(512) NULL, submitted_at TIMESTAMP NOT NULL DEFAULT now())
  - `certificate`(id, cohort_id FK CASCADE, mentee_id FK→users RESTRICT, image_path VARCHAR(512) NOT NULL, issued_at TIMESTAMP NOT NULL DEFAULT now(), **UNIQUE(cohort_id, mentee_id)**)
  - `settlement_status`(id, cohort_id FK CASCADE **UNIQUE**, mentor_id FK→users RESTRICT, satisfied BOOLEAN NOT NULL, evaluated_at TIMESTAMP NOT NULL DEFAULT now())
  - 인덱스: `final_report(cohort_id, submitted_at)`(이력 페이지네이션), certificate·settlement은 UNIQUE로 조회 충족
- 트레이스: FR-7/8/9, INV-U5-1/2, domain-entities §2~4

### Step 2: 엔티티 (domain-entities §2~4)
- [ ] `com.learnkk.completion.FinalReport`(cohortId·authorId·body·filePath·submittedAt, 팩토리, @PrePersist submittedAt)
- [ ] `com.learnkk.completion.Certificate`(cohortId·menteeId·imagePath·issuedAt, 팩토리)
- [ ] `com.learnkk.completion.SettlementStatus`(cohortId·mentorId·satisfied·evaluatedAt, 팩토리 + `updateSatisfied` 세터 for upsert)
- [ ] 모두 private 생성자 + static 팩토리 + getter만, Entity 미노출
- 트레이스: domain-entities §2~4

### Step 3: 리포지토리
- [ ] `FinalReportRepository`: `Page<FinalReport> findByCohortIdOrderBySubmittedAtDesc(cohortId, Pageable)`, `existsByCohortIdAndAuthorId(cohortId, mentorId)`(정산 판정 mentorReportExists)
- [ ] `CertificateRepository`: `findByCohortIdAndMenteeId(...)`(사전조회 멱등), `countByCohortId(...)`(요약)
- [ ] `SettlementStatusRepository`: `findByCohortId(...)`(upsert)
- 트레이스: performance-design §3, R-U5-11

### Step 4: DTO (API 경계)
- [ ] `ReportDto`(id·cohortId·authorId·body·첨부 여부·submittedAt), `CertificateDto`(id·cohortId·menteeId·issuedAt), `CohortEndSummaryDto`(certifiedCount·notCertifiedCount·totalConfirmed·settlementSatisfied·issuedCertificateCount), `ReportSubmitRequest`(body @NotBlank)
- [ ] `from(entity)` 정적 팩토리
- 트레이스: R-U5-15, INV-U5

### Step 5: 수료증 렌더러 (performance-design §2, security-design §3)
- [ ] `com.learnkk.completion.CertificateRenderer`: Java2D `BufferedImage`+`Graphics2D`로 수료증 PNG 생성(멘티 성명·코호트명·발급일 임베드). `java.awt.headless=true`. 한글 렌더링용 Hangul 폰트를 `resources/fonts/`에 번들(OFL 라이선스 폰트, 예: NotoSansKR)하고 `Font.createFont`로 로드. `javax.imageio.ImageIO`로 PNG 바이트 인코딩(외부 이미지 라이브러리 의존 없음). 멘티별 순차 생성·참조 해제(메모리 효율)
- 트레이스: FR-8, performance-design §2, security-design §3(PII 최소)

### Step 6: 서비스 레이어 (business-logic-model §2~4)
- [ ] `ReportService`:
  - `submit(userId, cohortId, ReportSubmitRequest, MultipartFile?): ReportDto` — 참여자 확인, body 필수, 첨부 있으면 U1 store → [TX insert] → 롤백 시 delete 보상(U4 패턴). 첨부 없으면 순수 DB TX
  - `historyOf(cohortId, requesterId, isAdmin, Pageable): Page<ReportDto>` — 참여자·관리자(R-U5-19)
  - `mentorReportExists(cohortId, mentorId): boolean` — 정산 판정용
- [ ] `CompletionWriter`(@Transactional 별도 빈): `finalizeEnd(cohortId, mentorId, certifyData[], settlementSatisfied, confirmedMenteeIds[])` = Certificate insert(사전조회 멱등) + SettlementStatus upsert + `cohortService.closeByCompletion(cohortId)` + 각 멘티 notify(COMPLETION_RESULT)
- [ ] `CompletionService`(오케스트레이션):
  - `endCohort(mentorId, cohortId): CohortEndSummaryDto` — 비TX: 사전검증(404·소유 403·진행중 아님 409) → 회차 집계(total/verified, total==0이면 500) → U3 confirmedEnrollments → 수료 판정(verified*100>=total*80) → 수료 시 멘티별 이미지 생성+store(imagePath 누적) → 정산 판정(verified==total && mentorReportExists) → try{writer.finalizeEnd}catch{누적 imagePath delete 보상; 500}
  - `certificateOf(cohortId, requesterId, isAdmin): (Resource, filename)` 또는 CertificateDto+다운로드 — 본인 멘티·관리자(security-design §1)
- [ ] Cohort/Session 읽기·closeByCompletion은 U2 주입. 확정 멘티는 U3 EnrollmentService/Repository. 알림은 U3 NotificationService
- 트레이스: business-logic-model §2~4, R-U5-01~20, INV-U5-1~5

### Step 7: 컨트롤러 (frontend-components §3)
- [ ] `CompletionController`:
  - `POST /api/cohorts/{cohortId}/end` → 200 CohortEndSummaryDto (endCohort, 멘토)
  - `GET /api/cohorts/{cohortId}/certificate` → 수료증 조회/다운로드(본인 멘티·관리자, 스트리밍)
- [ ] `ReportController`:
  - `POST /api/cohorts/{cohortId}/reports` (multipart) → 201 ReportDto
  - `GET /api/cohorts/{cohortId}/reports` (page) → 200 Page<ReportDto>
- [ ] 사용자 id는 CurrentUserProvider. springdoc @Operation(한글)
- 트레이스: frontend-components §3

### Step 8: 백엔드 테스트 (Comprehensive)
- [ ] `CompletionServiceTest`(단위): endCohort 사전검증(404·403·409), **수료 경계**(total=5 verified=4 수료 / verified=3 미수료; total=10 verified=8/7), 정산 조건(verified==total && 멘토보고서), 재종료 멱등(기존 수료증 skip), **롤백 시 이미지 보상 delete 호출**
- [ ] `ReportServiceTest`(단위): submit body 필수·참여자·첨부 보상, historyOf 권한, mentorReportExists
- [ ] `CertificateRendererTest`(단위): PNG 바이트 생성(비어있지 않음)·한글 문자열 렌더 예외 없음
- [ ] `CompletionIntegrationTest`(Testcontainers): 종료 원자성(수료증 N장+정산+status 종료됨+알림 함께 커밋), **80% 경계 실 DB 검증**, 롤백 시 N건 이미지 보상, 보고서 첨부 저장·이력, 재종료 멱등
- [ ] ArchUnit DTO 경계는 기존 자동 커버
- 트레이스: NFR-6, INV-U5-3/5, R-U5-08a

### Step 9: OpenAPI 계약 동기화
- [ ] springdoc 확인, FE types.ts 일치

---

## PART B — 프론트엔드 (learnkk-web)

### Step 10: API 클라이언트 & 타입
- [ ] `api/types.ts` 추가: ReportDto·CertificateDto·CohortEndSummaryDto·ReportSubmitRequest
- [ ] `api/completionApi.ts`: endCohort, getCertificate(blob), submitReport(multipart), listReports
- 트레이스: frontend-components §3

### Step 11: 컴포넌트 — U2 CohortDetailPage 확장 (frontend-components §2)
- [ ] `completion/EndCohortButton.tsx` + `EndCohortDialog.tsx`(멘토 전용, 되돌릴 수 없음 고지, 포커스 트랩·키보드 접근) → 성공 시 `CohortEndSummary`
- [ ] `completion/CohortEndSummary.tsx`(수료자/전체·정산 충족·발급 증서 수)
- [ ] `completion/ReportForm.tsx`(body + 선택 첨부 FileDropzone 재사용) + `ReportList.tsx`(이력, 첨부 다운로드) → U2 CohortDetailPage "보고서" 탭 채움
- [ ] `completion/CompletionResult.tsx`(멘티: 수료/미수료 배너 색+텍스트 + 수료증 다운로드)
- [ ] U2 CohortDetailPage에 종료 버튼(진행중·멘토)·보고서 탭·수료 결과 연결
- 트레이스: frontend-components §2, 접근성 §4

### Step 12: 프론트엔드 테스트 (Jest/RTL)
- [ ] `completion/EndCohortDialog.test.tsx`(확인·409/403 에러)
- [ ] `completion/ReportForm.test.tsx`(body 필수·첨부·제출)
- [ ] `completion/CompletionResult.test.tsx`(수료/미수료 배너·수료증 다운로드)
- [ ] `api/completionApi.test.ts`
- 트레이스: NFR-6

---

## Step 13: 코드 요약 산출
- [ ] `code-summary.md`: 생성/수정 파일, 핵심 결정(종료 원자성 writer 분리·N건 이미지 보상·정수 경계 판정·정산·멱등·렌더러 폰트), 테스트 결과, 계획 대비 편차

## 산출물(코드) 위치
- 백엔드: `learnkk-api/src/main/java/com/learnkk/completion/**`, `resources/fonts/`(수료증 폰트), `resources/db/migration/V5__*.sql`, 테스트 `.../completion/**`
- 프론트: `learnkk-web/src/completion/**`, `src/api/{completionApi.ts,types.ts}`, U2 `cohorts/CohortDetailPage.tsx` 보고서 탭·종료 버튼 갱신
- 애플리케이션 코드는 워크스페이스 루트 하위에만. 레코드 디렉터리에는 계획·요약만.
