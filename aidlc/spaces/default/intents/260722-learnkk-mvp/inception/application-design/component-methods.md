# Component Methods — LearnKK (파일럿)

> Inception · application-design 단계 산출물 · 서비스 주요 메서드(시그니처 개략)
> 상위 입력: `services.md`, `components.md`

## AuthService
- `signup(email, name, nickname, password): UserDto` — 이메일 중복 검증, BCrypt 해싱
- `login(email, password): AuthToken`

## CohortService
- `create(mentorId, CohortCreateReq): CohortDto` — 회차 N건 생성
- `update(mentorId, cohortId, CohortUpdateReq): CohortDto` — 본인만, 정원 축소 경고
- `end(mentorId, cohortId): CohortEndSummaryDto` — 상태 종료됨 + CompletionService 트리거
- `list/search(filter): CohortDto[]`, `get(cohortId): CohortDetailDto`

## EnrollmentService
- `join(menteeId, cohortId): JoinResultDto` — 트랜잭션+비관적 락; 여유 시 확정, 마감 시 대기, 중복 거부(상태별)
- `myApplications(menteeId): EnrollmentDto[]`

## AdminApprovalService
- `listWaiting(cohortId?): EnrollmentDto[]`
- `approve(adminId, enrollmentId)` / `reject(adminId, enrollmentId)` — 상태 갱신 + 알림 생성

## AttendanceService
- `uploadEvidence(mentorId, sessionId, file): EvidenceDto` — 형식/크기 검증, 회차 인증, 이력 적재
- `sessionsOf(cohortId): SessionDto[]`

## AnnouncementService
- `create(mentorId, cohortId, body, externalLink)` / `list(cohortId)`

## ReportService
- `submit(userId, cohortId, body, file?): ReportDto` / `historyOf(cohortId)`

## CompletionService
- `evaluateOnEnd(cohortId)` — 멘티별 수료(출석 인증회차/전체≥0.8) + 멘토 정산(전 회차 인증 && 보고서 제출) 판정
- `certificateOf(menteeId, cohortId): CertificateDto`

## MetricsService
- `overview(): MetricsDto` — 완주 코스 수/출석률/수료율/증서 수(집계 정의는 US-14)

## NotificationService
- `notify(userId, type, message)` / `listFor(userId)` / `markRead`

## FileStorageService
- `store(file, constraints): storedPath` — 웹루트 밖 저장, MIME/크기 검증
- `load(path): Resource`

## 프론트 주요 훅/호출 (예)
- `useJoinCohort()`, `useUploadEvidence()`, `useEndCohort()`, `useMetrics()` — 중앙 API 클라이언트(에러 정규화) 경유
