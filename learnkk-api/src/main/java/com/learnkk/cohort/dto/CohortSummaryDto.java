package com.learnkk.cohort.dto;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortStatus;
import java.time.LocalDate;

/**
 * 코호트 요약 DTO — 목록/대시보드 카드용 (R-U2-19, frontend-components §2.1).
 *
 * <p>회차 컬렉션을 로딩하지 않고 요약 필드만 담아 목록 조회 비용을 상한한다(performance-design.md §2).
 */
public record CohortSummaryDto(
    Long id,
    String title,
    CohortStatus status,
    String statusLabel,
    int capacity,
    LocalDate startDate,
    LocalDate endDate,
    int sessionCount,
    Long mentorId) {

  public static CohortSummaryDto from(Cohort cohort) {
    return new CohortSummaryDto(
        cohort.getId(),
        cohort.getTitle(),
        cohort.getStatus(),
        cohort.getStatus().getDisplayName(),
        cohort.getCapacity(),
        cohort.getStartDate(),
        cohort.getEndDate(),
        cohort.getSessionCount(),
        cohort.getMentorId());
  }
}
