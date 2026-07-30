package com.learnkk.enrollment.dto;

import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentStatus;
import java.time.Instant;

/**
 * 참여 단건 응답 DTO (INV-U3-4, R-U3-16).
 *
 * <p>{@code cohortTitle} 은 코호트(U2) 조회 조합으로 채운다. 크로스유닛 계약(confirmedEnrollments)에서 목록 반환 시에도 사용한다.
 * Entity 를 직접 노출하지 않는다.
 */
public record EnrollmentDto(
    Long id,
    Long cohortId,
    String cohortTitle,
    Long menteeId,
    EnrollmentStatus status,
    String statusLabel,
    Instant createdAt,
    Instant decidedAt) {

  public static EnrollmentDto from(Enrollment enrollment, String cohortTitle) {
    return new EnrollmentDto(
        enrollment.getId(),
        enrollment.getCohortId(),
        cohortTitle,
        enrollment.getMenteeId(),
        enrollment.getStatus(),
        enrollment.getStatus().getDisplayName(),
        enrollment.getCreatedAt(),
        enrollment.getDecidedAt());
  }
}
