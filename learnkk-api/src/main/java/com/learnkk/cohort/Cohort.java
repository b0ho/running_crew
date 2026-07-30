package com.learnkk.cohort;

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
import java.time.LocalDate;

/**
 * 코호트 도메인 엔티티 (domain-entities.md §2, INV-U2-1/3).
 *
 * <p>private 생성자 + static 팩토리({@link #open}) 로 생성하며, 개설 직후 status=RECRUITING(R-U2-06)이다. 편집 가능한 필드는
 * {@link #edit} 도메인 메서드로만 변경한다(수정 워크플로 W-U2-2). 상태 전이는 서비스 레이어의 상태 가드 UPDATE 로 수행하므로 엔티티에 상태 세터를 두지
 * 않는다(cid:nfr-design:state-transition-guarded-update). API 경계에서는 절대 노출하지 않으며 DTO 로만 전달한다(INV-U2-4).
 */
@Entity
@Table(name = "cohort")
public class Cohort {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "mentor_id", nullable = false)
  private Long mentorId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private int capacity;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "session_count", nullable = false)
  private int sessionCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CohortStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Cohort() {
    // JPA 전용 기본 생성자
  }

  private Cohort(
      Long mentorId,
      String title,
      String description,
      int capacity,
      LocalDate startDate,
      LocalDate endDate,
      int sessionCount) {
    this.mentorId = mentorId;
    this.title = title;
    this.description = description;
    this.capacity = capacity;
    this.startDate = startDate;
    this.endDate = endDate;
    this.sessionCount = sessionCount;
    this.status = CohortStatus.RECRUITING;
  }

  /** 코호트 개설 — status=RECRUITING 으로 초기화(R-U2-06, W-U2-1). */
  public static Cohort open(
      Long mentorId,
      String title,
      String description,
      int capacity,
      LocalDate startDate,
      LocalDate endDate,
      int sessionCount) {
    return new Cohort(mentorId, title, description, capacity, startDate, endDate, sessionCount);
  }

  /**
   * 편집 가능한 코호트 정보를 갱신한다(W-U2-2). 상태(status)는 이 경로로 변경하지 않는다(상태 가드 UPDATE 사용). sessionCount 갱신에 따른
   * 회차 추가/삭제는 서비스가 별도로 조정한다.
   */
  public void edit(
      String title,
      String description,
      int capacity,
      LocalDate startDate,
      LocalDate endDate,
      int sessionCount) {
    this.title = title;
    this.description = description;
    this.capacity = capacity;
    this.startDate = startDate;
    this.endDate = endDate;
    this.sessionCount = sessionCount;
  }

  @PrePersist
  void prePersist() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
    if (this.status == null) {
      this.status = CohortStatus.RECRUITING;
    }
  }

  public Long getId() {
    return id;
  }

  public Long getMentorId() {
    return mentorId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getCapacity() {
    return capacity;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public int getSessionCount() {
    return sessionCount;
  }

  public CohortStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** 소유 멘토 여부 — 서비스 소유권 검증(R-U2-07/15)에 사용. */
  public boolean isOwnedBy(Long userId) {
    return this.mentorId != null && this.mentorId.equals(userId);
  }
}
