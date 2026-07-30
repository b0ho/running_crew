package com.learnkk.enrollment.dto;

import com.learnkk.enrollment.Notification;
import com.learnkk.enrollment.NotificationType;
import java.time.Instant;

/** 알림 응답 DTO (INV-U3-4, R-U3-19). Entity 를 직접 노출하지 않는다. */
public record NotificationDto(
    Long id, NotificationType type, String message, boolean read, Instant createdAt) {

  public static NotificationDto from(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getType(),
        notification.getMessage(),
        notification.isRead(),
        notification.getCreatedAt());
  }
}
