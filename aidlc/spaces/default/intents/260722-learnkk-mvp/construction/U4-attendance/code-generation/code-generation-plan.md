# Code Generation Plan — U4 attendance (LearnKK 파일럿, Bolt 4)

> Construction · code-generation 단계 계획 · 유닛 U4-attendance (복잡도 M)
> 리드 aidlc-developer-agent, 리뷰어 aidlc-architecture-reviewer-agent
> 상위 입력: functional-design(business-logic-model·business-rules·domain-entities·frontend-components), nfr-design(performance·security), infrastructure-design(deployment), units-generation/unit-of-work(U4), requirements(FR-5)
> 기반: U1(FileStorageService store/load/delete·공통 인프라)·U2(Session/SessionService.markVerified·CohortDetailPage 진도·출석 탭) 재사용.
> 핵심: 증빙 업로드→회차 인증(예정→인증)의 원자성(INV-U4-1: 인증 회차 ↔ 증빙 최소 1건).

## 크로스유닛 계약 (code-generation 시 반드시 반영)

| 방향 | 계약 | U4 구현 방식 |
|---|---|---|
| U4 → U1 (호출) | `FileStorageService.store/load/delete` | 이미 U1 제공(delete 멱등). store 전 U4가 매직바이트 검증 |
| U4 → U2 (호출) | `SessionService.markVerified(sessionId)` | 이미 U2 제공(멱등). 회차 인증 전이는 이 경로로만(INV-U4-4) |
| U4 → U2 (읽기) | Session·Cohort 조회(권한·진도) | U2 SessionRepository/CohortRepository 주입(읽기 전용) |
| U5 → U4 (읽기) | 회차 인증 상태·증빙 이력 | U4 제공(read) |
| U6 → U4 (읽기) | 증빙 이력·출석 집계 | U4 제공(read) |

## 원자성/보상 설계 (INV-U4-1 — self-invocation 회피)

- `AttendanceService.uploadEvidence`(**비 @Transactional 오케스트레이션**): 사전검증 → `FileStorageService.store` → `try { writer.persistAndVerify(...) } catch { FileStorageService.delete(storedPath); ... }`
- `AttendanceEvidenceWriter`(**@Transactional 별도 빈**): `persistAndVerify` = AttendanceEvidence 저장 + `SessionService.markVerified(sessionId)` (동일 트랜잭션, 함께 커밋/롤백)
- 롤백 시 store한 파일을 delete로 보상. delete 실패 시 `ORPHAN_FILE_COMPENSATION_FAILED` 토큰으로 ERROR 로그(수동 정리 대상) 후 500.

## 테스트 전략 (Comprehensive + team.md 정련)

핵심 도메인(업로드 원자성·보상·매직바이트 검증·권한·진도 계산) 80% 목표. 단위(Mockito), 통합(Testcontainers): 업로드→증빙 저장+회차 VERIFIED 원자성, 진도율, 다운로드, 롤백 시 보상 delete, multipart 임시파일 누수 없음. FE Jest/RTL.

---

## PART A — 백엔드 (learnkk-api)

### Step 1: DB 스키마 & Flyway 마이그레이션 (domain-entities §2)
- [x] `V4__attendance_evidence.sql`:
  - `attendance_evidence`(id BIGSERIAL PK, session_id BIGINT NOT NULL FK→session(id) ON DELETE CASCADE, file_path VARCHAR(512) NOT NULL, mime_type VARCHAR(100) NOT NULL, size BIGINT NOT NULL, uploaded_by BIGINT NOT NULL FK→users(id) ON DELETE RESTRICT, created_at TIMESTAMP NOT NULL DEFAULT now())
  - 인덱스(performance-design §3): `attendance_evidence(session_id, created_at)`(이력·증빙 존재 확인)
- 트레이스: FR-5, domain-entities §2, INV-U4-1

### Step 2: 엔티티 (domain-entities §2)
- [x] `com.learnkk.attendance.AttendanceEvidence`(sessionId·filePath·mimeType·size·uploadedBy·createdAt, private 생성자 + static 팩토리 `of(...)`, @PrePersist createdAt, getter만). Entity 미노출(INV-U4-1 유지, DTO 경계)
- 트레이스: domain-entities §2

