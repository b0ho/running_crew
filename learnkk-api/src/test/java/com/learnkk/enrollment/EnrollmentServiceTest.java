package com.learnkk.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.common.exception.AlreadyEnrolledException;
import com.learnkk.common.exception.CohortNotOpenException;
import com.learnkk.common.exception.EnrollmentBusyException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.SelfEnrollmentException;
import com.learnkk.enrollment.dto.JoinResultDto;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * EnrollmentService 단위 테스트 — join 사전 검증·확정/대기 결정·락 타임아웃(business-rules R-U3-01~09).
 *
 * <p>동시성 정확성(정원 초과 방지)은 Testcontainers 통합 테스트({@link EnrollmentConcurrencyIntegrationTest})가 실 DB 행
 * 락으로 검증한다. 본 단위 테스트는 분기 로직을 격리 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

  @Mock private CohortRepository cohortRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private NotificationService notificationService;
  @InjectMocks private EnrollmentService enrollmentService;

  private static final Long MENTEE = 7L;
  private static final Long MENTOR = 10L;
  private static final Long COHORT = 1L;

  private Cohort cohort(Long id, Long mentorId, CohortStatus status, int capacity) {
    Cohort c =
        Cohort.open(
            mentorId,
            "자바 멘토링",
            "설명",
            capacity,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 1),
            6);
    ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "status", status);
    return c;
  }

  @Test
  void join_코호트가_없으면_404() {
    when(cohortRepository.findById(COHORT)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> enrollmentService.join(MENTEE, COHORT))
        .isInstanceOf(EntityNotFoundException.class);
    verify(cohortRepository, never()).findByIdForUpdate(anyLong());
  }

  @Test
  void join_종료됨_코호트면_409_CohortNotOpen() {
    when(cohortRepository.findById(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.CLOSED, 20)));

    assertThatThrownBy(() -> enrollmentService.join(MENTEE, COHORT))
        .isInstanceOf(CohortNotOpenException.class);
    verify(cohortRepository, never()).findByIdForUpdate(anyLong());
  }

  @Test
  void join_본인이_멘토인_코호트면_409_SelfEnrollment() {
    when(cohortRepository.findById(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTEE, CohortStatus.RECRUITING, 20)));

    assertThatThrownBy(() -> enrollmentService.join(MENTEE, COHORT))
        .isInstanceOf(SelfEnrollmentException.class);
    verify(cohortRepository, never()).findByIdForUpdate(anyLong());
  }

  @Test
  void join_이미_신청했으면_409_AlreadyEnrolled() {
    when(cohortRepository.findById(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(cohortRepository.findByIdForUpdate(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT, MENTEE))
        .thenReturn(Optional.of(Enrollment.confirmed(COHORT, MENTEE)));

    assertThatThrownBy(() -> enrollmentService.join(MENTEE, COHORT))
        .isInstanceOf(AlreadyEnrolledException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void join_정원_여유면_확정저장_및_알림() {
    when(cohortRepository.findById(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(cohortRepository.findByIdForUpdate(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT, MENTEE))
        .thenReturn(Optional.empty());
    when(enrollmentRepository.countByCohortIdAndStatus(COHORT, EnrollmentStatus.CONFIRMED))
        .thenReturn(5);
    when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    JoinResultDto result = enrollmentService.join(MENTEE, COHORT);

    assertThat(result.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(result.waitingPosition()).isNull();
    verify(notificationService)
        .notify(eq(MENTEE), eq(NotificationType.ENROLLMENT_CONFIRMED), any(String.class));
  }

  @Test
  void join_정원_마감이면_대기저장_및_알림없음() {
    when(cohortRepository.findById(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(cohortRepository.findByIdForUpdate(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT, MENTEE))
        .thenReturn(Optional.empty());
    when(enrollmentRepository.countByCohortIdAndStatus(COHORT, EnrollmentStatus.CONFIRMED))
        .thenReturn(20);
    when(enrollmentRepository.countByCohortIdAndStatus(COHORT, EnrollmentStatus.WAITING))
        .thenReturn(3);
    when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    JoinResultDto result = enrollmentService.join(MENTEE, COHORT);

    assertThat(result.status()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(result.waitingPosition()).isEqualTo(3);
    verify(notificationService, never()).notify(anyLong(), any(), any());
  }

  @Test
  void join_락_타임아웃이면_409_EnrollmentBusy() {
    when(cohortRepository.findById(COHORT))
        .thenReturn(Optional.of(cohort(COHORT, MENTOR, CohortStatus.RECRUITING, 20)));
    when(cohortRepository.findByIdForUpdate(COHORT))
        .thenThrow(new PessimisticLockingFailureException("lock timeout"));

    assertThatThrownBy(() -> enrollmentService.join(MENTEE, COHORT))
        .isInstanceOf(EnrollmentBusyException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }
}
