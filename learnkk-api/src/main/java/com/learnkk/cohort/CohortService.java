package com.learnkk.cohort;

import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDetailDto;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.cohort.dto.CohortSummaryDto;
import com.learnkk.cohort.dto.CohortUpdateRequest;
import com.learnkk.cohort.port.ConfirmedEnrollmentQuery;
import com.learnkk.common.exception.CapacityBelowConfirmedException;
import com.learnkk.common.exception.CohortClosedException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.common.exception.SessionVerifiedLockException;
import com.learnkk.common.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코호트 도메인 서비스 (business-logic-model.md §2~5, business-rules R-U2-01~20).
 *
 * <p>소유권 검증(R-U2-07)·상태 전이(상태 가드 UPDATE)·정원 축소 검증(U3 포트 조회)·회차 조정을 담당한다. 종료 판정·오케스트레이션은 U5 소유이며, U2
 * 는 {@link #closeByCompletion} 상태 전이 경로만 제공한다(§7 경계, cid:units-generation:c2).
 */
@Service
public class CohortService {

  /** 목록/탐색에서 인증 사용자에게 공개하는 상태(R-U2-19). 종료됨은 상세에서 소유자·관리자만. */
  private static final Set<CohortStatus> PUBLIC_STATUSES =
      Set.of(CohortStatus.RECRUITING, CohortStatus.ONGOING);

  private final CohortRepository cohortRepository;
  private final SessionRepository sessionRepository;
  private final AnnouncementRepository announcementRepository;
  private final ConfirmedEnrollmentQuery confirmedEnrollmentQuery;

  public CohortService(
      CohortRepository cohortRepository,
      SessionRepository sessionRepository,
      AnnouncementRepository announcementRepository,
      ConfirmedEnrollmentQuery confirmedEnrollmentQuery) {
    this.cohortRepository = cohortRepository;
    this.sessionRepository = sessionRepository;
    this.announcementRepository = announcementRepository;
    this.confirmedEnrollmentQuery = confirmedEnrollmentQuery;
  }

  /** 코호트 개설 + 회차 N건 생성 (W-U2-1, R-U2-01~06). */
  @Transactional
  public CohortDto create(Long mentorId, CohortCreateRequest req) {
    validateDates(req.startDate(), req.endDate());

    Cohort cohort =
        Cohort.open(
            mentorId,
            req.title().trim(),
            normalizeDescription(req.description()),
            req.capacity(),
            req.startDate(),
            req.endDate(),
            req.sessionCount());
    Cohort saved = cohortRepository.save(cohort);

    // 트랜잭션 내 회차 seq 1..sessionCount 일괄 생성(R-U2-03).
    List<Session> sessions = new ArrayList<>(req.sessionCount());
    for (int seq = 1; seq <= req.sessionCount(); seq++) {
      sessions.add(Session.scheduled(saved.getId(), seq));
    }
    sessionRepository.saveAll(sessions);

    return CohortDto.from(saved);
  }

  /** 코호트 수정 (W-U2-2, R-U2-07~10). */
  @Transactional
  public CohortDto update(Long mentorId, Long cohortId, CohortUpdateRequest req) {
    validateDates(req.startDate(), req.endDate());

    Cohort cohort = requireCohort(cohortId);
    requireOwner(cohort, mentorId);
    if (cohort.getStatus() == CohortStatus.CLOSED) {
      throw new CohortClosedException("종료된 코호트는 수정할 수 없습니다");
    }

    List<String> warnings = new ArrayList<>();

    // R-U2-09 — 정원 축소 검증(U3 확정 인원 포트 조회).
    if (req.capacity() < cohort.getCapacity()) {
      int confirmed = confirmedEnrollmentQuery.confirmedCount(cohortId);
      if (req.capacity() < confirmed) {
        throw new CapacityBelowConfirmedException(
            "정원을 현재 확정 인원(" + confirmed + "명) 미만으로 축소할 수 없습니다");
      }
      warnings.add("정원이 축소되었습니다. 현재 확정 인원: " + confirmed + "명");
    }

    // R-U2-10 — 회차 수 조정(인증 회차 절단 방지 + 예정 회차 추가/삭제).
    adjustSessions(cohortId, cohort.getSessionCount(), req.sessionCount());

    cohort.edit(
        req.title().trim(),
        normalizeDescription(req.description()),
        req.capacity(),
        req.startDate(),
        req.endDate(),
        req.sessionCount());
    Cohort saved = cohortRepository.save(cohort);

    return CohortDto.from(saved, warnings);
  }

  /** 상태 전이 모집중→진행중 (W-U2-5, R-U2-09s/11). */
  @Transactional
  public CohortDto start(Long mentorId, Long cohortId) {
    Cohort cohort = requireCohort(cohortId);
    requireOwner(cohort, mentorId);

    int affected =
        cohortRepository.updateStatusGuarded(
            cohortId, CohortStatus.RECRUITING, CohortStatus.ONGOING);
    if (affected == 0) {
      // 이미 진행중/종료됨이거나 동시 전이 경쟁 — 허용되지 않은 전이.
      throw new InvalidStateTransitionException("모집중 상태의 코호트만 시작할 수 있습니다");
    }
    // 가드 UPDATE 후 영속성 컨텍스트가 비워졌으므로 최신 상태를 재조회.
    return CohortDto.from(requireCohort(cohortId));
  }

  /**
   * 종료됨 전이 경로 (U5 호출용, R-U2-13). 진행중→종료됨 상태 가드 UPDATE 만 수행하며 종료 판정·오케스트레이션은 하지 않는다(§7 경계). U5 의
   * CompletionService 가 판정 완료 후 호출한다.
   */
  @Transactional
  public void closeByCompletion(Long cohortId) {
    int affected =
        cohortRepository.updateStatusGuarded(cohortId, CohortStatus.ONGOING, CohortStatus.CLOSED);
    if (affected == 0) {
      throw new InvalidStateTransitionException("진행중 상태의 코호트만 종료할 수 있습니다");
    }
  }

  /** 목록·탐색 (W-U2-3, R-U2-19). 기본 20건·createdAt desc 는 Pageable 로 주입. */
  @Transactional(readOnly = true)
  public Page<CohortSummaryDto> list(CohortStatus statusFilter, String keyword, Pageable pageable) {
    Set<CohortStatus> statuses =
        (statusFilter != null && PUBLIC_STATUSES.contains(statusFilter))
            ? Set.of(statusFilter)
            : PUBLIC_STATUSES;

    Page<Cohort> page;
    if (keyword != null && !keyword.isBlank()) {
      page =
          cohortRepository.findByStatusInAndTitleContainingIgnoreCase(
              statuses, keyword.trim(), pageable);
    } else {
      page = cohortRepository.findByStatusIn(statuses, pageable);
    }
    return page.map(CohortSummaryDto::from);
  }

  /** 상세 조회 (W-U2-4, R-U2-19/20). 회차·최근 공지 상한 5건 포함. */
  @Transactional(readOnly = true)
  public CohortDetailDto get(Long cohortId, Long requesterId, boolean isAdmin) {
    Cohort cohort = requireCohort(cohortId);
    assertReadable(cohort, requesterId, isAdmin);

    List<Session> sessions = sessionRepository.findByCohortIdOrderBySeqAsc(cohortId);
    List<Announcement> recent =
        announcementRepository.findTop5ByCohortIdOrderByCreatedAtDesc(cohortId);
    return CohortDetailDto.from(cohort, sessions, recent);
  }

  /** 대시보드 — 내가 멘토인 코호트(최신순) (frontend-components §2.1). */
  @Transactional(readOnly = true)
  public List<CohortSummaryDto> getMine(Long mentorId) {
    return cohortRepository.findByMentorIdOrderByCreatedAtDesc(mentorId).stream()
        .map(CohortSummaryDto::from)
        .toList();
  }

  // ---- 내부 헬퍼 ----

  private void adjustSessions(Long cohortId, int currentCount, int newCount) {
    if (newCount < currentCount) {
      long verifiedInTrimmed =
          sessionRepository.countByCohortIdAndSeqGreaterThanAndStatus(
              cohortId, newCount, SessionStatus.VERIFIED);
      if (verifiedInTrimmed > 0) {
        throw new SessionVerifiedLockException("이미 인증된 회차를 잘라내는 회차 수 축소는 불가합니다");
      }
      sessionRepository.deleteByCohortIdAndSeqGreaterThan(cohortId, newCount);
    } else if (newCount > currentCount) {
      List<Session> added = new ArrayList<>(newCount - currentCount);
      for (int seq = currentCount + 1; seq <= newCount; seq++) {
        added.add(Session.scheduled(cohortId, seq));
      }
      sessionRepository.saveAll(added);
    }
  }

  private void assertReadable(Cohort cohort, Long requesterId, boolean isAdmin) {
    // 모집중·진행중은 인증 사용자에게 공개. 종료됨은 소유 멘토·관리자만(참여 이력자 조회는 U3 데이터로 후속).
    if (cohort.getStatus() == CohortStatus.CLOSED && !isAdmin && !cohort.isOwnedBy(requesterId)) {
      throw new AccessDeniedException("종료된 코호트를 조회할 권한이 없습니다");
    }
  }

  private Cohort requireCohort(Long cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
  }

  private void requireOwner(Cohort cohort, Long userId) {
    if (!cohort.isOwnedBy(userId)) {
      throw new AccessDeniedException("코호트 소유 멘토만 수행할 수 있습니다");
    }
  }

  private void validateDates(java.time.LocalDate start, java.time.LocalDate end) {
    // 컨트롤러 @Valid(@EndDateAfterStartDate)가 1차 방어하나, 서비스 직접 호출 경로도 보호한다(R-U2-04).
    if (start != null && end != null && end.isBefore(start)) {
      throw new ValidationException("종료일은 시작일과 같거나 이후여야 합니다");
    }
  }

  private String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }
    String trimmed = description.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
