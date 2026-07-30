package com.learnkk.completion;

import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.completion.dto.ReportDto;
import com.learnkk.completion.dto.ReportSubmitRequest;
import com.learnkk.user.User;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 최종 보고서 API (frontend-components §2.2/§3, R-U5-15~19).
 *
 * <p>제출은 multipart/form-data(본문 필드 + 선택 첨부 파트)로 받는다. 현재 사용자 id 는 요청 파라미터가 아니라 {@link
 * CurrentUserProvider} 로 해석한다(신뢰 경계). 참여자·관리자 인가는 서비스 레이어에서 수행한다.
 */
@RestController
public class ReportController {

  private final ReportService reportService;
  private final CurrentUserProvider currentUserProvider;

  public ReportController(ReportService reportService, CurrentUserProvider currentUserProvider) {
    this.reportService = reportService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(
      summary = "최종 보고서 제출",
      description = "코호트 참여자(멘토·확정 멘티)가 최종 보고서를 제출합니다. 본문은 필수, 파일 첨부(jpg/png/pdf, ≤10MB)는 선택입니다.")
  @PostMapping(
      value = "/api/cohorts/{cohortId}/reports",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ReportDto> submit(
      @PathVariable Long cohortId,
      @RequestParam("body") String body,
      @RequestParam(value = "file", required = false) MultipartFile file) {
    Long userId = currentUserProvider.currentUserId();
    ReportDto created = reportService.submit(userId, cohortId, new ReportSubmitRequest(body), file);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(summary = "보고서 이력 조회", description = "코호트 참여자·관리자에게 제출된 최종 보고서 이력을 최신순으로 반환합니다.")
  @GetMapping("/api/cohorts/{cohortId}/reports")
  public ResponseEntity<Page<ReportDto>> history(
      @PathVariable Long cohortId, @PageableDefault(size = 20) Pageable pageable) {
    User current = currentUserProvider.currentUser();
    return ResponseEntity.ok(
        reportService.historyOf(cohortId, current.getId(), current.isAdmin(), pageable));
  }
}
