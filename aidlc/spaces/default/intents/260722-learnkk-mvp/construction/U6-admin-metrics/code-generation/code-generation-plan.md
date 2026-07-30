# Code Generation Plan — U6 admin-metrics (LearnKK 파일럿, Bolt 6)

> Construction · code-generation 단계 계획 · 유닛 U6-admin-metrics (복잡도 S, 읽기 전용)
> 리드 aidlc-developer-agent, 리뷰어 aidlc-architecture-reviewer-agent
> 상위 입력: functional-design(business-logic-model·business-rules·domain-entities·frontend-components), nfr-design(performance·security), infrastructure-design(deployment), units-generation/unit-of-work(U6), requirements(FR-10/11)
> 기반: U2(Cohort/Session·status)·U3(Enrollment CONFIRMED·confirmedCount)·U4(AttendanceEvidence)·U5(Certificate/FinalReport) 데이터를 read-only로 집계·조회. 신규 도메인 테이블 없음.
> 핵심: U6은 **말단 소비자·읽기 전용**(INV-U6-1) — 어떤 데이터도 쓰지 않고, 어떤 유닛도 U6을 호출하지 않는다. 관리자 전용 인가(ROLE_ADMIN)와 지표 정확성(실시간 집계·0 나눗셈 안전·CLOSED 범위 일관)이 관심사.

## 크로스유닛 계약 (모두 read-only — 소스 스키마/서비스 조회만)

| 방향 | 계약 | 상태 |
|---|---|---|
| U6 → U2 (읽기) | 종료됨(CLOSED) 코호트·회차 수 | 기존 스키마(cohort/session) 조회 |
| U6 → U3 (읽기) | 확정 멘티 수(수료율 분모) | EnrollmentService.confirmedCount 존재 / 집계는 enrollment 테이블 직접 조회 |
| U6 → U4 (읽기) | 증빙 이력(Session·Cohort·User 조인) | attendance_evidence 조회 |
| U6 → U5 (읽기) | 증서 수·보고서 이력 | certificate/final_report 조회 |

- 파일럿 기본 구현: **리포팅 읽기 모델**(U6 소유 read-only 집계/조인 쿼리, 쓰기 없음, INV-U6-1). 다른 유닛 도메인 로직 우회 없음(순수 조회).

## 기준 인덱스 점검 (performance-design §2)

| 인덱스 | 현황 | 조치 |
|---|---|---|
| `cohort(status)` | 존재(`ix_cohort_status`, V2) | 재사용 |
| `enrollment(cohort_id, status)` | 존재(`ix_enrollment_cohort_status`, V3) | 재사용 |
| `certificate(cohort_id)` | UNIQUE(cohort_id, mentee_id) prefix로 충족(V5) | 재사용 |
| `attendance_evidence(created_at)` | 기존은 `(session_id, created_at)` — 전역 최신순 정렬 미충족 | **V6에서 `ix_attendance_evidence_created` 추가** |
| `final_report(submitted_at)` | 기존은 `(cohort_id, submitted_at)` — 전역 최신순 정렬 미충족 | **V6에서 `ix_final_report_submitted` 추가** |

## 테스트 전략 (Comprehensive + team.md 정련)

핵심 도메인(집계 산식·0 나눗셈·CLOSED 범위·관리자 인가·페이지네이션) 80% 목표. 단위(Mockito): overview 산식·분모 0 안전, evidence/report 이력 페이지네이션·cohortId 필터, 인가. 통합(Testcontainers): 실 DB에 종료 코호트·회차·증서·확정 멘티를 세팅해 지표가 실제 데이터와 일치(FR-11)·CLOSED 범위 일관·이력 조인/최신순/페이지네이션 검증. FE Jest/RTL.

---

## PART A — 백엔드 (learnkk-api)

### Step 1: 보조 인덱스 마이그레이션 (performance-design §2)
- [x] `V6__metrics_history_indexes.sql`:
  - `CREATE INDEX ix_attendance_evidence_created ON attendance_evidence (created_at)` — 증빙 이력 전역 최신순
  - `CREATE INDEX ix_final_report_submitted ON final_report (submitted_at)` — 보고서 이력 전역 최신순
- 신규 테이블 없음(U6은 읽기 전용). 인덱스만 보완.
- 트레이스: performance-design §2, INV-U6 성능 전제

