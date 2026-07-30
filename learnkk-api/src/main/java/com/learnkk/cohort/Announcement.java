package com.learnkk.cohort;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 공지 도메인 엔티티 (domain-entities.md §4, US-5/FR-6).
 *
 * <p>externalLink 는 멘토가 붙이는 외부 미팅 URL(선택)이며 플랫폼이 서버에서 대신 fetch 하지 않는다. 스킴 화이트리스트 검증은 DTO 경계의
 * {@code @SafeExternalUrl} 이 담당한다(security-design.md §3).
 */
@Entity
@Table(name = "announcement")
public class Announcement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cohort_id", nullable = false)
  private Long cohortId;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "external_link", length = 2048)
  private String externalLink;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Announcement() {
    // JPA 전용 기본 생성자
  }

  private Announcement(Long cohortId, String body, String externalLink) {
    this.cohortId = cohortId;
    this.body = body;
    this.externalLink = externalLink;
  }

  /** 공지 작성(W-U2-6). */
  public static Announcement create(Long cohortId, String body, String externalLink) {
    return new Announcement(cohortId, body, externalLink);
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

  public Long getCohortId() {
    return cohortId;
  }

  public String getBody() {
    return body;
  }

  public String getExternalLink() {
    return externalLink;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
