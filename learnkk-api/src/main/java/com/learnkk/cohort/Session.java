package com.learnkk.cohort;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회차 도메인 엔티티 (domain-entities.md §3, INV-U2-2).
 *
 * <p>개설 시 seq 1..sessionCount 로 자동 생성되며 초기 status=SCHEDULED 이다. 예정→인증 전이는 {@link #markVerified()}
 * 로만 수행하며(멱등), U4 가 {@code SessionService.markVerified} 를 통해 호출한다. cohortId 는 스칼라 FK 로 보유하고 별도의 양방향
 * 연관을 두지 않는다(상세 조회는 리포지토리 벌크 조회로 N+1 을 회피).
 */
@Entity
@Table(name = "session")
public class Session {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cohort_id", nullable = false)
  private Long cohortId;

  @Column(nullable = false)
  private int seq;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionStatus status;

  protected Session() {
    // JPA 전용 기본 생성자
  }

  private Session(Long cohortId, int seq) {
    this.cohortId = cohortId;
    this.seq = seq;
    this.status = SessionStatus.SCHEDULED;
  }

  /** 예정 상태 회차 생성(개설 시 seq 1..N). */
  public static Session scheduled(Long cohortId, int seq) {
    return new Session(cohortId, seq);
  }

  /**
   * 예정→인증 전이(U4 증빙 업로드 트리거). 이미 인증이면 멱등적으로 무시한다.
   *
   * @return 이 호출로 상태가 변경되었으면 true(예정→인증), 이미 인증이었으면 false
   */
  public boolean markVerified() {
    if (this.status == SessionStatus.VERIFIED) {
      return false;
    }
    this.status = SessionStatus.VERIFIED;
    return true;
  }

  public Long getId() {
    return id;
  }

  public Long getCohortId() {
    return cohortId;
  }

  public int getSeq() {
    return seq;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public boolean isVerified() {
    return this.status == SessionStatus.VERIFIED;
  }
}