### Step 2: 읽기 모델 DTO (domain-entities §3)
- [x] `com.learnkk.metrics.dto.MetricsOverviewDto`(completedCohortCount:int, attendanceRate:double, completionRate:double, certificateCount:long, scopeLabel:String "종료된 코호트 N건 기준")
- [x] `com.learnkk.metrics.dto.EvidenceHistoryItemDto`(evidenceId, sessionId, cohortTitle, sessionSeq, mimeType, size, uploadedBy(성명), createdAt) — 다운로드 링크 조립용 sessionId 추가
- [x] `com.learnkk.metrics.dto.ReportHistoryItemDto`(reportId, cohortId, cohortTitle, authorName, hasAttachment:boolean, submittedAt)
- [x] Entity 미노출, DTO 경계 준수(U1 Mandated)
- 트레이스: domain-entities §3, security-design §3

### Step 3: 집계 리포지토리 (business-logic-model §2)
- [x] `com.learnkk.metrics.MetricsRepository`(읽기 전용 집계 — JPQL/native):
  - `long countClosedCohorts()` = `COUNT(cohort WHERE status='CLOSED')`
  - `long[] closedAttendanceSums()` 또는 프로젝션 = `SUM(인증 회차), SUM(전체 회차)` over CLOSED 코호트 세션 (COALESCE 0)
  - `long countConfirmedMenteesOfClosed()` = `COUNT(enrollment JOIN cohort ON ... WHERE c.status='CLOSED' AND e.status='CONFIRMED')`
  - `long countCertificates()` = `COUNT(certificate)`
- 소스 테이블 read-only 조회만(쓰기 없음, INV-U6-1)
- 트레이스: business-logic-model §2, R-U6-04~07

### Step 4: 이력 조회 리포지토리 (business-logic-model §3/4)
- [x] `com.learnkk.metrics.HistoryRepository`(read-only 조인 프로젝션, N+1 회피):
  - `Page<EvidenceHistoryItemDto> findEvidenceHistory(Long cohortId /*nullable*/, Pageable)` — AttendanceEvidence×Session×Cohort×User 조인, createdAt desc
  - `Page<ReportHistoryItemDto> findReportHistory(Long cohortId /*nullable*/, Pageable)` — FinalReport×Cohort×User 조인, filePath!=null→hasAttachment, submittedAt desc
  - cohortId null이면 전체, 아니면 필터
- 트레이스: R-U6-09/10, performance-design §1

### Step 5: 서비스 레이어 (business-logic-model §2~4)
- [x] `com.learnkk.metrics.MetricsService`(@Transactional(readOnly=true)):
  - `overview(): MetricsOverviewDto` — 4개 집계, 출석률=verified*100.0/total(total=0→0), 수료율=certCount/confirmedMentees(분모0→0), 소수 1자리 반올림(표시 전용), scopeLabel 조립
- [x] `com.learnkk.metrics.HistoryService`(@Transactional(readOnly=true)):
  - `evidenceHistory(Long cohortId, int page, int size): Page<EvidenceHistoryItemDto>` (기본 size=20)
  - `reportHistory(Long cohortId, int page, int size): Page<ReportHistoryItemDto>` (기본 size=20)
- 인가는 컨트롤러 @PreAuthorize로. 서비스는 순수 조회
- 트레이스: business-logic-model §2~4, R-U6-04~11, INV-U6-2/3/4

### Step 6: 컨트롤러 (frontend-components §3, security-design §1)
- [x] `com.learnkk.metrics.MetricsController` — `GET /api/admin/metrics` → 200 MetricsOverviewDto, `@PreAuthorize("hasRole('ADMIN')")`
- [x] `com.learnkk.metrics.HistoryController`:
  - `GET /api/admin/history/evidence?cohortId=&page=&size=` → 200 Page<EvidenceHistoryItemDto>
  - `GET /api/admin/history/reports?cohortId=&page=&size=` → 200 Page<ReportHistoryItemDto>
  - 클래스 레벨 `@PreAuthorize("hasRole('ADMIN')")`
- [x] springdoc @Operation(한글). 비관리자 403·미인증 401은 U1 공통 핸들러/SecurityConfig 재사용
- 트레이스: frontend-components §3, R-U6-01/02, security-design §1

