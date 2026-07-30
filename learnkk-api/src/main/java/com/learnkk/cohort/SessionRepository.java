package com.learnkk.cohort;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Session 리포지토리 (domain-entities.md §3). */
public interface SessionRepository extends JpaRepository<Session, Long> {

  /** 회차 목록(seq 오름차순) — 코호트 상세·SessionService.listByCohort. */
  List<Session> findByCohortIdOrderBySeqAsc(Long cohortId);

  /** 특정 상태의 회차 조회(예: 인증 회차). */
  List<Session> findByCohortIdAndStatus(Long cohortId, SessionStatus status);

  /** sessionCount 축소 시 잘려나갈 구간(seq > newCount)에 인증 회차가 있는지 검사(R-U2-10). */
  long countByCohortIdAndSeqGreaterThanAndStatus(Long cohortId, int seq, SessionStatus status);

  /**
   * sessionCount 축소 시 잘려나갈 구간(seq > newCount)의 예정 회차 삭제. 파생 delete 는 조회 후 삭제로 동작해 영속성 컨텍스트를 비우지
   * 않으므로, 동일 트랜잭션에서 로딩한 Cohort 가 detach 되지 않는다.
   */
  void deleteByCohortIdAndSeqGreaterThan(Long cohortId, int seq);
}
