# 빌드·테스트 실행 결과 (build-test-results) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · **Step 10 실제 실행 결과** · 실행 시각 2026-07-30T14:14Z 무렵 (UTC)
> 실행 머신: 로컬(macOS). **Docker 미가용**(`docker info` → `DOCKER_UNAVAILABLE`) → 통합 테스트 CI 위임.
> 정직성 원칙: 실패는 실패로 기록한다. 아래 결과는 실제 명령 출력에서 캡처했다.

## 1. 요약 (한눈에)

| 영역 | 명령 | 결과 | total / passed / failed / skipped |
|---|---|---|---|
| 백엔드 포맷+컴파일+단위 | `./gradlew spotlessJavaCheck compileTestJava test -PexcludeIntegration` | ✅ BUILD SUCCESSFUL | 107 / 107 / 0 / 0 |
| 프론트 단위 | `npm test -- --ci --watchAll=false` | ✅ PASS | 94 / 94 / 0 / 0 (25 suites) |
| 프론트 빌드 | `npm run build` (tsc --noEmit && vite build) | ✅ 성공 | — (82 modules, dist 생성) |
| 프론트 린트 | `npm run lint` (eslint --max-warnings=0) | ✅ 통과(exit 0) | 경고 0 |
| 통합 테스트(Testcontainers) | (미실행) | ⏸ CI 위임 | 로컬 Docker 미가용 |

**적용한 수정: 없음.** 모든 명령이 1차 실행에서 통과하여 재시도/수정이 필요하지 않았다.

## 2. 백엔드 — 빌드 + 단위 테스트

명령: `cd learnkk-api && ./gradlew spotlessJavaCheck compileTestJava test -PexcludeIntegration`

결과: **BUILD SUCCESSFUL in 8s** (7 actionable tasks).
- `spotlessJavaCheck`(Google Java Format) 통과.
- 단위/구조 테스트 **107건 전부 PASSED, 0 failed, 0 errors, 0 skipped**.
- 집계 근거: `build/test-results/test/*.xml` 18개 클래스의 `tests`/`failures`/`errors`/`skipped` 합산 = `tests=107 skipped=0 failures=0 errors=0`.
- `@Tag("integration")` 통합 테스트는 `-PexcludeIntegration`으로 제외됨(로컬 Docker 부재).

### 클래스별 통과 건수 (18개 클래스, 107건)

| 클래스 | 건수 | 유닛 |
|---|---|---|
| `arch.ArchitectureTest` | 1 | U1 |
| `auth.AuthServiceTest` | 8 | U1 |
| `seed.AdminSeederTest` | 4 | U1 |
| `file.FileStorageServiceTest` | 7 | U1 |
| `cohort.CohortServiceTest` | 14 | U2 |
| `cohort.SessionServiceTest` | 3 | U2 |
| `cohort.AnnouncementServiceTest` | 5 | U2 |
| `common.validation.SafeExternalUrlValidatorTest` | 3 | U2 |
| `enrollment.EnrollmentServiceTest` | 7 | U3 |
| `enrollment.AdminApprovalServiceTest` | 5 | U3 |
| `enrollment.NotificationServiceTest` | 3 | U3 |
| `attendance.AttendanceServiceTest` | 10 | U4 |
| `attendance.FileSignatureValidatorTest` | 7 | U4 |
| `completion.CompletionServiceTest` | 11 | U5 |
| `completion.ReportServiceTest` | 7 | U5 |
| `completion.CertificateRendererTest` | 2 | U5 |
| `metrics.MetricsServiceTest` | 5 | U6 |
| `metrics.HistoryServiceTest` | 5 | U6 |
| **합계** | **107** | U1~U6 |

> 참고: Gradle이 `Deprecated Gradle features were used`(Gradle 9.0 호환성) 경고를 출력했으나 빌드 실패와 무관한 정보성 경고다.

