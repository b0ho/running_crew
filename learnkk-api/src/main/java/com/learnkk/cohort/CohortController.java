package com.learnkk.cohort;

import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDetailDto;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.cohort.dto.CohortSummaryDto;
import com.learnkk.cohort.dto.CohortUpdateRequest;
import com.learnkk.common.security.CurrentUserProvider;
import com.learnkk.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 코호트 API (frontend-components §3, security-design.md §1).
 *
 * <p>SecurityConfig 에서 {@code /api/cohorts/**} 는 authenticated 로 자동 보호된다. 현재 사용자 id 는 요청 바디가 아니라
 * {@link CurrentUserProvider}(세션 email→User 조회)로 해석한다(신뢰 경계).
 */
@RestController
@RequestMapping("/api/cohorts")
public class CohortController {

  private final CohortService cohortService;
  private final CurrentUserProvider currentUserProvider;

  public CohortController(CohortService cohortService, CurrentUserProvider currentUserProvider) {
    this.cohortService = cohortService;
    this.currentUserProvider = currentUserProvider;
  }

  @Operation(summary = "코호트 개설", description = "멘토가 코호트를 개설하고 회차 N건을 자동 생성합니다.")
  @PostMapping
  public ResponseEntity<CohortDto> create(@Valid @RequestBody CohortCreateRequest request) {
    Long mentorId = currentUserProvider.currentUserId();
    CohortDto created = cohortService.create(mentorId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(summary = "코호트 수정", description = "소유 멘토가 코호트 정보·정원·회차 수를 수정합니다.")
  @PutMapping("/{id}")
  public ResponseEntity<CohortDto> update(
      @PathVariable Long id, @Valid @RequestBody CohortUpdateRequest request) {
    Long mentorId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(cohortService.update(mentorId, id, request));
  }

  @Operation(summary = "코호트 시작", description = "소유 멘토가 코호트를 모집중에서 진행중으로 전이합니다.")
  @PostMapping("/{id}/start")
  public ResponseEntity<CohortDto> start(@PathVariable Long id) {
    Long mentorId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(cohortService.start(mentorId, id));
  }

  @Operation(summary = "코호트 목록·탐색", description = "인증 사용자에게 모집중·진행중 코호트를 페이지네이션으로 제공합니다.")
  @GetMapping
  public ResponseEntity<Page<CohortSummaryDto>> list(
      @RequestParam(required = false) CohortStatus status,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(cohortService.list(status, keyword, pageable));
  }

  @Operation(summary = "내 코호트 목록", description = "현재 사용자가 멘토인 코호트를 최신순으로 반환합니다(대시보드).")
  @GetMapping("/mine")
  public ResponseEntity<List<CohortSummaryDto>> mine() {
    Long mentorId = currentUserProvider.currentUserId();
    return ResponseEntity.ok(cohortService.getMine(mentorId));
  }

  @Operation(summary = "코호트 상세", description = "기본정보 + 회차 목록 + 최근 공지(상한 5건)를 반환합니다.")
  @GetMapping("/{id}")
  public ResponseEntity<CohortDetailDto> get(@PathVariable Long id) {
    User current = currentUserProvider.currentUser();
    return ResponseEntity.ok(cohortService.get(id, current.getId(), current.isAdmin()));
  }
}
