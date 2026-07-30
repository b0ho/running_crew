# Code Summary — U4 attendance (LearnKK 파일럿, Bolt 4)

> Construction · code-generation 산출 요약 · 유닛 U4-attendance (복잡도 M)
> 리드 aidlc-developer-agent
> 핵심: 증빙 업로드 → 회차 인증(예정→인증)의 원자성(INV-U4-1: 인증 회차 ↔ 증빙 최소 1건).

## 생성/수정 파일

### 백엔드 (learnkk-api) — 신규
- `resources/db/migration/V4__attendance_evidence.sql` — attendance_evidence 테이블(session FK CASCADE, uploaded_by FK RESTRICT, `(session_id, created_at)` 인덱스)
- `attendance/AttendanceEvidence.java` — 엔티티(private 생성자 + static `of` + @PrePersist, getter만)
- `attendance/AttendanceEvidenceRepository.java` — 이력/존재/벌크 조회
- `attendance/dto/EvidenceDto.java` — 증빙 메타(원경로 비노출)
- `attendance/dto/SessionAttendanceDto.java` — 회차별 상태 + 증빙 존재/최근 증빙 id
- `attendance/dto/CohortAttendanceDto.java` — 인증/전체 회차 + 진도율(0.0~1.0)
- `attendance/FileSignatureValidator.java` — 매직바이트(JPEG/PNG/PDF) + 선언 MIME 교차 검증
- `attendance/AttendanceEvidenceWriter.java` — **@Transactional 별도 빈**: 증빙 저장 + `SessionService.markVerified`(동일 TX)
- `attendance/AttendanceService.java` — **비 @Transactional 오케스트레이션**: 사전검증 → store → try{writer}catch{보상 delete}
- `attendance/EvidenceDownload.java` — 다운로드 스트리밍 핸들(Resource·mime·filename·size)
- `attendance/AttendanceController.java` — `POST /api/sessions/{id}/evidence`(multipart), `GET /api/cohorts/{id}/attendance`, `GET /api/sessions/{id}/evidence/{evidenceId}`(스트리밍)

### 백엔드 (learnkk-api) — 수정
- `resources/application.yml` — multipart `max-request-size=11MB`, `file-size-threshold=1MB`(performance-design §2)

### 백엔드 테스트 — 신규
- `attendance/FileSignatureValidatorTest.java` — 매직바이트 통과/위조 거부/크기·빈 파일 거부(7건)
- `attendance/AttendanceServiceTest.java` — 사전검증(404·403·409·400)·성공 위임·**보상 delete 호출**·진도율·참여자 인가(11건)
- `attendance/AttendanceIntegrationTest.java`(Testcontainers) — 업로드 원자성·재업로드 이력·진도율·다운로드·저장소 고아 없음·확정 멘티 인가(6건)
- `attendance/AttendanceCompensationIntegrationTest.java`(Testcontainers, @MockBean writer) — 롤백 시 파일 보상 삭제·회차 미인증·이력 0(INV-U4-1 일관)

### 프론트엔드 (learnkk-web) — 신규
- `api/attendanceApi.ts` — uploadEvidence(multipart)·getAttendance·evidenceDownloadUrl·loadEvidence(blob)
- `attendance/fileConstraints.ts` — 클라이언트 사전 검증(jpg/png/pdf ≤10MB)
- `attendance/FileDropzone.tsx` — 드래그앤드롭 + 키보드 접근 대체 input(접근성)
- `attendance/SessionEvidenceUpload.tsx` — 멘토 전용 업로드 + 에러 코드 매핑
- `attendance/ProgressSummary.tsx` — 진도율 바(색+수치, role=progressbar)
- `attendance/SessionAttendanceList.tsx`(+SessionRow) — 회차별 배지·다운로드·업로드
- `attendance/AttendancePanel.tsx` — 진도·출석 탭 패널(조회 + 업로드 후 즉시 재조회)
- 테스트: `api/attendanceApi.test.ts`, `attendance/{ProgressSummary,SessionAttendanceList,SessionEvidenceUpload}.test.tsx`