## 3. 프론트엔드 — 단위 테스트 / 빌드 / 린트

### 3.1 `npm test -- --ci --watchAll=false`
결과: **PASS**
```
Test Suites: 25 passed, 25 total
Tests:       94 passed, 94 total
Snapshots:   0 total
```
- 실패 0. 콘솔에 `React Router Future Flag Warning`(v7 상대 경로/스플랫 관련) `console.warn`이 출력되나, 이는 테스트 실패가 아닌 라이브러리 향후 버전 경고다(라우터 v7 마이그레이션 시 future flag로 해소 가능).
- `node_modules`가 이미 존재하여 `npm ci`는 실행하지 않음(과제 규칙: 부재 시에만).

### 3.2 `npm run build`
결과: **성공** (exit 0)
```
vite v5.4.21 building for production...
✓ 82 modules transformed.
dist/index.html                   0.39 kB
dist/assets/index-*.css          14.64 kB │ gzip: 3.50 kB
dist/assets/index-*.js          224.31 kB │ gzip: 68.62 kB
✓ built in 534ms
```
- `tsc --noEmit`(타입체크) 오류 0 후 `vite build` 성공.

### 3.3 `npm run lint`
결과: **통과** (`eslint . --max-warnings=0`, exit 0, 경고 0).

## 4. 통합 테스트 — 미실행 (CI 위임)

- **CI 위임(로컬 Docker 미가용)**. `docker info` 실행 결과 `DOCKER_UNAVAILABLE`.
- 대상: U1 `AuthIntegrationTest`, U2 `CohortIntegrationTest`, U3 `EnrollmentConcurrencyIntegrationTest`·`EnrollmentIntegrationTest`, U4 `AttendanceIntegrationTest`·`AttendanceCompensationIntegrationTest`, U5 `CompletionIntegrationTest`·`CompletionRollbackCompensationIntegrationTest`, U6 `MetricsIntegrationTest`·`HistoryIntegrationTest`.
- 이 테스트들은 각 유닛 code-generation 단계에서 작성·컴파일 완료 상태이며(U3·U4는 당시 Docker 가용 환경에서 실제 실행·통과 이력 있음), Docker 가용 CI에서 `-PexcludeIntegration` 없이 전량 실행한다(integration-test-instructions.md).

## 5. 성능/보안 실행 노트

- **성능 라이브 latency 스모크**: Docker 미가용으로 실 스택 기동 불가 → 로컬 미측정. 지배 비용(BCrypt·인덱스 조회·N+1 회피 상수 쿼리)은 설계·단위/구조 테스트로 확인. 통계 p95/부하는 Operation performance-validation 이관(performance-test-instructions.md).
- **보안 단위 검증**: 파일 매직바이트(`FileSignatureValidatorTest` 7)·경로 이탈(`FileStorageServiceTest`)·외부 링크(`SafeExternalUrlValidatorTest` 3)·사용자 열거 방지(`AuthServiceTest`)·본인 스코프(`NotificationServiceTest`)가 위 107건에 포함되어 통과. end-to-end 인가(403/401)는 CI 통합/라이브 스모크 위임(security-test-instructions.md).

## 6. 실패 / 미해결 (honest notes)

- **실패한 테스트: 없음**(백엔드 107/107, 프론트 94/94, 빌드·린트 통과).
- **로컬에서 실행하지 못한 것**: 통합 테스트(Testcontainers) 전량 — 로컬 Docker 미가용. → CI 위임으로 정직하게 기록.
- **정보성 경고(비차단)**: Gradle Deprecation 경고, React Router v7 future flag 경고. 둘 다 빌드/테스트 실패와 무관.

## 7. 상위 산출물 참조

- 각 유닛 테스트 목록·근거: U1~U6 `code-generation/code-summary.md`.
- 실행 명령·전제: 본 디렉터리 `build-instructions.md`, `unit-test-instructions.md`, `integration-test-instructions.md`.
