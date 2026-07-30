package com.learnkk.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 알림(Notification) 도메인 엔티티 (domain-entities.md §3).
 *
 * <p>수신자 userId 스코프로만 조회·읽음 처리된다(R-U3-19). message 에는 최소 정보만 담고 민감정보를 포함하지 않는다(security-design.md
 * §4). API 경계에서는 절대 노출하지 않으며 DTO 로만 전달한다(INV-U3-4).
 */
@Entity
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private NotificationType type;

  @Column(nullable = false, length = 500)
  private String message;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Notification() {
    // JPA 전용 기본 생성자
  }

  private Notification(Long userId, NotificationType type, String message) {
    this.userId = userId;
    this.type = type;
    this.message = message;
    this.read = false;
  }

  /** 알림 생성 팩토리(W-U3-7). */
  public static Notification of(Long userId, NotificationType type, String message) {
    return new Notification(userId, type, message);
  }

  /** 읽음 처리(R-U3-19) — 소유 확인은 서비스가 선행한다. */
  public void markRead() {
    this.read = true;
  }

  @PrePersist
  void prePersist() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public NotificationType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public boolean isRead() {
    return read;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** 소유자 여부 — markRead 소유 확인(R-U3-19)에 사용. */
  public boolean isOwnedBy(Long candidateUserId) {
    return this.userId != null && this.userId.equals(candidateUserId);
  }
}
