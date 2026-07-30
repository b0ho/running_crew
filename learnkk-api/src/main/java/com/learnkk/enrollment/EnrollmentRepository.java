package com.learnkk.enrollment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Enrollment 리포지토리 (performance-design.md §3, R-U3-07a/09). */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  /**
   * 확정(CONFIRMED) 인원 집계 — 반드시 join 의 비관적 락 보유 구간 안에서 호출한다(R-U3-09). {@code enrollment(cohort_id,
   * status)} 인덱스로 O(log n) 집계.
   */
  int countByCohortIdAndStatus(Long cohortId, EnrollmentStatus status);

  /** 사전 중복 조회(R-U3-04). 락 보유 상태에서 기존 신청 존재 여부 확인. */
  Optional<Enrollment> findByCohortIdAndMenteeId(Long cohortId, Long menteeId);

  /** 확정 멘티 목록(confirmedEnrollments — U5/U6 크로스유닛 계약, §7). */
  List<Enrollment> findByCohortIdAndStatus(Long cohortId, EnrollmentStatus status);

  /** 내 신청 목록(mentee 스코프, 최신순) (R-U3-16, myApplications). */
  Page<Enrollment> findByMenteeIdOrderByCreatedAtDesc(Long menteeId, Pageable pageable);

  /** 대기 목록(관리자 — 전체) (R-U3-11, listWaiting). */
  Page<Enrollment> findByStatusOrderByCreatedAtAsc(EnrollmentStatus status, Pageable pageable);

  /** 대기 목록(관리자 — 특정 코호트) (R-U3-11, listWaiting). */
  Page<Enrollment> findByCohortIdAndStatusOrderByCreatedAtAsc(
      Long cohortId, EnrollmentStatus status, Pageable pageable);

  /**
   * 상태 가드 조건부 UPDATE — 승인/거절(R-U3-12, cid:nfr-design:state-transition-guarded-update).
   *
   * <p>{@code WHERE id=:id AND status=:from} — 영향 행 0 이면 이미 다른 상태(경쟁/불허 전이)이므로 서비스가 409
   * INVALID_STATE_TRANSITION 으로 매핑한다. 두 관리자의 동시 승인 경합에서 DB 행 락이 UPDATE 를 직렬화하므로 정확히 1건만 성공하고 알림도
   * 1건만 생성된다(reliability-design.md §2). 벌크 UPDATE 후 영속성 컨텍스트를 flush/clear 하여 후속 재조회가 최신 상태를 읽게 한다.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE Enrollment e SET e.status = :to, e.decidedAt = CURRENT_TIMESTAMP"
          + " WHERE e.id = :id AND e.status = :from")
  int updateStatusGuarded(
      @Param("id") Long id, @Param("from") EnrollmentStatus from, @Param("to") EnrollmentStatus to);
}
