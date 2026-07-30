package com.learnkk.completion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 정산 상태 도메인 엔티티 (domain-entities.md §4, US-13 / FR-9, INV-U5-2).
 *
 * <p>멘토 정산 조건(전 회차 인증 완료 AND 최종 보고서 제출) 충족 여부를 코호트당 1건(UNIQUE(cohortId)) 보관한다. 실제 정산/결제는 없으며 {@code
 * satisfied} 플래그와 메시지 수준으로만 표현한다(R-U5-13). 종료 재판정(upsert) 시 기존 1건을 {@link #updateSatisfied}로 갱신한다.
 * private 생성자 + static 팩토리로 생성한다.
 */
@Entity
@Table(name = "settlement_status")
public class SettlementStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cohort_id", nullable = false, unique = true)
  private Long cohortId;

  @Column(name = "mentor_id", nullable = false)
  private Long mentorId;

  @Column(nullable = false)
  private boolean satisfied;

  @Column(name = "evaluated_at", nullable = false)
  private Instant evaluatedAt;

  protected SettlementStatus() {
    // JPA 전용 기본 생성자
  }

  private SettlementStatus(Long cohortId, Long mentorId, boolean satisfied) {
    this.cohortId = cohortId;
    this.mentorId = mentorId;
    this.satisfied = satisfied;
    this.evaluatedAt = Instant.now();
  }

  /** 정산 판정 결과 최초 생성 (R-U5-11/12). */
  public static SettlementStatus of(Long cohortId, Long mentorId, boolean satisfied) {
    return new SettlementStatus(cohortId, mentorId, satisfied);
  }

  /** 재판정 upsert — 기존 1건 갱신(판정 시각도 갱신). */
  public void updateSatisfied(boolean satisfied) {
    this.satisfied = satisfied;
    this.evaluatedAt = Instant.now();
  }

  @PrePersist
  void prePersist() {
    if (this.evaluatedAt == null) {
      this.evaluatedAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getCohortId() {
    return cohortId;
  }

  public Long getMentorId() {
    return mentorId;
  }

  public boolean isSatisfied() {
    return satisfied;
  }

  public Instant getEvaluatedAt() {
    return evaluatedAt;
  }
}