### 프론트엔드 (learnkk-web) — 수정
- `api/ApiClient.ts` — `postForm`(멀티파트, Content-Type 자동)·`getBlob`(바이너리)·`apiUrl` 추가
- `api/types.ts` — EvidenceDto·SessionAttendanceDto·CohortAttendanceDto 가산
- `cohorts/CohortDetailPage.tsx` — "진도·출석" 탭 플레이스홀더를 AttendancePanel 로 교체 + aria-live Toast
- `cohorts/CohortDetailPage.test.tsx` — attendanceApi 목킹 반영(진도 탭 통합 갱신)

## 핵심 결정

1. **원자성 writer 분리**: `AttendanceEvidenceWriter`(@Transactional 별도 빈)에서 증빙 저장 + `markVerified`(전파 REQUIRED)를 동일 트랜잭션으로 커밋/롤백 → INV-U4-1 구조적 보장. 오케스트레이션(`AttendanceService.uploadEvidence`)은 비트랜잭션이며 self-invocation 회피를 위해 빈을 분리(프록시 적용 보장).
2. **보상 delete**: 트랜잭션 롤백 시 `FileStorageService.delete`(멱등)로 고아 파일 보상. delete 실패 시 `ORPHAN_FILE_COMPENSATION_FAILED path=...` ERROR 로그 후 500(IllegalStateException → INTERNAL_ERROR). 잔여 리스크는 디스크 고아 파일뿐(R-U4-13, 파일럿 허용).
3. **매직바이트 검증**: `FileSignatureValidator`가 store **전에** JPEG(FF D8 FF)/PNG(89 50 4E 47)/PDF(25 50 44 46)를 확인하고 선언 MIME 과 교차 검증(위반 400). U1 store 계약은 불변(기본 검증만).
4. **스트리밍 다운로드**: `FileStorageService.load`의 `Resource`를 `ResponseEntity<Resource>`로 반환(전체 메모리 적재 없음). 파일명은 서버가 evidenceId+MIME 기준으로 생성(헤더 인젝션 방지), `Content-Disposition: attachment` + 정확한 Content-Type.
5. **참여자 인가**: U3 `EnrollmentRepository.findByCohortIdAndMenteeId` 읽기로 확정 멘티(CONFIRMED) 판정 → 소유 멘토·확정 멘티·관리자만 진도 조회/다운로드(R-U4-09/11). 회차 인증 전이는 U2 `markVerified`로만(INV-U4-4, 리포 직접 수정 없음).

## 테스트 결과

- 백엔드 단위 + spotless(과제 지정 명령): `./gradlew compileJava compileTestJava test -PexcludeIntegration spotlessCheck -q` → **BUILD SUCCESSFUL**(전부 통과).
- 백엔드 통합(Testcontainers, Docker 가용): `AttendanceIntegrationTest`·`AttendanceCompensationIntegrationTest` 격리 실행 시 **전부 통과**(원자성·보상·진도·다운로드·인가 검증).
- 프론트: `npm run build && npm test && npm run lint` → **build OK, 62 tests(17 suites) 통과, lint 0 warning**.

## 계획 대비 편차 / 이슈

- **U2/U3 통합 테스트의 선행 결함(회귀 아님)**: 전체 통합 스위트를 한 Gradle 실행으로 돌리면 `CohortIntegrationTest`·`EnrollmentIntegrationTest` 등에서 12건이 실패한다. 원인은 이들 테스트의 `setUp()`이 `userRepository`를 정리하지 않아(예: `mentor@learnkk.local` 중복 키) 지속형 Testcontainer 에서 2번째 이후 메서드가 유니크 제약 위반을 일으키는 **선행 테스트 격리 결함**이다. 해당 테스트는 U2/U3 소유(현재 미커밋 상태)이며 U4 변경(application.yml multipart·V4 마이그레이션)과 인과관계가 없다(사용자 insert 중복은 multipart 설정·증빙 스키마와 무관). U4 산출물은 격리 실행 시 전부 그린이므로 U4 회귀 아님. → U2/U3 소유자가 `setUp`에 `userRepository.deleteAll()` 추가 권장(본 유닛 범위 밖이라 미수정).
- 그 외 계획(Step 1~13) 대비 편차 없음. springdoc @Operation·모든 산출물 한글(team.md Mandated), Entity 미노출·DTO 경계(ArchUnit 자동 커버) 준수.

## Next Steps

