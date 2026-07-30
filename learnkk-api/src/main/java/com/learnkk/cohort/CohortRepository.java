package com.learnkk.cohort;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** Cohort 리포지토리 (performance-design.md §2/§3, R-U2-19). */
public interface CohortRepository extends JpaRepository<Cohort, Long> {

  /** 목록/탐색 — 상태 필터(모집중·진행중) + 페이지네이션(기본 20건, createdAt desc). */
  Page<Cohort> findByStatusIn(Collection<CohortStatus> statuses, Pageable pageable);

  /** 제목 키워드 필터 검색(대소문자 무시). */
  Page<Cohort> findByStatusInAndTitleContainingIgnoreCase(
      Collection<CohortStatus> statuses, String keyword, Pageable pageable);

  /** 대시보드 — 내가 멘토인 코호트(최신순). */
  List<Cohort> findByMentorIdOrderByCreatedAtDesc(Long mentorId);

  /**
   * 상태 가드 조건 UPDATE(cid:nfr-design:state-transition-guarded-update).
   *
   * <p>{@code WHERE id=:id AND status=:from} — 영향 행 0 이면 이미 다른 상태(경쟁/불허 전이)이므로 서비스가 409
   * INVALID_STATE_TRANSITION 으로 매핑한다. 벌크 UPDATE 후 영속성 컨텍스트를 flush/clear 하여 후속 재조회가 최신 상태를 읽게 한다.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE Cohort c SET c.status = :to WHERE c.id = :id AND c.status = :from")
  int updateStatusGuarded(
      @Param("id") Long id, @Param("from") CohortStatus from, @Param("to") CohortStatus to);

  /**
   * 비관적 쓰기 락으로 코호트 행을 조회한다(U3 선착순 참여 동시성 — R-U3-07).
   *
   * <p>{@code LockModeType.PESSIMISTIC_WRITE} = {@code SELECT ... FOR UPDATE}. 대상 코호트 행 1개만 잠가 동일
   * 코호트의 동시 join 을 직렬화한다(다른 코호트 join 은 완전 병렬). 락은 트랜잭션 커밋까지 보유된다. 무한 대기를 막기 위해 {@code
   * jakarta.persistence.lock.timeout = 3000ms} 힌트를 둔다(performance-design.md §2). 타임아웃 시 락 획득 실패 예외가
   * 발생하며 EnrollmentService 가 409 ENROLLMENT_BUSY 로 매핑한다.
   *
   * <p>Cohort 는 U2 소유(읽기 전용) 자산이며, U3 는 이 메서드로 락만 획득할 뿐 Cohort 를 수정하지 않는다(단방향 경계).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  @Query("SELECT c FROM Cohort c WHERE c.id = :id")
  Optional<Cohort> findByIdForUpdate(@Param("id") Long id);
}
