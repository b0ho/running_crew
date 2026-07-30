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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * 참여(Enrollment) 도메인 엔티티 (domain-entities.md §2, INV-U3-2/3).
 *
 * <p>private 생성자 + static 팩토리({@link #confirmed}/{@link #waiting})로 생성한다. 상태 전이(대기중→확정/거절)는 서비스
 * 레이어의 상태 가드 조건부 UPDATE 로 수행하므로(cid:nfr-design:state-transition-guarded-update) 엔티티에 상태 세터를 두지 않는다.
 * {@code version}(@Version)은 보조 방어선이다. API 경계에서는 절대 노출하지 않으며 DTO 로만 전달한다(INV-U3-4).
 */
@Entity
@Table(
    name = "enrollment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "ux_enrollment_cohort_mentee",
            columnNames = {"cohort_id", "mentee_id"}))
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cohort_id", nullable = false)
  private Long cohortId;

  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EnrollmentStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected Enrollment() {
    // JPA 전용 기본 생성자
  }

  private Enrollment(Long cohortId, Long menteeId, EnrollmentStatus status) {
    this.cohortId = cohortId;
    this.menteeId = menteeId;
    this.status = status;
  }

  /** 정원 여유가 있어 즉시 확정된 참여(W-U3-1, R-U3-02). */
  public static Enrollment confirmed(Long cohortId, Long menteeId) {
    return new Enrollment(cohortId, menteeId, EnrollmentStatus.CONFIRMED);
  }

  /** 정원 마감으로 대기 등록된 참여(W-U3-1, R-U3-02). */
  public static Enrollment waiting(Long cohortId, Long menteeId) {
    return new Enrollment(cohortId, menteeId, EnrollmentStatus.WAITING);
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

  public Long getMenteeId() {
    return menteeId;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public long getVersion() {
    return version;
  }

  /** 소유자(신청 멘티) 여부 — 본인 스코프 검증(R-U3-17/19)에 사용. */
  public boolean isOwnedBy(Long userId) {
    return this.menteeId != null && this.menteeId.equals(userId);
  }
}