### Step 3: 리포지토리
- [x] `AttendanceEvidenceRepository`: `findBySessionIdOrderByCreatedAtDesc(sessionId)`, `existsBySessionId(sessionId)`, `findBySessionIdIn(...)`(진도 조회 시 회차별 증빙 존재 집계), `countBySessionId(...)`
- 트레이스: performance-design §3

### Step 4: DTO (API 경계)
- [x] `EvidenceDto`(id·sessionId·mimeType·size·uploadedBy·createdAt — filePath 원경로 비노출, 다운로드는 evidenceId 경유), `SessionAttendanceDto`(seq·status·증빙 존재 여부·최근 증빙 id), `CohortAttendanceDto`(verifiedCount·totalCount·progressRate + `List<SessionAttendanceDto>`)
- [x] `from(entity)` 정적 팩토리
- 트레이스: R-U4-10, INV-U4-1

### Step 5: 파일 시그니처(매직바이트) 검증 (security-design §2)
- [x] `com.learnkk.attendance.FileSignatureValidator`(또는 common): MultipartFile의 앞부분 바이트로 실제 형식 확인 — JPEG(FF D8 FF), PNG(89 50 4E 47), PDF(25 50 44 46). 선언 MIME과 교차 확인. 불일치 시 `FileConstraintViolationException`(U1 400 재사용)
- 트레이스: security-design §2(이중 검증), R-U4-02, INV-U4-3

### Step 6: 서비스 레이어 (business-logic-model §2~4)
- [x] `AttendanceEvidenceWriter`(@Transactional 별도 빈): `persistAndVerify(sessionId, filePath, mime, size, uploadedBy): AttendanceEvidence` = 이력 저장 + `sessionService.markVerified(sessionId)`(동일 TX, 원자적)
- [x] `AttendanceService`(오케스트레이션, 비 @Transactional for upload):
  - `uploadEvidence(mentorId, sessionId, MultipartFile): EvidenceDto` — 사전검증(회차/코호트 404·소유 멘토 403·CLOSED 409·매직바이트+크기+MIME 400) → `FileStorageService.store` → try{writer.persistAndVerify}catch{FileStorageService.delete + delete 실패 시 ORPHAN 로그 + 500}
  - `sessionsOf(cohortId, requesterId, isAdmin): CohortAttendanceDto` — 권한(참여자·관리자) → 회차 status 집계 + 진도율(인증/전체) (R-U4-09/10)
  - `downloadEvidence(sessionId, evidenceId, requesterId, isAdmin): (Resource, mime, filename)` — 권한 확인 → `FileStorageService.load` (R-U4-11)
- [x] Session·Cohort 읽기: U2 SessionRepository/CohortRepository 주입(권한·소속 확인). 참여자 판정은 U3 데이터 필요 → 파일럿에서는 멘토/관리자 + 확정 멘티(U3 EnrollmentRepository read 또는 EnrollmentQuery 확장) 조회. **가능하면 U3 확정 멘티 조회로 참여자 인가, U3 미가용 경로는 멘토/관리자로 완화하고 javadoc 명시**
- 트레이스: business-logic-model §2~4, R-U4-01~11, INV-U4-1/4

### Step 7: 컨트롤러 (frontend-components §3, multipart)
- [x] `AttendanceController`:
  - `POST /api/sessions/{sessionId}/evidence` (multipart/form-data, `@RequestParam MultipartFile file`) → 201 EvidenceDto
  - `GET /api/cohorts/{cohortId}/attendance` → 200 CohortAttendanceDto
  - `GET /api/sessions/{sessionId}/evidence/{evidenceId}` → 200 스트리밍 다운로드(InputStreamResource/StreamingResponseBody, Content-Type 정확·Content-Disposition attachment 안전 인코딩)
- [x] 사용자 id는 CurrentUserProvider. springdoc @Operation(한글). multipart는 SecurityConfig authenticated로 보호됨
- 트레이스: frontend-components §3, security-design §2(다운로드 헤더)

