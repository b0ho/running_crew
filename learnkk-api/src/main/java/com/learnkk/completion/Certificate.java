package com.learnkk.completion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 수료증 도메인 엔티티 (domain-entities.md §3, US-12 / FR-8, INV-U5-1).
 *
 * <p>코호트 종료 판정 시 수료 기준(출석률 80%)을 충족한 확정 멘티에게 발급되는 단순 이미지 1장이다. {@code imagePath}는 U1 FileStorage 저장
 * 경로(NOT NULL — store 성공 후에만 insert, R-U5-08a). UNIQUE(cohortId, menteeId)로 코호트별 멘티당 1장만 허용해 재종료 시
 * 중복 발급을 방지한다. private 생성자 + static 팩토리로 생성하며 세터를 두지 않는다.
 */
@Entity
@Table(
    name = "certificate",
    uniqueConstraints =
        @UniqueConstraint(
            name = "ux_certificate_cohort_mentee",
            columnNames = {"cohort_id", "mentee_id"}))
public class Certificate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cohort_id", nullable = false)
  private Long cohortId;

  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Column(name = "image_path", nullable = false, length = 512)
  private String imagePath;

  @Column(name = "issued_at", nullable = false, updatable = false)
  private Instant issuedAt;

  protected Certificate() {
    // JPA 전용 기본 생성자
  }

  private Certificate(Long cohortId, Long menteeId, String imagePath) {
    this.cohortId = cohortId;
    this.menteeId = menteeId;
    this.imagePath = imagePath;
  }

  /** 수료증 발급 — imagePath 는 U1 store 성공 경로(NOT NULL) (R-U5-08). */
  public static Certificate issue(Long cohortId, Long menteeId, String imagePath) {
    return new Certificate(cohortId, menteeId, imagePath);
  }

  @PrePersist
  void prePersist() {
    if (this.issuedAt == null) {
      this.issuedAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getCohortId() {
    return cohortId;
  }

  public Long getMenteeId() {
    return menteeId;
  }

  public String getImagePath() {
    return imagePath;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }
}