- U5(completion): U4가 제공하는 회차 인증 상태(Session.status via U2)·증빙 존재를 read-only 로 소비해 수료 판정(출석≥80%) 수행(U4→U5 호출 없음).
- U6(admin-metrics): `AttendanceEvidenceRepository`의 `(session_id, created_at)` 이력 인덱스로 증빙 이력 뷰(US-15) 페이지네이션.
- 운영 확장 시: 고아 파일 주기적 GC 배치, 콘텐츠 심층 스캔(ClamAV), 오브젝트 스토리지+CDN 이관(U1 FileStorageService 백엔드 교체).
- (권장) U2/U3 통합 테스트 `setUp` 사용자 정리 보강으로 전체 통합 스위트 그린화.

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 적대적 검증 — 원자성·보상·매직바이트 핵심 경로

본 리뷰는 "결함 존재 가정 후 반증 시도" 방식으로 수행했다. U4의 핵심은 **INV-U4-1(인증 회차 ↔ 증빙 최소 1건)**을 구조적으로 보장하는 원자성·보상 설계다. 다음 경로를 집중 검증했다:

#### 1. 원자성(INV-U4-1) — AttendanceEvidenceWriter 분리 ✅

**검증 항목**: 증빙 저장 + `SessionService.markVerified`가 동일 트랜잭션인가? self-invocation으로 @Transactional 프록시가 무력화되지 않는가?

**코드 확인**:
- `AttendanceEvidenceWriter`(별도 빈, `@Transactional`): `persistAndVerify`가 증빙 이력 저장 + `sessionService.markVerified(sessionId)` 호출(REQUIRED 전파 → 동일 TX).
- `AttendanceService`(비@Transactional 오케스트레이션): 사전검증(회차/코호트 404·소유 멘토 403·CLOSED 409·매직바이트+크기 400) → `FileStorageService.store` → `try { evidenceWriter.persistAndVerify(...) } catch { compensate(storedPath); throw ... }`.
- **분리 근거**(javadoc 명시): 오케스트레이션이 비트랜잭션 파일 I/O + 보상 로직 포함 → 같은 클래스면 self-invocation으로 프록시 미적용. 별도 빈 분리로 `@Transactional` 프록시 보장.

**판정**: ✅ 원자성 구조 정확. 증빙 이력 + 회차 인증은 `evidenceWriter` 단일 트랜잭션으로 함께 커밋/롤백 → "인증됐는데 증빙 이력 없음"은 구조적으로 불가.

#### 2. 보상 경로(R-U4-13) — 고아 파일 삭제 ✅

**검증 항목**: 트랜잭션 롤백 시 `FileStorageService.delete` 호출 여부. delete 실패 시 로그 형식 및 500 반환. 보상 delete가 (실패한 TX와 무관하게) 실제로 실행되는 구조인가?

**코드 확인**:
- `AttendanceService.uploadEvidence`의 `try { evidenceWriter.persistAndVerify(...) } catch (RuntimeException txError) { compensate(storedPath); throw new IllegalStateException("증빙 저장에 실패했습니다", txError); }`.
- `compensate` 메서드: `fileStorageService.delete(storedPath)` 호출(멱등), delete 실패 시 `log.error("ORPHAN_FILE_COMPENSATION_FAILED path={}", storedPath, deleteError)` → 500.
- **비트랜잭션 오케스트레이션** 구조이므로 `compensate`는 TX 롤백 후 catch 블록에서 실제로 실행된다(TX 콜백 의존 없음).

**테스트 확인**: `AttendanceCompensationIntegrationTest`가 writer를 목킹(`@MockBean`)해 RuntimeException을 던지도록 설정 → `uploadEvidence` 호출 → 저장소 파일 수 0 확인(보상 삭제 성공), 회차 SCHEDULED·증빙 이력 0 확인(INV-U4-1 일관).

**판정**: ✅ 보상 경로 정확. 트랜잭션 롤백 시 고아 파일을 delete로 보상 삭제, 실패 시 `ORPHAN_FILE_COMPENSATION_FAILED` 로그 + 500. 잔여 리스크는 delete까지 실패한 디스크 고아 파일뿐(business-rules §3.5, 파일럿 허용).

#### 3. 매직바이트 검증(security-design §2) — store 전 실행 ✅

