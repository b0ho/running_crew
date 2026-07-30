package com.learnkk.metrics;

import com.learnkk.cohort.Cohort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * 운영 지표 집계 리포지토리 — 읽기 전용 (business-logic-model.md §2, R-U6-04~07).
 *
 * <p>U6 는 리포팅 읽기 모델로서 소스 유닛(U2 Cohort/Session, U3 Enrollment, U5 Certificate)의 스키마를 <b>읽기만</b> 하여
 * 집계한다(쓰기 없음, INV-U6-1). 이를 위해 쓰기 메서드가 노출되지 않는 좁은 {@link Repository} 베이스를 상속하고, 집계 쿼리만 선언한다. 집계 범위는
 * 종료됨(CLOSED) 코호트로 일관한다(INV-U6-4). 세션·참여는 JPA 연관 없이 스칼라 FK 만 보유하므로 서브쿼리로 종료됨 코호트 집합을 필터한다.
 */
public interface MetricsRepository extends Repository<Cohort, Long> {

  /** 완주(종료됨) 코호트 수 (R-U6-06). */
  @Query("SELECT COUNT(c) FROM Cohort c WHERE c.status = com.learnkk.cohort.CohortStatus.CLOSED")
  long countClosedCohorts();

  /**
   * 종료됨 코호트의 인증(VERIFIED) 회차 수 (출석률 분자, R-U6-04). {@code COUNT} 이므로 회차가 없으면 0 을 반환한다(INV-U6-3 분모 0
   * 안전 처리의 전제).
   */
  @Query(
      "SELECT COUNT(s) FROM Session s"
          + " WHERE s.status = com.learnkk.cohort.SessionStatus.VERIFIED"
          + " AND s.cohortId IN"
          + " (SELECT c.id FROM Cohort c WHERE c.status = com.learnkk.cohort.CohortStatus.CLOSED)")
  long countVerifiedSessionsOfClosed();

  /** 종료됨 코호트의 전체 회차 수 (출석률 분모, R-U6-04). 0 이면 서비스에서 출석률 0% 로 안전 처리(INV-U6-3). */
  @Query(
      "SELECT COUNT(s) FROM Session s"
          + " WHERE s.cohortId IN"
          + " (SELECT c.id FROM Cohort c WHERE c.status = com.learnkk.cohort.CohortStatus.CLOSED)")
  long countTotalSessionsOfClosed();

  /** 종료됨 코호트의 확정(CONFIRMED) 멘티 총수 (수료율 분모, R-U6-05). 0 이면 수료율 0% 로 안전 처리(INV-U6-3). */
  @Query(
      "SELECT COUNT(e) FROM Enrollment e"
          + " WHERE e.status = com.learnkk.enrollment.EnrollmentStatus.CONFIRMED"
          + " AND e.cohortId IN"
          + " (SELECT c.id FROM Cohort c WHERE c.status = com.learnkk.cohort.CohortStatus.CLOSED)")
  long countConfirmedMenteesOfClosed();

  /** 발급 증서 수 — 전체 증서 count (R-U6-07). 수료율 분자이자 certificateCount 지표. */
  @Query("SELECT COUNT(cert) FROM Certificate cert")
  long countCertificates();
}
