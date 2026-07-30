# 단위 테스트 지침 (unit-test-instructions) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · 리드 QUALITY · Test Strategy: **Comprehensive**
> 입력: U1~U6 `code-summary.md`·`code-generation-plan.md`, team.md(Testing Posture), phases/construction.md(Testing Standards)
> 도구: 백엔드 **JUnit 5 + Mockito**, 프론트 **Jest + React Testing Library(RTL)**. 커버리지 목표: **핵심 도메인 로직 80% 라인**(DTO·getter/setter·설정·단순 UI 마크업 제외).

## 1. 컴포넌트별 단위 테스트 접근

### 1.1 백엔드 (JUnit 5 + Mockito)

- **서비스 계층 중심**: 리포지토리·크로스유닛 포트·`FileStorageService`를 Mockito로 목킹하고, 서비스의 비즈니스 규칙(인가·상태 전이·경계 판정·예외 매핑)을 순수 단위로 검증한다. 트랜잭션·실 DB는 통합 테스트(Testcontainers)로 분리한다.
- **검증 스타일**: happy path + 최소 2개 이상의 오류/경계 케이스(phases/construction.md Testing Standards). 항상 통과하는 테스트(`assert true`) 금지.
- **구조 규칙 강제**: `ArchitectureTest`(ArchUnit)가 `com.learnkk` 전체 `@RestController`의 반환 타입이 `@Entity`가 아님을 검증(DTO 경계, INV-1). 유닛 추가 시 자동 커버.
- **동시성 검증**: 선착순 참여/정원·상태 전이 등 진성 동시성은 `ExecutorService` + `CountDownLatch`로 통합 테스트에서 검증한다(단위 범위 밖 → integration-test-instructions.md).

### 1.2 프론트엔드 (Jest / RTL)

- **행위 기반**: 사용자 관점(폼 검증·제출·에러 표시·라우팅 가드·페이지네이션·다운로드 트리거)을 RTL로 검증. API 클라이언트는 목킹.
- **접근성 훅**: `data-testid`·`role`·`aria-live` 기반 쿼리로 접근성 계약도 함께 검증(예: Toast `role="status"`, 진도바 `role="progressbar"`).
- **중앙 에러 정규화**: `ApiClient` 래퍼의 에러 정규화(`ApiError`)·`credentials: include`를 단위로 검증.

## 2. 실행 명령 (execution commands)

```bash
# 백엔드 단위 테스트(통합 제외)
cd learnkk-api && ./gradlew test -PexcludeIntegration
#   포맷 검사까지: ./gradlew spotlessJavaCheck compileTestJava test -PexcludeIntegration
#   특정 클래스만:  ./gradlew test --tests "com.learnkk.cohort.CohortServiceTest" -PexcludeIntegration

# 프론트엔드 단위 테스트
cd learnkk-web && npm test -- --ci --watchAll=false
#   특정 파일만:    npm test -- --ci --watchAll=false CohortForm
```

- `-PexcludeIntegration`은 `@Tag("integration")` 통합 테스트를 제외한다(로컬 Docker 부재 대응).
- 프론트는 `--watchAll=false`/`--ci`로 워치 모드 없이 1회 실행한다(장기 실행 프로세스 금지).

## 3. 커버리지 목표 (coverage targets)

- **핵심 도메인 로직 80% 라인 커버리지 목표**(team.md): 선착순 참여·정원 마감, 출석 인증, 집계, 인증/인가, 수료·정산 판정.
- **제외**: DTO·record·getter/setter·설정 클래스·단순 UI 마크업(커버리지 요구 대상 아님).
- **게이트 운영**: 80%는 획일적 하드 게이트가 아니라 핵심 모듈 팀 목표치. 전역 머지 게이트는 **린트 + 테스트 그린**(team.md Testing Posture, project.md Mandated CI).
- 정량 커버리지 리포트(JaCoCo 등)는 파일럿에서 필수 산출물로 강제하지 않으며, 아래 §4 유닛별 인벤토리로 핵심 규칙 커버를 추적한다.

## 4. 유닛별(U1~U6) 커버리지 인벤토리

각 유닛 `code-summary.md`의 테스트 목록을 근거로 정리한다. 백엔드 단위 테스트 클래스는 로컬에서 전량 실행되며(build-test-results.md), 통합 테스트는 CI 위임이다.

### U1 foundation (공통 인프라·인증·RBAC 시드)
| 테스트 클래스 | 건수 | 커버 규칙/영역 |
|---|---|---|
| `auth.AuthServiceTest` | 8 | 가입(정상·중복 409·검증·isAdmin 강제 false), 로그인(성공·ROLE_ADMIN·미존재/불일치 동일 401) — R-U1-01~09 |
| `seed.AdminSeederTest` | 4 | fail-fast·멱등·BCrypt·isAdmin=true — R-U1-25~27 |
| `file.FileStorageServiceTest` | 7 | MIME/크기/UUID/load/경로이탈/멱등 delete — R-U1-21~24 |
| `arch.ArchitectureTest` | 1 | 컨트롤러 Entity 미노출(전 유닛 자동 커버) — INV-1 |
| (FE) `ApiClient`·`LoginForm`·`SignupForm`·`RequireAuth` | 13 | 에러 정규화·credentials·검증·401 동일 문구·라우트 가드 |

