package com.learnkk.cohort.dto;

import com.learnkk.cohort.Announcement;
import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 코호트 상세 응답 DTO — 기본정보 + 회차 목록 + 최근 공지(상한 5건) (R-U2-20, performance-design.md §3).
 *
 * <p>상세 응답 1회로 회차·최근 공지를 함께 수신해 FE 추가 라운드트립을 줄인다. 전체 공지는 별도 페이지네이션 엔드포인트로 조회한다.
 */
public record CohortDetailDto(
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
    List<SessionDto> sessions,
    List<AnnouncementDto> recentAnnouncements) {

  public static CohortDetailDto from(
      Cohort cohort, List<Session> sessions, List<Announcement> recentAnnouncements) {
    return new CohortDetailDto(
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
        sessions.stream().map(SessionDto::from).toList(),
        recentAnnouncements.stream().map(AnnouncementDto::from).toList());
  }
}
