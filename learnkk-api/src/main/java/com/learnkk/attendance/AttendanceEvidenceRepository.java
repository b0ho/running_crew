package com.learnkk.attendance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** AttendanceEvidence 리포지토리 (performance-design.md §3). */
public interface AttendanceEvidenceRepository extends JpaRepository<AttendanceEvidence, Long> {

  /** 회차 증빙 이력(최신순) — 이력 조회·미리보기(R-U4-06, U6 이력 뷰 대비). */
  List<AttendanceEvidence> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

  /** 회차 증빙 존재 여부 — 진도 조회 시 회차별 인증 판정 보조(INV-U4-1). */
  boolean existsBySessionId(Long sessionId);

  /** 여러 회차의 증빙을 한 번에 조회 — 진도 조회 N+1 회피(performance-design.md §3). */
  List<AttendanceEvidence> findBySessionIdInOrderByCreatedAtDesc(List<Long> sessionIds);

  /** 회차 증빙 건수. */
  long countBySessionId(Long sessionId);
}