### Step 7: 백엔드 테스트 (Comprehensive)
- [x] `MetricsServiceTest`(단위): 출석률/수료율 정상 산식, **분모 0→0%**(회차 0·확정 멘티 0), CLOSED 범위 일관(진행중 제외 mock), certificateCount — 5 케이스 PASS
- [x] `HistoryServiceTest`(단위): evidenceHistory/reportHistory 페이지네이션 기본 20·최신순, cohortId 필터 유무, hasAttachment 계산 — 5 케이스 PASS
- [x] `MetricsIntegrationTest`(Testcontainers): 실 DB에 CLOSED 코호트·회차·증서·확정 멘티 세팅 → overview가 실제 데이터와 일치(FR-11), 진행중 코호트 제외 확인 — 작성·컴파일 완료(Docker 미가용 로컬 미실행, `@Tag("integration")`)
- [x] `HistoryIntegrationTest`(Testcontainers): 증빙/보고서 이력 조인·최신순·페이지네이션·필터 검증 — 작성·컴파일 완료(Docker 미가용 로컬 미실행)
- 트레이스: FR-11, R-U6-04~10, INV-U6-2~4

### Step 8: OpenAPI 계약 동기화
- [x] springdoc 확인, FE types.ts 일치 — @Operation(한글) 확인, `com.learnkk.metrics.dto` ↔ `api/types.ts`(MetricsOverviewDto·EvidenceHistoryItem·ReportHistoryItem) 필드명·타입 정합

---

## PART B — 프론트엔드 (learnkk-web)

### Step 9: API 클라이언트 & 타입
- [x] `api/types.ts` 추가: MetricsOverviewDto·EvidenceHistoryItem·ReportHistoryItem·Page 공통형(기존 재사용)
- [x] `api/adminMetricsApi.ts`: getMetrics, listEvidenceHistory(cohortId?,page), listReportHistory(cohortId?,page)
- 트레이스: frontend-components §3

### Step 10: 컴포넌트 — AdminPage 탭 확장 (frontend-components §1/2)
- [x] `admin/MetricCard.tsx`(수치+라벨 병기, 스크린리더 설명) + `admin/MetricsOverview.tsx`(카드 4개: 완주 코스 수/출석률/수료율/증서 수 + scopeLabel, 빈/로딩 상태)
- [x] `admin/EvidenceHistoryTable.tsx`(컬럼: 코호트·회차·업로더·형식·크기·업로드일·다운로드, 20건 페이지네이션·키보드 접근, 다운로드는 U1 load 경유 — `attendanceApi.evidenceDownloadUrl`)
- [x] `admin/ReportHistoryTable.tsx`(컬럼: 코호트·작성자·첨부유무·제출일, 별도 탭)
- [x] `admin/AdminPage.tsx` 확장: 기존 대기승인 탭(U3)에 지표·증빙 이력·보고서 이력 탭 추가(관리자 전용 가드 재사용)
- [x] 테이블 헤더 scope 지정, 로딩/빈 상태 명시(접근성 §4)
- 트레이스: frontend-components §1/2/4

### Step 11: 프론트엔드 테스트 (Jest/RTL)
- [x] `admin/MetricsOverview.test.tsx`(카드 4개 렌더·scopeLabel·0% 안전 표시) — 3 케이스 PASS
- [x] `admin/EvidenceHistoryTable.test.tsx`(행 렌더·페이지네이션·다운로드 링크) — 3 케이스 PASS
- [x] `admin/ReportHistoryTable.test.tsx`(첨부유무 표기·페이지 이동) — 3 케이스 PASS
- [x] `api/adminMetricsApi.test.ts`(엔드포인트·쿼리 파라미터) — 4 케이스 PASS
- 트레이스: NFR-6

---

## Step 12: 코드 요약 산출
- [x] `code-summary.md`: 생성/수정 파일, 핵심 결정(읽기 전용 리포팅 모델·집계 산식·0 나눗셈 안전·CLOSED 범위·관리자 인가·보조 인덱스), 테스트 결과, 계획 대비 편차

## 산출물(코드) 위치
- 백엔드: `learnkk-api/src/main/java/com/learnkk/metrics/**`(+dto), `resources/db/migration/V6__metrics_history_indexes.sql`, 테스트 `.../metrics/**`
- 프론트: `learnkk-web/src/admin/**`(MetricCard·MetricsOverview·EvidenceHistoryTable·ReportHistoryTable·AdminPage 확장), `src/api/{adminMetricsApi.ts,types.ts}`
- 애플리케이션 코드는 워크스페이스 루트 하위에만. 레코드 디렉터리에는 계획·요약만.
