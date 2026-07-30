// 백엔드 계약 DTO 타입 (OpenAPI 스펙과 동기화 — learnkk-api/springdoc)

export interface UserDto {
  id: number;
  email: string;
  name: string;
  nickname: string;
  isAdmin: boolean;
}

export interface SignupRequest {
  email: string;
  name: string;
  nickname: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// 공통 에러 응답 DTO (R-U1-17)
export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  path: string;
}

// ---- U2 cohort DTO 타입 (learnkk-api com.learnkk.cohort.dto 와 동기화) ----

export type CohortStatus = 'RECRUITING' | 'ONGOING' | 'CLOSED';
export type SessionStatus = 'SCHEDULED' | 'VERIFIED';

export interface CohortSummaryDto {
  id: number;
  title: string;
  status: CohortStatus;
  statusLabel: string;
  capacity: number;
  startDate: string;
  endDate: string;
  sessionCount: number;
  mentorId: number;
}

export interface CohortDto {
  id: number;
  mentorId: number;
  title: string;
  description: string | null;
  capacity: number;
  startDate: string;
  endDate: string;
  sessionCount: number;
  status: CohortStatus;
  statusLabel: string;
  createdAt: string;
  warnings: string[];
}

export interface SessionDto {
  id: number;
  seq: number;
  status: SessionStatus;
  statusLabel: string;
}

export interface AnnouncementDto {
  id: number;
  cohortId: number;
  body: string;
  externalLink: string | null;
  createdAt: string;
}

export interface CohortDetailDto {
  id: number;
  mentorId: number;
  title: string;
  description: string | null;
  capacity: number;
  startDate: string;
  endDate: string;
  sessionCount: number;
  status: CohortStatus;
  statusLabel: string;
  createdAt: string;
  sessions: SessionDto[];
  recentAnnouncements: AnnouncementDto[];
}

export interface CohortCreateRequest {
  title: string;
  description?: string | null;
  capacity: number;
  startDate: string;
  endDate: string;
  sessionCount: number;
}

export type CohortUpdateRequest = CohortCreateRequest;

export interface AnnouncementCreateRequest {
  body: string;
  externalLink?: string | null;
}

/** Spring Data Page 응답 래퍼(사용 필드만 정의). */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ---- U3 enrollment / notification DTO 타입 (learnkk-api com.learnkk.enrollment.dto 와 동기화) ----

export type EnrollmentStatus = 'CONFIRMED' | 'WAITING' | 'REJECTED';

export type NotificationType =
  | 'ENROLLMENT_CONFIRMED'
  | 'ENROLLMENT_REJECTED'
  | 'COMPLETION_RESULT';

/** 선착순 참여 결과. 대기(WAITING)면 waitingPosition 이 대기 순번, 확정이면 null. */
export interface JoinResultDto {
  enrollmentId: number;
  cohortId: number;
  status: EnrollmentStatus;
  statusLabel: string;
  waitingPosition: number | null;
}

export interface EnrollmentDto {
  id: number;
  cohortId: number;
  cohortTitle: string | null;
  menteeId: number;
  status: EnrollmentStatus;
  statusLabel: string;
  createdAt: string;
  decidedAt: string | null;
}

export interface NotificationDto {
  id: number;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface WaitingEnrollmentDto {
  enrollmentId: number;
  cohortId: number;
  cohortTitle: string | null;
  menteeId: number;
  menteeName: string | null;
  menteeNickname: string | null;
  createdAt: string;
}

// ---- U4 attendance DTO 타입 (learnkk-api com.learnkk.attendance.dto 와 동기화) ----

/** 증빙 메타(원경로 비노출 — 다운로드는 id 경유). */
export interface EvidenceDto {
  id: number;
  sessionId: number;
  mimeType: string;
  size: number;
  uploadedBy: number;
  createdAt: string;
}

/** 회차별 출석 상태 + 증빙 존재/최근 증빙 id. */
export interface SessionAttendanceDto {
  sessionId: number;
  seq: number;
  status: SessionStatus;
  statusLabel: string;
  hasEvidence: boolean;
  latestEvidenceId: number | null;
}

/** 코호트 진도·출석 — 인증/전체 회차와 진도율(0.0~1.0). */
export interface CohortAttendanceDto {
  cohortId: number;
  verifiedCount: number;
  totalCount: number;
  progressRate: number;
  sessions: SessionAttendanceDto[];
}