**검증 항목**: 매직바이트 검증이 store 호출 **전**에 수행되는가? 위조 파일(확장자만 pdf인 텍스트)을 거부하는가(400)?

**코드 확인**:
- `AttendanceService.uploadEvidence`의 사전검증 단계: `fileSignatureValidator.validate(file)` → `fileStorageService.store(file)` 순서.
- `FileSignatureValidator.validate`: JPEG(FF D8 FF)/PNG(89 50 4E 47)/PDF(25 50 44 46) 매직바이트 확인, 선언 MIME과 교차 검증(불일치 시 `FileConstraintViolationException` → 400).
- U1 `FileStorageService.store`는 기본 검증(확장자+선언 MIME+크기)만 수행 → U4가 매직바이트 강화 규칙 추가.

**테스트 확인**: `FileSignatureValidatorTest.확장자만_pdf인_텍스트파일은_매직바이트_불일치로_거부` — 텍스트 바이트 + `application/pdf` MIME → 예외. `AttendanceServiceTest.upload_매직바이트_위반이면_400_이고_저장하지_않는다` — validator 예외 시 store 미호출 확인.

**판정**: ✅ 매직바이트 검증이 store 전에 수행되며, 위조 파일(확장자만 pdf인 텍스트) 거부 확인.

#### 4. 권한 검증(R-U4-01/09/11) — 멘토 소유·참여자 인가 ✅

**검증 항목**: 업로드 소유 멘토 403, 진도/다운로드 참여자(확정 멘티 U3 조회)·관리자, 종료 코호트 409.

**코드 확인**:
- 업로드: `cohort.isOwnedBy(mentorId)` 아니면 `AccessDeniedException` → 403.
- 종료 코호트: `cohort.getStatus() == CohortStatus.CLOSED` 이면 `CohortClosedException` → 409.
- 진도 조회: `assertParticipantOrAdmin(cohort, requesterId, isAdmin)` — 관리자 또는 소유 멘토 또는 `isConfirmedMentee(cohortId, userId)`.
- `isConfirmedMentee`: `enrollmentRepository.findByCohortIdAndMenteeId(cohortId, userId)` 읽기(U3) → `status == EnrollmentStatus.CONFIRMED`.

**테스트 확인**: `AttendanceServiceTest.upload_비소유_멘토면_403`, `upload_종료된_코호트면_409`, `sessionsOf_확정_멘티는_조회할_수_있다` 모두 통과.

**판정**: ✅ 권한 검증 정확. 업로드 소유 멘토(403), 진도/다운로드 참여자(U3 확정 멘티)·관리자, 종료 코호트(409) 모두 구현됨.

#### 5. 회차 인증 전이(INV-U4-4) — U2 계약으로만 ✅

**검증 항목**: 회차 status 전이를 U2 `SessionService.markVerified`로만 수행하는가(리포 직접 수정 없음)?

**코드 확인**:
- `AttendanceEvidenceWriter.persistAndVerify`: `sessionService.markVerified(sessionId)` 호출(U2 계약).
- U4 코드에서 `SessionRepository.save(session)`을 직접 호출하지 않음(grep 확인).
- U2 `SessionService.markVerified`(learnkk-api/src/main/java/com/learnkk/cohort/SessionService.java, @Transactional): `session.markVerified()` → `sessionRepository.save(session)` (U2 캡슐화).

**판정**: ✅ 회차 인증 전이가 U2 계약으로만 수행됨(INV-U4-4 준수).

#### 6. Entity 미노출(INV-U4-1 유지, DTO 경계) ✅

**검증 항목**: AttendanceEvidence 엔티티가 API 경계에 노출되지 않는가?

**코드 확인**:
- `AttendanceController`: 모든 엔드포인트가 `EvidenceDto`/`CohortAttendanceDto`/`SessionAttendanceDto` 반환, `AttendanceEvidence` 타입 미노출.
- `EvidenceDto.from(AttendanceEvidence)` 정적 팩토리로 변환, `filePath` 원경로 비노출(다운로드는 evidenceId 경유).
- business-rules §1에 "Entity 미노출(DTO 경계)" 명시, ArchUnit 자동 검증(U1 상속).

**판정**: ✅ Entity 미노출 확인.

#### 7. 테스트가 always-pass 아님 — 실제 검증 ✅

