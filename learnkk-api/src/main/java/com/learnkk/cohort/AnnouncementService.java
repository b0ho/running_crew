package com.learnkk.cohort;

import com.learnkk.cohort.dto.AnnouncementCreateRequest;
import com.learnkk.cohort.dto.AnnouncementDto;
import com.learnkk.common.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지 서비스 (business-logic-model.md §6, R-U2-15~18).
 *
 * <p>작성은 소유 멘토만(R-U2-15). 조회 권한(R-U2-18 참여자·관리자)에서 참여 멘티 판정은 U3 데이터가 필요하므로, 파일럿에서는 코호트 존재 확인 후 인증
 * 사용자에게 목록을 제공하고 종료됨 코호트는 소유 멘토·관리자로 제한한다(U3 빌드 시 참여자 필터 강화 예정 — code-summary 편차 참조).
 */
@Service
public class AnnouncementService {

  private final AnnouncementRepository announcementRepository;
  private final CohortRepository cohortRepository;

  public AnnouncementService(
      AnnouncementRepository announcementRepository, CohortRepository cohortRepository) {
    this.announcementRepository = announcementRepository;
    this.cohortRepository = cohortRepository;
  }

  /** 공지 작성 (W-U2-6, R-U2-15/16/17). */
  @Transactional
  public AnnouncementDto create(Long mentorId, Long cohortId, AnnouncementCreateRequest req) {
    Cohort cohort = requireCohort(cohortId);
    if (!cohort.isOwnedBy(mentorId)) {
      throw new AccessDeniedException("코호트 소유 멘토만 공지를 작성할 수 있습니다");
    }
    Announcement announcement =
        Announcement.create(cohortId, req.body().trim(), normalizeLink(req.externalLink()));
    return AnnouncementDto.from(announcementRepository.save(announcement));
  }

  /** 공지 목록(최신순·페이지네이션) (R-U2-18, performance-design.md §3). */
  @Transactional(readOnly = true)
  public Page<AnnouncementDto> list(
      Long cohortId, Long requesterId, boolean isAdmin, Pageable pageable) {
    Cohort cohort = requireCohort(cohortId);
    if (cohort.getStatus() == CohortStatus.CLOSED && !isAdmin && !cohort.isOwnedBy(requesterId)) {
      throw new AccessDeniedException("종료된 코호트의 공지를 조회할 권한이 없습니다");
    }
    return announcementRepository
        .findByCohortIdOrderByCreatedAtDesc(cohortId, pageable)
        .map(AnnouncementDto::from);
  }

  private Cohort requireCohort(Long cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
  }

  private String normalizeLink(String externalLink) {
    if (externalLink == null) {
      return null;
    }
    String trimmed = externalLink.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
