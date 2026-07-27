# Services — LearnKK (파일럿)

> Inception · application-design 단계 산출물 · 백엔드 서비스 책임 정의
> 상위 입력: `components.md`, `requirements-analysis/requirements.md`

| 서비스 | 책임 | 관련 FR/US |
|---|---|---|
| AuthService | 회원가입(이메일·성명·닉네임), 로그인, BCrypt 해싱, 세션/토큰 | FR-1, US-1/2 |
| CohortService | 코호트 개설·수정·종료, 회차 N건 생성, 상태 전이(모집중→진행중→종료됨) | FR-2, US-3/4 |
| EnrollmentService | 선착순 자동 확정, 정원 마감 대기 등록, **동시성 제어(유니크 제약+비관적 락)**, 중복 방지 | FR-3, US-6a/6b |
| AdminApprovalService | 대기 신청 승인/거절, 상태 갱신 → NotificationService 호출 | FR-10, US-8 |
| AttendanceService | 회차 증빙 파일 업로드·검증(형식/크기), 출석 인증, 증빙 이력 | FR-5, US-9/10 |
| AnnouncementService | 코호트 공지 작성·조회(외부 링크) | FR-6, US-5 |
| ReportService | 최종 보고서 제출·조회 | FR-7, US-11 |
| CompletionService | 코호트 종료 시 수료 판정(출석≥80%)·수료증 발급, 정산 조건 판정(전 회차 인증+보고서) | FR-8/9, US-12/13 |
| MetricsService | 완주 코스 수·출석률·수료율·증서 발급 수 집계 | FR-11, US-14 |
| HistoryService | 증빙 이력·보고서 이력 조회(관리자) | FR-10, US-15 |
| NotificationService | 상태 변경 알림 생성·조회 | FR-4, US-7 |
| FileStorageService | 로컬 볼륨(웹루트 밖) 파일 저장·조회, 파일 제약(이미지/pdf ≤10MB) | US-9/11/12/15 |

## 트랜잭션·동시성 노트

- EnrollmentService.join: 트랜잭션 내 `SELECT ... FOR UPDATE`(비관적 락)로 정원 확인 후 확정, (cohortId, menteeId) 유니크 제약으로 중복·경쟁 방지. 정원 초과 시 대기 등록.
- CompletionService.endCohort: 코호트 종료 액션 트랜잭션에서 전 멘티 수료 판정 + 멘토 정산 판정 일괄 계산.

## 시드

- 최초 관리자: DB 시드 스크립트/마이그레이션으로 `isAdmin=true` 계정 부트스트랩(US-0).