### U2 cohort (코호트/회차/공지)
| 테스트 클래스 | 건수 | 커버 규칙/영역 |
|---|---|---|
| `cohort.CohortServiceTest` | 14 | 개설·소유권 403·CLOSED 409·정원 축소 409/경고·회차 조정 락 409·start 전이·get 권한 — R-U2-01~13,19,20 |
| `cohort.SessionServiceTest` | 3 | 회차 인증 전이(예정→인증)·멱등·미존재 — U4/U5 계약(markVerified) |
| `cohort.AnnouncementServiceTest` | 5 | 공지 작성 소유권·조회 권한(종료 코호트)·최신순 — R-U2-15~20 |
| `common.validation.SafeExternalUrlValidatorTest` | 3 | 외부링크 스킴 화이트리스트·상대URL 거부 — security-design §3 |
| (FE) `CohortForm`·`AnnouncementForm`·`CohortDetailPage`·`cohortApi` | — | 폼 검증·제출·요청 경로 |

### U3 enrollment (선착순 참여·대기·알림 · 최대 리스크)
| 테스트 클래스 | 건수 | 커버 규칙/영역 |
|---|---|---|
| `enrollment.EnrollmentServiceTest` | 7 | 정원 여유 확정·정원 마감 대기·중복 409·self 409·종료 409·락 타임아웃 409·404 — R-U3-05~08 |
| `enrollment.AdminApprovalServiceTest` | 5 | 승인/거절 상태 전이·이미 처리 409·정원 초과 승인 허용·알림 1건 — R-U3-11~13 |
| `enrollment.NotificationServiceTest` | 3 | 본인 소유 읽음·타인 알림 404·기본 메시지 |
| (FE) `JoinButton`·`MyApplicationsPage`·`NotificationBell`·`WaitingList`·`enrollmentApi` | — | 참여·본인 스코프·알림·대기 승인 UI |

### U4 attendance (증빙 업로드·회차 인증 원자성)
| 테스트 클래스 | 건수 | 커버 규칙/영역 |
|---|---|---|
| `attendance.AttendanceServiceTest` | 10 | 사전검증 404/403/409/400·성공 위임·**보상 delete**·진도율·참여자 인가 — R-U4-01/09/11, INV-U4-1 |
| `attendance.FileSignatureValidatorTest` | 7 | 매직바이트(JPEG/PNG/PDF) 통과·위조 거부·크기/빈 파일 거부·MIME 교차검증 — security-design §2 |
| (FE) `ProgressSummary`·`SessionAttendanceList`·`SessionEvidenceUpload`·`attendanceApi` | — | 업로드·진도바·다운로드 링크 |

### U5 completion (종료 오케스트레이션·수료·정산·보고서·수료증)
| 테스트 클래스 | 건수 | 커버 규칙/영역 |
|---|---|---|
| `completion.CompletionServiceTest` | 11 | 사전검증 404/403/409·회차0 500·수료 경계(4/5,3/5,8/10,7/10)·정산 조건·이미지 다건 보상 — INV-U5-3/5, R-U5-06/08a/11 |
| `completion.ReportServiceTest` | 7 | 참여자 인가·본문 필수 400·첨부 store·첨부 롤백 보상·mentorReportExists |
| `completion.CertificateRendererTest` | 2 | 한글/null 렌더 예외 없음·유효 PNG 생성 |
| (FE) `EndCohortDialog`·`ReportForm`·`CompletionResult`·`completionApi` | — | 종료 확인·보고서 제출·수료 배너/다운로드 |

### U6 admin-metrics (읽기 전용 지표·이력)
| 테스트 클래스 | 건수 | 커버 규칙/영역 |
|---|---|---|
| `metrics.MetricsServiceTest` | 5 | 정상 산식·출석률 분모0→0%·수료율 확정0→0%·반복소수 반올림·certificateCount — INV-U6-3, R-U6-04~07 |
| `metrics.HistoryServiceTest` | 5 | 기본 20·음수 page 정규화·cohortId 필터(null 포함)·hasAttachment 전달 — R-U6-09/10 |
| (FE) `MetricsOverview`·`EvidenceHistoryTable`·`ReportHistoryTable`·`adminMetricsApi` | — | 지표 카드·이력 테이블·페이지네이션 |

**백엔드 단위 테스트 총계: 18개 클래스, 107건**(로컬 전량 실행·통과, build-test-results.md 참조). 프론트 단위 테스트: 25개 스위트, 94건.

## 5. 상위 산출물 참조

- 각 유닛 테스트의 상세 케이스 목록·규칙 트레이스: U1~U6 `code-summary.md` §테스트.
- 테스트 대상 규칙(R-Ux-xx·INV-Ux-x): 각 유닛 `functional-design/business-rules.md`.
- 통합·동시성·성능·보안 테스트는 각각 integration/performance/security-test-instructions.md를 참조한다.
