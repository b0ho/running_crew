package com.learnkk.enrollment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification 리포지토리 (performance-design.md §3, R-U3-19). */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  /** 본인 알림 목록(user 스코프, 최신순) (R-U3-19, listFor). */
  Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /** 안읽은 알림 수(NotificationBell 배지). */
  long countByUserIdAndReadFalse(Long userId);

  /** 소유 확인용 단건 조회(markRead — 타인 알림 조작 차단, R-U3-19). */
  Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
