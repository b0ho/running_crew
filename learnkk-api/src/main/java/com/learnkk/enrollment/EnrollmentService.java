package com.learnkk.enrollment;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.common.exception.AlreadyEnrolledException;
import com.learnkk.common.exception.CohortNotOpenException;
import com.learnkk.common.exception.EnrollmentBusyException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.SelfEnrollmentException;
import com.learnkk.enrollment.dto.EnrollmentDto;
import com.learnkk.enrollment.dto.JoinResultDto;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여 도메인 서비스 — 선착순 참여 동시성 제어의 핵심 (business-logic-model.md §2, business-rules R-U3-01~10).
 *
 * <p>{@link #join}은 단일 트랜잭션 안에서 (1) 락 밖 사전 검증 → (2) 대상 Cohort 행 비관적 쓰기 락 획득 → (3) <b>락 보유 상태에서</b>
 * 확정 인원 집계 → (4) 상태 결정 및 저장 → (5) 확정 시 알림 순으로 수행한다. 집계를 락 구간 밖으로 분리하지 않으며(R-U3-07a),
 * UNIQUE(cohort_id, mentee_id)를 최종 방어선으로 둔다(R-U3-08). 정원 초과 확정이 어떤 동시 실행에서도 발생하지 않는다(INV-U3-1).
 */
@Service
public class EnrollmentService {

  private final CohortRepository cohortRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final NotificationService notificationService;

  public EnrollmentService(
      CohortRepository cohortRepository,
      EnrollmentRepository enrollmentRepository,
      NotificationService notificationService) {
    this.cohortRepository = cohortRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.notificationService = notificationService;
  }

  /**
   * 선착순 참여 신청 (W-U3-1, R-U3-01~09). menteeId 는 세션에서 해석된 값이며 요청 바디로 신뢰하지 않는다.
   *
   * @return 확정/대기 결과. 대기 시 대기 순번을 포함한다.
   */
  @Transactional
  public JoinResultDto join(Long menteeId, Long cohortId) {
    // 1) 락 밖 사전 검증 — 락 보유 구간을 최소화한다(performance-design.md §2).
    Cohort preCheck =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
    if (preCheck.getStatus() == CohortStatus.CLOSED) {
      throw new CohortNotOpenException("참여할 수 없는 코호트입니다");
    }
    if (preCheck.isOwnedBy(menteeId)) {
      throw new SelfEnrollmentException("본인이 개설한 코호트에는 참여할 수 없습니다");
    }

    // 2) 비관적 쓰기 락 획득(R-U3-07). 동일 코호트의 동시 join 을 직렬화한다.
    Cohort locked = acquireLock(cohortId);

    // 3) 사전 중복 조회(R-U3-04). 락 보유 상태에서 기존 신청 존재 여부 확인.
    if (enrollmentRepository.findByCohortIdAndMenteeId(cohortId, menteeId).isPresent()) {
      throw new AlreadyEnrolledException("이미 신청한 코호트입니다");
    }

    // 4) 락 보유 상태에서 확정 인원 집계(R-U3-07a/09) → 상태 결정(R-U3-02).
    int confirmed =
        enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED);
    boolean hasRoom = confirmed < locked.getCapacity();

    Enrollment enrollment =
        hasRoom ? Enrollment.confirmed(cohortId, menteeId) : Enrollment.waiting(cohortId, menteeId);

    // 5) 저장 — UNIQUE 제약이 최종 방어선(R-U3-08). 동시 이중 제출 위반은 서비스 레벨에서 변환(R-U3-21b)해
    //    U1 전역 DataIntegrityViolation→DUPLICATE_EMAIL 핸들러와의 충돌을 피한다.
    Enrollment saved;
    try {
      saved = enrollmentRepository.saveAndFlush(enrollment);
    } catch (DataIntegrityViolationException ex) {
      throw new AlreadyEnrolledException("이미 신청한 코호트입니다");
    }

    // 6) 확정이면 즉시 알림(동일 트랜잭션, reliability-design.md §2). 대기면 대기 순번 안내.
    Integer waitingPosition = null;
    if (hasRoom) {
      notificationService.notify(
          menteeId,
          NotificationType.ENROLLMENT_CONFIRMED,
          "'" + locked.getTitle() + "' 코호트 참여가 확정되었습니다");
    } else {
      waitingPosition =
          enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.WAITING);
    }

    return JoinResultDto.from(saved, waitingPosition);
  }

  /**
   * 내 신청 목록 (W-U3-2, R-U3-16/17). menteeId 는 세션에서 해석된 본인 id 여야 한다(수평 권한 상승 방지). 코호트 제목은 배치 조회로
   * 조합한다(N+1 회피).
   */
  @Transactional(readOnly = true)
  public Page<EnrollmentDto> myApplications(Long menteeId, Pageable pageable) {
    Page<Enrollment> page =
        enrollmentRepository.findByMenteeIdOrderByCreatedAtDesc(menteeId, pageable);
    Map<Long, String> titles =
        cohortTitles(page.getContent().stream().map(Enrollment::getCohortId).toList());
    return page.map(e -> EnrollmentDto.from(e, titles.get(e.getCohortId())));
  }

  /**
   * 확정 인원 집계 (W-U3-3, §7 크로스유닛 계약). U6 집계 등에서 사용한다. U2 는 별도 포트({@link EnrollmentQueryAdapter})로
   * 접근하며, 이 메서드는 서비스 계약 노출용이다.
   */
  @Transactional(readOnly = true)
  public int confirmedCount(Long cohortId) {
    return enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED);
  }

  /** 확정 멘티 목록 (W-U3-3, §7 크로스유닛 계약). U5 수료증 발급 순회·U6 집계에서 사용한다. */
  @Transactional(readOnly = true)
  public List<EnrollmentDto> confirmedEnrollments(Long cohortId) {
    String title = cohortRepository.findById(cohortId).map(Cohort::getTitle).orElse(null);
    return enrollmentRepository
        .findByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED)
        .stream()
        .map(e -> EnrollmentDto.from(e, title))
        .toList();
  }

  // ---- 내부 헬퍼 ----

  private Cohort acquireLock(Long cohortId) {
    try {
      return cohortRepository
          .findByIdForUpdate(cohortId)
          .orElseThrow(() -> new EntityNotFoundException("코호트를 찾을 수 없습니다"));
    } catch (PessimisticLockingFailureException
        | LockTimeoutException
        | PessimisticLockException ex) {
      // 락 타임아웃(jakarta.persistence.lock.timeout=3000ms 초과) → 409 ENROLLMENT_BUSY.
      throw new EnrollmentBusyException("신청이 몰려 잠시 후 다시 시도해 주세요");
    }
  }

  private Map<Long, String> cohortTitles(List<Long> cohortIds) {
    if (cohortIds.isEmpty()) {
      return Map.of();
    }
    return cohortRepository.findAllById(cohortIds).stream()
        .collect(Collectors.toMap(Cohort::getId, Cohort::getTitle, (a, b) -> a));
  }
}
