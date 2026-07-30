package com.learnkk.enrollment;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.enrollment.dto.WaitingEnrollmentDto;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대기 승인/거절 서비스 (business-logic-model.md §4, business-rules R-U3-11~15).
 *
 * <p>승인/거절은 상태 가드 조건부 UPDATE({@code WHERE id=? AND status=WAITING})로 수행한다. 영향 행 0 이면 이미 처리된 신청이므로
 * 409 INVALID_STATE_TRANSITION 으로 매핑하며, 두 관리자의 동시 승인 경합에서 DB 행 락이 UPDATE 를 직렬화해 정확히 1건만 성공·알림 1건만
 * 생성한다(reliability-design.md §2). 승인은 정원을 초과할 수 있으며(관리자 수동 판단, R-U3-13) 초과 시 감사 로그를 남긴다. 상태 전이와 알림은
 * 동일 트랜잭션에서 커밋된다.
 */
@Service
public class AdminApprovalService {

  private static final Logger log = LoggerFactory.getLogger(AdminApprovalService.class);

  private final EnrollmentRepository enrollmentRepository;
  private final CohortRepository cohortRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  public AdminApprovalService(
      EnrollmentRepository enrollmentRepository,
      CohortRepository cohortRepository,
      UserRepository userRepository,
      NotificationService notificationService) {
    this.enrollmentRepository = enrollmentRepository;
    this.cohortRepository = cohortRepository;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  /** 대기 목록 조회 (W-U3-4, R-U3-11). cohortId 가 null 이면 전체 대기 신청, 지정되면 해당 코호트만. 신청 순(createdAt asc). */
  @Transactional(readOnly = true)
  public Page<WaitingEnrollmentDto> listWaiting(Long cohortId, Pageable pageable) {
    Page<Enrollment> page =
        (cohortId == null)
            ? enrollmentRepository.findByStatusOrderByCreatedAtAsc(
                EnrollmentStatus.WAITING, pageable)
            : enrollmentRepository.findByCohortIdAndStatusOrderByCreatedAtAsc(
                cohortId, EnrollmentStatus.WAITING, pageable);

    List<Enrollment> rows = page.getContent();
    Map<Long, String> titles = cohortTitles(rows.stream().map(Enrollment::getCohortId).toList());
    Map<Long, User> mentees = mentees(rows.stream().map(Enrollment::getMenteeId).toList());

    return page.map(
        e -> {
          User mentee = mentees.get(e.getMenteeId());
          return WaitingEnrollmentDto.from(
              e,
              titles.get(e.getCohortId()),
              mentee == null ? null : mentee.getName(),
              mentee == null ? null : mentee.getNickname());
        });
  }

  /** 대기 승인 (W-U3-5, R-U3-12/13/15). 대기중→확정 상태 가드 UPDATE → 확정 알림 1건. 정원 초과 승인은 허용하되 감사 로그를 남긴다. */
  @Transactional
  public void approve(Long adminId, Long enrollmentId) {
    Enrollment enrollment =
        decideGuarded(enrollmentId, EnrollmentStatus.CONFIRMED, "대기중 상태의 신청만 승인할 수 있습니다");

    String title = cohortTitle(enrollment.getCohortId());
    warnIfOverCapacity(adminId, enrollment.getCohortId());

    notificationService.notify(
        enrollment.getMenteeId(),
        NotificationType.ENROLLMENT_CONFIRMED,
        "'" + title + "' 코호트 참여가 확정되었습니다");
  }

  /** 대기 거절 (W-U3-6, R-U3-14/15). 대기중→거절 상태 가드 UPDATE → 거절 알림 1건. */
  @Transactional
  public void reject(Long adminId, Long enrollmentId) {
    Enrollment enrollment =
        decideGuarded(enrollmentId, EnrollmentStatus.REJECTED, "대기중 상태의 신청만 거절할 수 있습니다");

    String title = cohortTitle(enrollment.getCohortId());
    notificationService.notify(
        enrollment.getMenteeId(),
        NotificationType.ENROLLMENT_REJECTED,
        "'" + title + "' 코호트 참여가 거절되었습니다");
  }

  // ---- 내부 헬퍼 ----

  private Enrollment decideGuarded(Long enrollmentId, EnrollmentStatus to, String conflictMessage) {
    int affected =
        enrollmentRepository.updateStatusGuarded(enrollmentId, EnrollmentStatus.WAITING, to);
    if (affected == 0) {
      // 이미 확정/거절이거나 동시 처리 경쟁에서 진 경우 — 허용되지 않은 전이(알림 중복 방지).
      throw new InvalidStateTransitionException(conflictMessage);
    }
    return enrollmentRepository
        .findById(enrollmentId)
        .orElseThrow(() -> new EntityNotFoundException("신청을 찾을 수 없습니다"));
  }

  private void warnIfOverCapacity(Long adminId, Long cohortId) {
    Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
    if (cohort == null) {
      return;
    }
    int confirmed =
        enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED);
    if (confirmed > cohort.getCapacity()) {
      // R-U3-13 — 정원 초과 승인은 관리자 판단으로 허용하되 감사 로그를 남긴다.
      log.warn(
          "정원 초과 승인: adminId={}, cohortId={}, 확정 인원={}, 정원={}",
          adminId,
          cohortId,
          confirmed,
          cohort.getCapacity());
    }
  }

  private String cohortTitle(Long cohortId) {
    return cohortRepository.findById(cohortId).map(Cohort::getTitle).orElse("");
  }

  private Map<Long, String> cohortTitles(List<Long> cohortIds) {
    if (cohortIds.isEmpty()) {
      return Map.of();
    }
    return cohortRepository.findAllById(cohortIds).stream()
        .collect(Collectors.toMap(Cohort::getId, Cohort::getTitle, (a, b) -> a));
  }

  private Map<Long, User> mentees(List<Long> menteeIds) {
    if (menteeIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(menteeIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
  }
}
