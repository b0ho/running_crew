package com.learnkk.enrollment;

import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.enrollment.dto.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API (frontend-components §2.3, security-design.md §1).
 *
 * <p>모든 조회·읽음 처리는 요청자 세션 id 로 스코프한다(본인만, R-U3-19). 클라이언트가 보낸 id 를 신뢰하지 않는다.
 */
@RestController
@RequestMapping("/api/me/notifications")
public class NotificationController {

  private final NotificationService notificationService;
  private final CurrentUserProvider currentUserProvider;

  public NotificationController(
      NotificationService notificationService, CurrentUserProvider currentUserProvider) {
    this.notificationService = notificationService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(summary = "내 알림 목록", description = "현재 사용자의 알림을 최신순으로 반환합니다.")
  @GetMapping
  public ResponseEntity<Page<NotificationDto>> list(@PageableDefault(size = 20) Pageable pageable) {
    Long userId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(notificationService.listFor(userId, pageable));
  }

  @Operation(summary = "안읽은 알림 수", description = "현재 사용자의 안읽은 알림 개수를 반환합니다(알림 배지).")
  @GetMapping("/unread-count")
  public ResponseEntity<Map<String, Long>> unreadCount() {
    Long userId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(userId)));
  }

  @Operation(summary = "알림 읽음 처리", description = "지정한 알림을 읽음으로 표시합니다. 본인 소유 알림만 처리됩니다.")
  @PostMapping("/{id}/read")
  public ResponseEntity<Void> markRead(@PathVariable Long id) {
    Long userId = currentUserProvider.currentUserId();
    notificationService.markRead(userId, id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
