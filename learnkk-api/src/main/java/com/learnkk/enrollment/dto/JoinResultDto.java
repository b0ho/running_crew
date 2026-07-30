package com.learnkk.enrollment.dto;

import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentStatus;

/**
 * 선착순 참여 결과 DTO (W-U3-1, frontend-components §2.1).
 *
 * <p>{@code status} 로 확정/대기 분기를 안내한다. 대기(WAITING)인 경우 {@code waitingPosition}(현재 대기 인원 순번)을 함께
 * 제공하고, 확정(CONFIRMED)이면 null 이다.
 */
public record JoinResultDto(
    Long enrollmentId,
    Long cohortId,
    EnrollmentStatus status,
    String statusLabel,
    Integer waitingPosition) {

  public static JoinResultDto from(Enrollment enrollment, Integer waitingPosition) {
    return new JoinResultDto(
        enrollment.getId(),
        enrollment.getCohortId(),
        enrollment.getStatus(),
        enrollment.getStatus().getDisplayName(),
        enrollment.getStatus() == EnrollmentStatus.WAITING ? waitingPosition : null);
  }
}
