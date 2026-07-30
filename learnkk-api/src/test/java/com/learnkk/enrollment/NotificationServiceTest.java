package com.learnkk.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.enrollment.dto.NotificationDto;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** NotificationService 단위 테스트 — 알림 생성·본인 스코프·markRead 소유 확인(business-rules R-U3-18/19). */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @InjectMocks private NotificationService notificationService;

  private static final Long USER = 7L;
  private static final Long OTHER = 99L;
  private static final Long NOTIFICATION = 100L;

  @Test
  void notify_메시지가_비면_유형_기본메시지로_생성() {
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    NotificationDto dto =
        notificationService.notify(USER, NotificationType.ENROLLMENT_CONFIRMED, "");

    assertThat(dto.type()).isEqualTo(NotificationType.ENROLLMENT_CONFIRMED);
    assertThat(dto.message()).isEqualTo(NotificationType.ENROLLMENT_CONFIRMED.getDefaultMessage());
    assertThat(dto.read()).isFalse();
  }

  @Test
  void markRead_본인_소유_알림이면_읽음처리() {
    Notification n = Notification.of(USER, NotificationType.ENROLLMENT_REJECTED, "거절");
    when(notificationRepository.findByIdAndUserId(NOTIFICATION, USER)).thenReturn(Optional.of(n));

    notificationService.markRead(USER, NOTIFICATION);

    assertThat(n.isRead()).isTrue();
    verify(notificationRepository).save(n);
  }

  @Test
  void markRead_타인_알림이면_404_그리고_저장없음() {
    // 소유 스코프 조회(findByIdAndUserId)가 빈 결과 → 타인 알림 조작 차단(수평 권한 상승 방지).
    when(notificationRepository.findByIdAndUserId(NOTIFICATION, OTHER))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.markRead(OTHER, NOTIFICATION))
        .isInstanceOf(EntityNotFoundException.class);
    verify(notificationRepository, never()).save(any());
  }
}
