package com.learnkk.attendance.dto;

import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionStatus;

/**
 * 회차별 출석 상태 DTO (R-U4-10, frontend-components §2.2).
 *
 * <p>회차 순번·인증 상태(예정/인증)와 함께 증빙 존재 여부·최근 증빙 id 를 실어 FE 가 상태 배지와 다운로드 링크를 한 응답으로 렌더할 수 있게 한다.
 * statusLabel(예정/인증)을 함께 실어 색+텍스트 배지를 안정적으로 렌더한다(접근성 §4).
 */
public record SessionAttendanceDto(
    Long sessionId,
    int seq,
    SessionStatus status,
    String statusLabel,
    boolean hasEvidence,
    Long latestEvidenceId) {

  public static SessionAttendanceDto of(
      Session session, boolean hasEvidence, Long latestEvidenceId) {
    return new SessionAttendanceDto(
        session.getId(),
        session.getSeq(),
        session.getStatus(),
        session.getStatus().getDisplayName(),
        hasEvidence,
        latestEvidenceId);
  }
}
