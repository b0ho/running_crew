package com.learnkk.enrollment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.user.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** AdminApprovalService 단위 테스트 — 승인/거절 상태 가드 UPDATE·알림 1건(business-rules R-U3-12~15). */
@ExtendWith(MockitoExtension.class)
class AdminApprovalServiceTest {

  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CohortRepository cohortRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationService notificationService;
  @InjectMocks private AdminApprovalService adminApprovalService;

  private static final Long ADMIN = 1L;
  private static final Long ENROLLMENT = 100L;
  private static final Long COHORT = 5L;
  private static final Long MENTEE = 7L;

  private Enrollment waiting() {
    Enrollment e = Enrollment.waiting(COHORT, MENTEE);
    ReflectionTestUtils.setField(e, "id", ENROLLMENT);
    return e;
  }

  private Cohort cohort(int capacity) {
    Cohort c =
        Cohort.open(
            10L, "코호트", "설명", capacity, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 6);
    ReflectionTestUtils.setField(c, "id", COHORT);
    return c;
  }

  @Test
  void approve_대기중이면_확정_전이_및_확정알림_1건() {
    when(enrollmentRepository.updateStatusGuarded(
            ENROLLMENT, EnrollmentStatus.WAITING, EnrollmentStatus.CONFIRMED))
        .thenReturn(1);
    when(enrollmentRepository.findById(ENROLLMENT)).thenReturn(Optional.of(waiting()));
    when(cohortRepository.findById(COHORT)).thenReturn(Optional.of(cohort(20)));
    when(enrollmentRepository.countByCohortIdAndStatus(COHORT, EnrollmentStatus.CONFIRMED))
        .thenReturn(10);

    adminApprovalService.approve(ADMIN, ENROLLMENT);

    verify(notificationService)
        .notify(eq(MENTEE), eq(NotificationType.ENROLLMENT_CONFIRMED), any(String.class));
  }

  @Test
  void approve_이미_처리된_신청이면_409_그리고_알림없음() {
    // 상태 가드 UPDATE 영향 행 0 → 이미 확정/거절이거나 동시 경쟁에서 진 경우.
    when(enrollmentRepository.updateStatusGuarded(
            ENROLLMENT, EnrollmentStatus.WAITING, EnrollmentStatus.CONFIRMED))
        .thenReturn(0);

    assertThatThrownBy(() -> adminApprovalService.approve(ADMIN, ENROLLMENT))
        .isInstanceOf(InvalidStateTransitionException.class);
    verify(notificationService, never()).notify(anyLong(), any(), any());
  }

  @Test
  void approve_정원_초과여도_허용된다() {
    when(enrollmentRepository.updateStatusGuarded(
            ENROLLMENT, EnrollmentStatus.WAITING, EnrollmentStatus.CONFIRMED))
        .thenReturn(1);
    when(enrollmentRepository.findById(ENROLLMENT)).thenReturn(Optional.of(waiting()));
    when(cohortRepository.findById(COHORT)).thenReturn(Optional.of(cohort(5)));
    // 확정 인원(6) > 정원(5) — 초과 승인 허용(R-U3-13), 예외 없이 알림 발송.
    when(enrollmentRepository.countByCohortIdAndStatus(COHORT, EnrollmentStatus.CONFIRMED))
        .thenReturn(6);

    adminApprovalService.approve(ADMIN, ENROLLMENT);

    verify(notificationService)
        .notify(eq(MENTEE), eq(NotificationType.ENROLLMENT_CONFIRMED), any(String.class));
  }

  @Test
  void reject_대기중이면_거절_전이_및_거절알림() {
    when(enrollmentRepository.updateStatusGuarded(
            ENROLLMENT, EnrollmentStatus.WAITING, EnrollmentStatus.REJECTED))
        .thenReturn(1);
    when(enrollmentRepository.findById(ENROLLMENT)).thenReturn(Optional.of(waiting()));
    when(cohortRepository.findById(COHORT)).thenReturn(Optional.of(cohort(20)));

    adminApprovalService.reject(ADMIN, ENROLLMENT);

    verify(notificationService)
        .notify(eq(MENTEE), eq(NotificationType.ENROLLMENT_REJECTED), any(String.class));
  }

  @Test
  void reject_이미_처리된_신청이면_409() {
    when(enrollmentRepository.updateStatusGuarded(
            ENROLLMENT, EnrollmentStatus.WAITING, EnrollmentStatus.REJECTED))
        .thenReturn(0);

    assertThatThrownBy(() -> adminApprovalService.reject(ADMIN, ENROLLMENT))
        .isInstanceOf(InvalidStateTransitionException.class);
    verify(notificationService, never()).notify(anyLong(), any(), any());
  }
}