**검증 항목**: 원자성·보상·매직바이트·진도율 테스트가 실제로 단언하는가(항상 통과하지 않음)?

**코드 확인**:
- `AttendanceCompensationIntegrationTest.트랜잭션_롤백시_저장된_파일을_보상_삭제하고_회차는_인증되지_않는다`: writer 목킹 예외 → 파일 수 0·회차 SCHEDULED·증빙 이력 0 **단언**.
- `AttendanceServiceTest.upload_트랜잭션_실패시_저장한_파일을_보상_삭제한다`: writer 예외 → `verify(fileStorageService).delete("orphan.jpg")` **단언**.
- `FileSignatureValidatorTest.확장자만_pdf인_텍스트파일은_매직바이트_불일치로_거부`: 텍스트 바이트 → **예외 단언**.
- `AttendanceServiceTest.sessionsOf_소유멘토는_진도율을_계산한다`: `progressRate() == 0.5` **단언**.

**판정**: ✅ 테스트가 실제로 원자성·보상·매직바이트·진도율을 검증함(always-pass 아님).

#### 8. Machine-checkable 빌드/테스트 결과 ✅

**실행 결과**:
- `./gradlew compileJava compileTestJava -q`: **BUILD SUCCESSFUL** (컴파일 통과).
- `./gradlew test --tests "com.learnkk.attendance.*"`: **BUILD SUCCESSFUL in 11s** (U4 단위+통합 테스트 전부 통과).
- `(cd learnkk-web && npm run build)`: **✓ built in 1.32s** (프론트 빌드 통과).
- `(cd learnkk-web && npm test)`: **Test Suites: 17 passed, Tests: 62 passed** (프론트 테스트 전부 통과).

**U2/U3 선행 격리 결함(U4 회귀 아님)**: 전체 통합 스위트 동시 실행 시 `CohortIntegrationTest`·`EnrollmentIntegrationTest` 등에서 `userRepository` 미정리로 유니크 제약 위반 12건 실패한다. 원인은 해당 테스트의 `setUp()`이 `userRepository.deleteAll()` 누락(예: `mentor@learnkk.local` 중복 키)이며, U4 변경(application.yml multipart·V4 마이그레이션)과 인과관계가 없다(사용자 insert 중복은 multipart 설정·증빙 스키마와 무관). U4 산출물은 격리 실행 시 전부 그린이므로 U4 회귀 아님 → U2/U3 소유자가 `setUp`에 `userRepository.deleteAll()` 추가 권장(본 유닛 범위 밖이라 미수정).

**판정**: ✅ U4 격리 테스트(단위+통합+프론트) 전부 통과. U2/U3 통합 실패는 선행 결함으로 U4 판정과 분리.

### 크로스유닛 계약 검증 ✅

**U4 → U1 (FileStorageService.store/load/delete)**:
- `store(MultipartFile)`: 확장자+선언 MIME+크기 기본 검증, 서버 UUID 파일명, 웹루트 밖 저장 → U1 제공 확인(`FileStorageService.java` 존재).
- `load(storedName)`: 경로 이탈 방지(canonical path), Resource 반환 → U1 제공 확인.
- `delete(storedName)`: 멱등(대상 없으면 no-op), 경로 이탈 방지 → U1 제공 확인(보상 목적).

**U4 → U2 (SessionService.markVerified)**:
- `markVerified(sessionId)`: Session.status 예정→인증 전이 → U2 제공 확인(`SessionService.java` 존재, @Transactional, REQUIRED 전파).

**U4 → U2 (읽기)**:
- `SessionRepository.findById`, `CohortRepository.findById` → U2 제공 확인.

**U4 → U3 (읽기)**:
- `EnrollmentRepository.findByCohortIdAndMenteeId` → U3 제공 확인(참여자 인가 판정 용).

**판정**: ✅ 크로스유닛 계약 모두 유효(실제 메서드 존재 확인).

### 구조적 정합성 검증 ✅