### Step 8: 백엔드 테스트 (Comprehensive)
- [x] `FileSignatureValidatorTest`(단위): jpg/png/pdf 매직바이트 통과, 위조(확장자만 pdf인 텍스트) 거부, 크기 초과
- [x] `AttendanceServiceTest`(단위, Mockito): upload 사전검증(404·403·CLOSED 409·매직바이트 400), 성공 시 writer 호출·EvidenceDto, **롤백 시 FileStorageService.delete 보상 호출**(writer가 예외 던질 때), sessionsOf 진도율 계산·권한
- [x] `AttendanceEvidenceWriterTest` 또는 통합에서 원자성 검증
- [x] `AttendanceIntegrationTest`(Testcontainers, @Tag integration): 업로드 → attendance_evidence 1건 저장 + Session VERIFIED **원자적**, 재업로드 시 이력 누적·회차 인증 유지, 진도율, 다운로드 스트리밍, **트랜잭션 롤백 시 파일 보상 삭제**(markVerified가 던지도록 유도하거나 제약 위반), multipart 임시파일 누수 없음
- [x] ArchUnit DTO 경계는 기존 ArchitectureTest 자동 커버
- 트레이스: NFR-6, INV-U4-1, R-U4-13(보상)

### Step 9: OpenAPI 계약 동기화
- [x] springdoc 확인, FE types.ts와 DTO 필드명 일치

---

## PART B — 프론트엔드 (learnkk-web)

### Step 10: API 클라이언트(멀티파트) & 타입
- [x] `api/ApiClient.ts`에 멀티파트 업로드 지원(`postForm`/`upload` — Content-Type 자동, FormData 전송, 세션 쿠키), 바이너리 다운로드(blob) 지원
- [x] `api/types.ts` 추가: EvidenceDto, SessionAttendanceDto, CohortAttendanceDto
- [x] `api/attendanceApi.ts`: uploadEvidence(multipart), getAttendance, evidenceDownloadUrl/loadEvidence
- 트레이스: frontend-components §3

### Step 11: 컴포넌트 — U2 CohortDetailPage "진도·출석" 탭 채움 (frontend-components §2)
- [x] `attendance/ProgressSummary.tsx`(인증/전체 회차·진도율 바, 색+수치)
- [x] `attendance/SessionAttendanceList.tsx` + `SessionRow`(seq·status 배지·증빙 미리보기/다운로드 링크)
- [x] `attendance/SessionEvidenceUpload.tsx`(멘토 전용) + `FileDropzone`(jpg/png/pdf ≤10MB 클라이언트 사전 체크, 키보드 접근 대체 input) + 업로드 결과 Toast(재사용)
- [x] U2 `CohortDetailPage`의 "진도·출석" 탭 플레이스홀더를 위 컴포넌트로 교체(멘토일 때만 업로드 노출; 서버 권한 최종 방어)
- 트레이스: frontend-components §2, 접근성 §4

### Step 12: 프론트엔드 테스트 (Jest/RTL)
- [x] `attendance/SessionEvidenceUpload.test.tsx`(형식/크기 사전 검증·업로드 성공 시 인증 배지·409/400 에러 토스트)
- [x] `attendance/ProgressSummary.test.tsx`(진도율 렌더)
- [x] `attendance/SessionAttendanceList.test.tsx`(증빙 링크·상태 배지)
- [x] `api/attendanceApi.test.ts`(multipart 요청·다운로드)
- 트레이스: NFR-6

---

## Step 13: 코드 요약 산출
- [x] `code-summary.md`: 생성/수정 파일, 핵심 결정(원자성 writer 분리·보상 delete·매직바이트·스트리밍·참여자 인가), 테스트 결과, 계획 대비 편차

## 산출물(코드) 위치
- 백엔드: `learnkk-api/src/main/java/com/learnkk/attendance/**`, `resources/db/migration/V4__*.sql`, 테스트 `.../attendance/**`
- 프론트: `learnkk-web/src/attendance/**`, `src/api/{attendanceApi.ts,types.ts,ApiClient.ts}`, U2 `cohorts/CohortDetailPage.tsx` 진도·출석 탭 갱신
- 애플리케이션 코드는 워크스페이스 루트 하위에만. 레코드 디렉터리에는 계획·요약만.
