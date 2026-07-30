package com.learnkk.enrollment;

import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.enrollment.dto.WaitingEnrollmentDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대기 승인 API (frontend-components §2.4, security-design.md §1).
 *
 * <p>관리자 인가는 메서드 레벨이 아닌 클래스 레벨 {@code @PreAuthorize("hasRole('ADMIN')")}(R-U3-11, U1 R-U1-16a 상속)로
 * 강제한다. 관리자 id 는 {@link CurrentUserProvider}로 해석해 감사 로그에 사용한다.
 */
@RestController
@RequestMapping("/api/admin/enrollments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEnrollmentController {

  private final AdminApprovalService adminApprovalService;
  private final CurrentUserProvider currentUserProvider;

  public AdminEnrollmentController(
      AdminApprovalService adminApprovalService, CurrentUserProvider currentUserProvider) {
    this.adminApprovalService = adminApprovalService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(
      summary = "대기 목록 조회",
      description = "정원 마감으로 대기중인 참여 신청 목록을 신청 순으로 반환합니다. cohortId 로 코호트를 좁힐 수 있습니다.")
  @GetMapping("/waiting")
  public ResponseEntity<Page<WaitingEnrollmentDto>> listWaiting(
      @RequestParam(required = false) Long cohortId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(adminApprovalService.listWaiting(cohortId, pageable));
  }

  @Operation(
      summary = "대기 승인",
      description = "대기중 신청을 확정으로 전이하고 멘티에게 알림을 보냅니다. 정원을 초과할 수 있습니다(관리자 판단).")
  @PostMapping("/{id}/approve")
  public ResponseEntity<Void> approve(@PathVariable Long id) {
    Long adminId = currentUserProvider.currentUserId();
    adminApprovalService.approve(adminId, id);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "대기 거절", description = "대기중 신청을 거절로 전이하고 멘티에게 알림을 보냅니다.")
  @PostMapping("/{id}/reject")
  public ResponseEntity<Void> reject(@PathVariable Long id) {
    Long adminId = currentUserProvider.currentUserId();
    adminApprovalService.reject(adminId, id);
    return ResponseEntity.ok().build();
  }
}
