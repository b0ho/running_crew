package com.learnkk.enrollment;

import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.enrollment.dto.EnrollmentDto;
import com.learnkk.enrollment.dto.JoinResultDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 참여 API (frontend-components §3, security-design.md §1).
 *
 * <p>SecurityConfig 에서 {@code /api/**} 는 authenticated 로 자동 보호된다. 현재 사용자 id 는 요청 바디가 아니라 {@link
 * CurrentUserProvider}(세션 email→User 조회)로 해석한다(신뢰 경계, 수평 권한 상승 방지).
 */
@RestController
public class EnrollmentController {

  private final EnrollmentService enrollmentService;
  private final CurrentUserProvider currentUserProvider;

  public EnrollmentController(
      EnrollmentService enrollmentService, CurrentUserProvider currentUserProvider) {
    this.enrollmentService = enrollmentService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(summary = "코호트 참여 신청", description = "선착순으로 참여를 신청합니다. 정원 여유가 있으면 확정, 마감이면 대기로 등록됩니다.")
  @PostMapping("/api/cohorts/{cohortId}/enrollments")
  public ResponseEntity<JoinResultDto> join(@PathVariable Long cohortId) {
    Long menteeId = currentUserProvider.currentUserId();
    JoinResultDto result = enrollmentService.join(menteeId, cohortId);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @Operation(summary = "내 신청 목록", description = "현재 사용자의 참여 신청 상태(대기중/확정/거절)를 최신순으로 반환합니다.")
  @GetMapping("/api/me/enrollments")
  public ResponseEntity<Page<EnrollmentDto>> myApplications(
      @PageableDefault(size = 20) Pageable pageable) {
    // 정렬은 리포지토리 메서드(OrderByCreatedAtDesc)가 강제한다.
    Long menteeId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(enrollmentService.myApplications(menteeId, pageable));
  }
}
