package com.learnkk.attendance;

import com.learnkk.attendance.dto.CohortAttendanceDto;
import com.learnkk.attendance.dto.EvidenceDto;
import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.user.User;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
 * 출석·증빙 API (frontend-components §3, security-design.md §1/§2).
 *
 * <p>SecurityConfig 에서 {@code /api/**} 는 authenticated 로 자동 보호되며, 현재 사용자 id 는 요청 파라미터가 아니라 {@link
 * CurrentUserProvider}(세션 email→User 조회)로 해석한다(신뢰 경계). 업로드는 multipart/form-data, 다운로드는 정확한
 * Content-Type + {@code Content-Disposition: attachment}(서버 생성 안전 파일명)로 스트리밍한다.
 */
@RestController
public class AttendanceController {

  private final AttendanceService attendanceService;
  private final CurrentUserProvider currentUserProvider;

  public AttendanceController(
      AttendanceService attendanceService, CurrentUserProvider currentUserProvider) {
    this.attendanceService = attendanceService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(
      summary = "회차 증빙 업로드",
      description =
          "소유 멘토가 회차 증빙(jpg/png/pdf, ≤10MB)을 업로드하면 해당 회차가 즉시 인증됩니다. 증빙 저장과 회차 인증은 원자적입니다.")
  @PostMapping(
      value = "/api/sessions/{sessionId}/evidence",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<EvidenceDto> upload(
      @PathVariable Long sessionId, @RequestParam("file") MultipartFile file) {
    Long mentorId = currentUserProvider.currentUserId();
    EvidenceDto created = attendanceService.uploadEvidence(mentorId, sessionId, file);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(
      summary = "코호트 진도·출석 조회",
      description = "참여자(멘토·확정 멘티)·관리자에게 회차별 인증 상태와 진도율(인증/전체 회차)을 반환합니다.")
  @GetMapping("/api/cohorts/{cohortId}/attendance")
  public ResponseEntity<CohortAttendanceDto> attendance(@PathVariable Long cohortId) {
    User current = currentUserProvider.currentUser();
    return ResponseEntity.ok(
        attendanceService.sessionsOf(cohortId, current.getId(), current.isAdmin()));
  }

  @Operation(
      summary = "증빙 다운로드",
      description = "참여자·관리자가 회차 증빙 파일을 스트리밍으로 다운로드합니다. 파일명은 서버가 안전하게 생성합니다.")
  @GetMapping("/api/sessions/{sessionId}/evidence/{evidenceId}")
  public ResponseEntity<Resource> download(
      @PathVariable Long sessionId, @PathVariable Long evidenceId) {
    User current = currentUserProvider.currentUser();
    EvidenceDownload download =
        attendanceService.downloadEvidence(
            sessionId, evidenceId, current.getId(), current.isAdmin());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(download.mimeType()))
        .contentLength(download.size())
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
        .body(download.resource());
  }
}
