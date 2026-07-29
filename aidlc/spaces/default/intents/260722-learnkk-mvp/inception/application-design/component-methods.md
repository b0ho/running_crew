# Component Methods — LearnKK (파일럿)

> Inception · application-design 단계 산출물 · 서비스 주요 메서드(시그니처 개략)
> 상위 입력: `services.md`, `components.md`

## AuthService
- `signup(email, name, nickname, password): UserDto` — 이메일 중복 검증, BCrypt 해싱
- `login(email, password): AuthToken`

## CohortService
- `create(mentorId, CohortCreateReq): CohortDto` — 회차 N건 생성
- `update(mentorId, cohortId, CohortUpdateReq): CohortDto` — 본인만, 정원 축소 경고
- `start(mentorId, cohortId): CohortDto` — 모집중→진행중 전이(멘토 명시 시작 액션)
- `list/search(filter): CohortDto[]`, `get(cohortId): CohortDetailDto`
- `transitionToEnded(cohortId): void` — 진행중→종료됨 status 전이 세터(상태 가드 조건 UPDATE). **종료 오케스트레이션은 U5가 소유**하며(`cid:units-generation:c2`), U5가 종료 판정 후 이 세터를 호출한다. 사용자 대면 `end` 엔드포인트는 U2가 노출하지 않음(U2 §7 계약). (nfr-design U5 리뷰 조율로 레지스트리 반영 2026-07-28)

> 주(2026-07-28): 최초 `end(...)` 항목은 종료 오케스트레이션이 U2에 있다는 초기 표현이었으나, units-generation의 단일 소유(U5) 결정(`cid:units-generation:c2`)에 따라 U2는 `transitionToEnded` status 세터만 제공한다. 종료 요약 산출(`CohortEndSummaryDto`)과 오케스트레이션은 U5 `CompletionService.endCohort`가 소유.

## EnrollmentService
- `join(menteeId, cohortId): JoinResultDto` — 트랜잭션+비관적 락; 여유 시 확정, 마감 시 대기, 중복 거부(상태별)
- `myApplications(menteeId): EnrollmentDto[]`
- `confirmedCount(cohortId): int` — CONFIRMED 참여 수(read-only). U2 정원 축소 검증(R-U2-09)·U6 집계용. (U3 functional-design §3에서 확립, nfr-design 조율로 레지스트리 반영 2026-07-28)
- `confirmedEnrollments(cohortId): List<EnrollmentDto>` — 확정 멘티 목록(read-only). U5 수료증 발급 순회·U6 집계용. (U3 functional-design §7에서 확립, 2026-07-28)

## SessionService
- `markVerified(sessionId): void` — Session.status 예정→인증 전이(캡슐화; 리포지토리 직접 접근 대신 이 경로 사용). U4 증빙 업로드가 호출. (U2 functional-design §8에서 확립, nfr-design 조율로 레지스트리 반영 2026-07-28)

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
- `delete(path): void` — 저장 파일 삭제(경로 이탈 방지 후), 멱등(대상 없으면 no-op). 상위 유닛(U4 증빙 업로드, U5 보고서/증서)의 파일+DB 결합 트랜잭션 롤백 시 고아 파일 보상용. U1 functional-design §7에서 확립됨(nfr-design U4 리뷰 조율로 계약 레지스트리에 반영, 2026-07-28).

## 프론트 주요 훅/호출 (예)
- `useJoinCohort()`, `useUploadEvidence()`, `useEndCohort()`, `useMetrics()` — 중앙 API 클라이언트(에러 정규화) 경유
