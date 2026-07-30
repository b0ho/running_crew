package com.learnkk.cohort.dto;

import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionStatus;

/**
 * 회차 응답 DTO (INV-U2-4).
 *
 * <p>statusLabel(예정/인증)을 함께 실어 FE 가 색+텍스트 배지를 색 매핑만으로 렌더할 수 있게 한다(접근성).
 */
public record SessionDto(Long id, int seq, SessionStatus status, String statusLabel) {

  public static SessionDto from(Session session) {
    return new SessionDto(
        session.getId(),
        session.getSeq(),
        session.getStatus(),
        session.getStatus().getDisplayName());
  }
}
