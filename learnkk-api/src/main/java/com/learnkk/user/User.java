package com.learnkk.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * User 도메인 엔티티 (domain-entities.md §2).
 *
 * <p>email 은 저장 전 소문자 정규화(R-U1-02), passwordHash 는 항상 BCrypt(INV-2). API 경계에서는 절대 노출하지 않으며 UserDto
 * 로만 전달한다(INV-1).
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(name = "password_hash", nullable = false, length = 60)
  private String passwordHash;

  @Column(name = "is_admin", nullable = false)
  private boolean admin;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected User() {
    // JPA 전용 기본 생성자
  }

  private User(String email, String name, String nickname, String passwordHash, boolean admin) {
    this.email = email;
    this.name = name;
    this.nickname = nickname;
    this.passwordHash = passwordHash;
    this.admin = admin;
  }

  /** 일반 가입 사용자 생성 — isAdmin 은 항상 false 로 강제한다(R-U1-06). */
  public static User newMember(String email, String name, String nickname, String passwordHash) {
    return new User(email, name, nickname, passwordHash, false);
  }

  /** 시드 관리자 생성 — isAdmin=true (부트스트랩 시드로만 사용, R-U1-16). */
  public static User newAdmin(String email, String name, String nickname, String passwordHash) {
    return new User(email, name, nickname, passwordHash, true);
  }

  void onPersistDefaults() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }

  @jakarta.persistence.PrePersist
  void prePersist() {
    onPersistDefaults();
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getNickname() {
    return nickname;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public boolean isAdmin() {
    return admin;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
