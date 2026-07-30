package com.learnkk.enrollment;

/**
 * 알림 유형 (domain-entities.md §3).
 *
 * <p>승인/거절 알림(U3)과 확장 대비 수료 결과 알림(U5)을 포함한다. NotificationService.notify 는 이 유형을 받아 알림을 생성하며, 타
 * 유닛(U5/U8)이 U3 의 알림 API 를 호출할 때 사용한다(§5 크로스유닛 계약).
 */
public enum NotificationType {
  ENROLLMENT_CONFIRMED("참여가 확정되었습니다"),
  ENROLLMENT_REJECTED("참여가 거절되었습니다"),
  COMPLETION_RESULT("수료 결과가 확정되었습니다");

  private final String defaultMessage;

  NotificationType(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
