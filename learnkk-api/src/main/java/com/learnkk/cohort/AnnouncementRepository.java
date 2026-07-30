package com.learnkk.cohort;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Announcement 리포지토리 (performance-design.md §3). */
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

  /** 공지 목록(최신순, 페이지네이션 — 기본 20건). */
  Page<Announcement> findByCohortIdOrderByCreatedAtDesc(Long cohortId, Pageable pageable);

  /** 코호트 상세에 포함할 최근 공지 상한 5건(performance-design.md §3). */
  List<Announcement> findTop5ByCohortIdOrderByCreatedAtDesc(Long cohortId);
}
