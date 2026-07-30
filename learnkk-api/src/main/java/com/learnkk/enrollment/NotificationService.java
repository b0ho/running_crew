package com.learnkk.enrollment;

import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.enrollment.dto.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 도메인 서비스 (business-logic-model.md §6, R-U3-18/19).
 *
 * <p>{@link #notify}는 타 유닛(U5 수료 통지·U3 내부 승인/거절)이 호출하는 알림 생성 계약이다(§7 크로스유닛). 조회·읽음 처리는 반드시 요청자 세션
 * id 로 스코프하며(파라미터로 받은 id 를 신뢰하지 않음), markRead 는 대상 알림의 소유자 확인 후에만 처리한다(수평 권한 상승 방지,
 * security-design.md §1).
 */
@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;

  public NotificationService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  /**
   * 알림 생성 (R-U3-18, §7 크로스유닛 계약). 상태 변경(확정/거절)·수료 통지 등에서 호출한다. 호출자의 트랜잭션에 참여한다(예: 승인+알림 동일 트랜잭션,
   * reliability-design.md §2).
   */
  @Transactional
  public NotificationDto notify(Long userId, NotificationType type, String message) {
    String resolved = (message == null || message.isBlank()) ? type.getDefaultMessage() : message;
    Notification saved = notificationRepository.save(Notification.of(userId, type, resolved));
    return NotificationDto.from(saved);
  }

  /** 본인 알림 목록(최신순, 페이지네이션) (R-U3-19). userId 는 세션에서 해석된 값이어야 한다. */
  @Transactional(readOnly = true)
  public Page<NotificationDto> listFor(Long userId, Pageable pageable) {
    return notificationRepository
        .findByUserIdOrderByCreatedAtDesc(userId, pageable)
        .map(NotificationDto::from);
  }

  /** 안읽은 알림 수(NotificationBell 배지). */
  @Transactional(readOnly = true)
  public long unreadCount(Long userId) {
    return notificationRepository.countByUserIdAndReadFalse(userId);
  }

  /**
   * 읽음 처리 (R-U3-19). 대상 알림이 요청자 소유가 아니면 404(존재 노출 최소화 — 타인 알림 열람/조작 차단). 소유 확인을 {@code
   * findByIdAndUserId}로 원자적으로 수행한다.
   */
  @Transactional
  public void markRead(Long userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다"));
    notification.markRead();
    notificationRepository.save(notification);
  }
}