- **순환 의존성**: DAG U1→U2→(U3∥U4)→U5→U6 유지. U4는 U5/U6를 호출하지 않음(read-only 제공). ✅ 순환 없음.
- **DB 스키마**: `V4__attendance_evidence.sql` — `attendance_evidence(session_id FK CASCADE, uploaded_by FK RESTRICT, (session_id, created_at) 인덱스)` → domain-entities §2 정합 ✅.
- **Entity 필드**: `AttendanceEvidence.java` — sessionId·filePath·mimeType·size·uploadedBy·createdAt, private 생성자 + static `of` + @PrePersist, getter만 → domain-entities §2 정합 ✅.
- **다운로드 경로 이탈 방지·Content-Disposition 안전**: `FileStorageService.load`가 canonical path 검증, `AttendanceService.downloadEvidence`가 evidenceId 기준 서버 생성 파일명 반환 → security-design §2 정합 ✅.
- **스트리밍**: `FileStorageService.load`의 `Resource` → `EvidenceDownload` → 컨트롤러 `ResponseEntity<Resource>` → 전체 메모리 적재 없음 → performance-design §2 정합 ✅.
- **multipart 설정**: `application.yml` — `max-file-size=10MB`, `max-request-size=11MB`, `file-size-threshold=1MB` → performance-design §2 정합 ✅.

**판정**: ✅ 구조적 정합성 확보.

### Blast Radius 검증 ✅

- 파일 store 실패 → 해당 요청만(400/500) ✅.
- 트랜잭션 롤백 → 해당 요청만(보상 포함, 500) ✅.
- 보상 실패 → 고아 파일 1건(정합성 무관, 수동 정리 대상, R-U4-13) ✅.
- 파일 볼륨 장애 → 업로드/다운로드만(Important 티어), 나머지 기능 지속 ✅.

**판정**: ✅ Blast radius 적절히 격리됨.

### 구현 완결성 ✅

본 산출물(계획·요약·Q&A)과 상위 계약(functional-design·nfr-design·infrastructure-design)만으로 개발자가 원자성·보상·매직바이트·스트리밍·권한을 구현 가능함이 확인됨. 모든 실패 모드 → HTTP 상태 코드 매핑 완료, 크로스유닛 계약 명시, 잔여 리스크(바이러스 스캔 보류) 명시적 스코프아웃.

**판정**: ✅ 구현 완결성 충족.

### 최종 판정 근거

1. **INV-U4-1 원자성**: 증빙 이력 저장 + 회차 인증이 `AttendanceEvidenceWriter` 단일 트랜잭션으로 함께 커밋/롤백 → "인증됐는데 증빙 이력 없음"은 구조적으로 불가. self-invocation 회피를 위한 별도 빈 분리 정확. ✅
2. **보상 경로**: 트랜잭션 롤백 시 `FileStorageService.delete` 호출 → 고아 파일 보상 삭제. delete 실패 시 `ORPHAN_FILE_COMPENSATION_FAILED` 로그 + 500. 잔여 리스크는 디스크 고아 파일뿐(파일럿 허용). ✅
3. **매직바이트 검증**: store **전**에 JPEG/PNG/PDF 매직바이트 확인, 선언 MIME 교차 검증 → 위조 파일(확장자만 pdf인 텍스트) 거부(400). ✅
4. **권한**: 업로드 소유 멘토(403), 진도/다운로드 참여자(U3 확정 멘티)·관리자, 종료 코호트(409). ✅
5. **회차 인증 전이**: U2 `markVerified`로만(리포 직접 수정 없음, INV-U4-4). ✅
6. **Entity 미노출**: DTO 경계 확보. ✅
7. **테스트**: always-pass 아님, 실제 원자성·보상·매직바이트·진도율 검증. ✅
8. **Machine-checkable**: 백엔드 단위+통합·프론트 빌드+테스트 전부 통과. U2/U3 선행 격리 결함은 U4 판정과 분리. ✅
9. **크로스유닛 계약**: U1(store/load/delete)·U2(markVerified·읽기)·U3(읽기) 모두 유효. ✅
10. **구조적 정합성**: 순환 의존성 없음, DB 스키마·Entity·다운로드·스트리밍·multipart 설정 모두 상위 설계 정합. ✅

**개발자가 본 산출물만으로 원자성·보상·매직바이트·스트리밍·권한 경계를 구현 가능함**이 확인됨. 파일럿 보류 항목(바이러스 스캔·TLS·rate-limit)은 명시적으로 스코프아웃되어 있고 잔여 리스크 기록됨.

**READY.**
