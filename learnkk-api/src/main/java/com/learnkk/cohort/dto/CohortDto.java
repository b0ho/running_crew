package com.learnkk.cohort.dto;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 코호트 단건 응답 DTO (INV-U2-4).
 *
 * <p>{@code warnings} 는 정원 축소가 확정 인원 이상이어서 허용되되 주의가 필요한 경우 등의 경고 메시지를 담는다(reliability-design
 * warnings[]). 경고가 없으면 빈 리스트.
 */
public record CohortDto(
    Long id,
    Long mentorId,
    String title,
    String description,
    int capacity,
    LocalDate startDate,
    LocalDate endDate,
    int sessionCount,
    CohortStatus status,
    String statusLabel,
    Instant createdAt,
    List<String> warnings) {

  public static CohortDto from(Cohort cohort) {
    return from(cohort, List.of());
  }

  public static CohortDto from(Cohort cohort, List<String> warnings) {
    return new CohortDto(
        cohort.getId(),
        cohort.getMentorId(),
        cohort.getTitle(),
        cohort.getDescription(),
        cohort.getCapacity(),
        cohort.getStartDate(),
        cohort.getEndDate(),
        cohort.getSessionCount(),
        cohort.getStatus(),
        cohort.getStatus().getDisplayName(),
        cohort.getCreatedAt(),
        warnings == null ? List.of() : List.copyOf(warnings));
  }
}
