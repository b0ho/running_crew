# Components — LearnKK (파일럿)

> Inception · application-design 단계 산출물 · 리드 architect (aws-platform·design 관점)
> 상위 입력: `requirements-analysis/requirements.md`(FR-1~11), `user-stories/stories.md`(US-0~15), `practices-discovery/team-practices.md`
> 아키텍처: React(FE) + Spring Boot(BE) 분리 저장소, REST + springdoc-openapi, 로컬 Docker, RDB.

## 도메인 모델 (핵심 엔티티)

| 엔티티 | 설명 | 주요 필드 |
|---|---|---|
| User | 직원 계정 | id, email(unique), name, nickname, passwordHash, isAdmin |
| Cohort | 코호트(과정) | id, mentorId, title, description, capacity, startDate, endDate, sessionCount, status(모집중/진행중/종료됨) |
| Session | 회차 | id, cohortId, seq, status(예정/인증) |
| Enrollment | 참여/대기 | id, cohortId, menteeId, status(확정/대기중/거절), createdAt |
| AttendanceEvidence | 출석 증빙 | id, sessionId, menteeScope, filePath, mimeType, size, uploadedBy, createdAt |
| Announcement | 공지 | id, cohortId, body, externalLink, createdAt |
| FinalReport | 최종 보고서 | id, cohortId, authorId, body, filePath?, submittedAt |
| Certificate | 수료증 | id, cohortId, menteeId, imagePath, issuedAt |
| SettlementStatus | 정산 조건 상태 | id, cohortId, mentorId, satisfied(bool), evaluatedAt |
| Notification | 알림 | id, userId, type, message, read, createdAt |

> 주: 출석 인증은 회차 단위 증빙 첨부(멘토)로 성립(FR-5). Enrollment 유니크 제약: (cohortId, menteeId).

## 엔티티 관계 & 제약 (FK / ON DELETE)

| 관계 | 카디널리티 | 제약 |
|---|---|---|
| User (mentor) → Cohort | 1:N | Cohort.mentorId → User.id, NOT NULL, ON DELETE RESTRICT |
| Cohort → Session | 1:N | Session.cohortId → Cohort.id, NOT NULL, ON DELETE CASCADE |
| Cohort → Enrollment | 1:N | Enrollment.cohortId → Cohort.id, NOT NULL, ON DELETE CASCADE |
| User (mentee) → Enrollment | 1:N | Enrollment.menteeId → User.id, NOT NULL, ON DELETE RESTRICT |
| Session → AttendanceEvidence | 1:N | Evidence.sessionId → Session.id, NOT NULL, ON DELETE CASCADE |
| Cohort → Announcement | 1:N | Announcement.cohortId → Cohort.id, ON DELETE CASCADE |
| Cohort → FinalReport | 1:N | Report.cohortId → Cohort.id, ON DELETE CASCADE |
| Cohort → Certificate | 1:N | Certificate.cohortId → Cohort.id, ON DELETE CASCADE |
| Cohort → SettlementStatus | 1:1 | Settlement.cohortId → Cohort.id, ON DELETE CASCADE |
| User → Notification | 1:N | Notification.userId → User.id, ON DELETE CASCADE |
| Enrollment 유니크 | — | UNIQUE(cohortId, menteeId) — 중복/경쟁 방지 |

> 정책: 사용자(User)는 참여/멘토 이력이 있으면 삭제 제한(RESTRICT). 코호트 삭제 시 하위(회차·참여·증빙·공지·보고서·증서·정산)는 CASCADE. 파일럿에서 코호트 하드 삭제는 지양하고 "종료됨" 상태 전이를 우선.

## 백엔드 컴포넌트 (Spring, 레이어)

- Controller: Auth, Cohort, Session/Attendance, Enrollment, Announcement, Report, Completion, Admin, Metrics, Notification (REST, DTO 경계)
- Service: 위 도메인별 서비스 (services.md 참조)
- Repository: 각 엔티티 JPA 리포지토리
- 공통: `@RestControllerAdvice` 전역 예외 핸들러 + 공통 에러 DTO, Spring Security(BCrypt), FileStorage(로컬 볼륨)

## 프론트엔드 컴포넌트 (React, feature 폴더)

| 화면(mockups) | 컴포넌트 |
|---|---|
| S1 로그인/가입 | AuthPage, SignupForm, LoginForm |
| S2 대시보드 | DashboardPage, CohortCard, EmptyState |
| S3 탐색/참여 | ExplorePage, CohortCard, JoinButton, Toast |
| S3b 신청 상태 | MyApplicationsPage, StatusBadge |
| S4 코호트 상세 | CohortDetailPage, Tabs(공지/진도출석/멤버/보고서), SessionList, FileUpload, MemberList |
| S4b 보고서 | ReportForm |
| S5 개설/종료 | CohortForm(회차 수 포함), EndCohortDialog |
| S6 관리자 | AdminPage, Tabs(대기승인/지표/증빙이력/보고서이력) |
| S7/S8 수료·종료요약 | CompletionResult, CohortEndSummary |
| 공통 | TopTabBar/BottomTabBar, NotificationBell |

## 매핑 (요약)

- US-0 RBAC → Security + 시드 스크립트; US-1/2 → Auth; US-3/4 → Cohort/Session; US-6a/6b → Enrollment(동시성); US-9/10 → Attendance; US-5 → Announcement; US-11 → Report; US-12/13 → Completion; US-8/14/15 → Admin/Metrics; US-7 → Notification.
