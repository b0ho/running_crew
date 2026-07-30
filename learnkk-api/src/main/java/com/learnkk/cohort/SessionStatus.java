package com.learnkk.cohort;

/**
 * 회차 상태 (domain-entities.md §3).
 *
 * <p>예정(SCHEDULED) → 인증(VERIFIED). 인증 전이는 U4(attendance)가 증빙 업로드 시 {@code
 * SessionService.markVerified(sessionId)} 로 수행한다(리포지토리 직접 접근 금지 — 캡슐화).
 */
public enum SessionStatus {
  SCHEDULED("예정"),
  VERIFIED("인증");

  private final String displayName;

  SessionStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
