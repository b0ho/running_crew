package com.learnkk.cohort;

import com.learnkk.cohort.dto.AnnouncementCreateRequest;
import com.learnkk.cohort.dto.AnnouncementDto;
import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지 API (frontend-components §3, US-5/FR-6).
 *
 * <p>작성은 소유 멘토만(서비스 레이어 R-U2-15 검증). 목록은 최신순 페이지네이션(기본 20건).
 */
@RestController
@RequestMapping("/api/cohorts/{cohortId}/announcements")
public class AnnouncementController {

  private final AnnouncementService announcementService;
  private final CurrentUserProvider currentUserProvider;

  public AnnouncementController(
      AnnouncementService announcementService, CurrentUserProvider currentUserProvider) {
    this.announcementService = announcementService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(summary = "공지 작성", description = "소유 멘토가 코호트 공지를 작성합니다(외부 링크 선택).")
  @PostMapping
  public ResponseEntity<AnnouncementDto> create(
      @PathVariable Long cohortId, @Valid @RequestBody AnnouncementCreateRequest request) {
    Long mentorId = currentUserProvider.currentUserId();
    AnnouncementDto created = announcementService.create(mentorId, cohortId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(summary = "공지 목록", description = "코호트 공지를 최신순으로 페이지네이션 조회합니다.")
  @GetMapping
  public ResponseEntity<Page<AnnouncementDto>> list(
      @PathVariable Long cohortId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    User current = currentUserProvider.currentUser();
    return ResponseEntity.ok(
        announcementService.list(cohortId, current.getId(), current.isAdmin(), pageable));
  }
}
