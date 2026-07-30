package com.learnkk.completion;

import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.completion.dto.CohortEndSummaryDto;
import com.learnkk.user.User;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 코호트 종료·수료증 API (frontend-components §3, security-design.md §1/§3).
 *
 * <p>종료 엔드포인트는 오케스트레이션 소유자인 U5 가 노출한다(U2 아님). 현재 사용자 id 는 요청 파라미터가 아니라 {@link
 * CurrentUserProvider}(세션 email→User)로 해석한다(신뢰 경계). 수료증 다운로드는 요청자 세션 id 로 스코프해 본인 수료증만 스트리밍한다(수평 권한
 * 상승 방지). 서비스가 던지는 도메인 예외는 U1 GlobalExceptionHandler 가 404/403/409/500 으로 매핑한다.
 */
@RestController
public class CompletionController {

  private final CompletionService completionService;
  private final CurrentUserProvider currentUserProvider;

  public CompletionController(
      CompletionService completionService, CurrentUserProvider currentUserProvider) {
    this.completionService = completionService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(
      summary = "코호트 종료",
      description =
          "소유 멘토가 진행중 코호트를 종료합니다. 단일 트랜잭션에서 수료 판정(출석률 80%)·수료증 발급·정산 판정·상태 전이·결과 알림을 원자적으로 수행하고,"
              + " 수료자 수·미수료 수·정산 충족 여부·발급 증서 수를 요약으로 반환합니다. 되돌릴 수 없습니다.")
  @PostMapping("/api/cohorts/{cohortId}/end")
  public ResponseEntity<CohortEndSummaryDto> endCohort(@PathVariable Long cohortId) {
    Long mentorId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(completionService.endCohort(mentorId, cohortId));
  }

  @Operation(
      summary = "수료증 다운로드",
      description = "본인의 수료증 이미지(PNG)를 스트리밍으로 다운로드합니다. 미수료·미발급이면 404 입니다.")
  @GetMapping("/api/cohorts/{cohortId}/certificate")
  public ResponseEntity<Resource> certificate(@PathVariable Long cohortId) {
    User current = currentUserProvider.currentUser();
    CertificateDownload download = completionService.certificateOf(cohortId, current.getId());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(download.mimeType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
        .body(download.resource());
  }
}
