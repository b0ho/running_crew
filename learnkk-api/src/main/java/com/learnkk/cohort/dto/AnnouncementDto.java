package com.learnkk.cohort.dto;

import com.learnkk.cohort.Announcement;
import java.time.Instant;

/** 공지 응답 DTO (INV-U2-4, US-5). */
public record AnnouncementDto(
    Long id, Long cohortId, String body, String externalLink, Instant createdAt) {

  public static AnnouncementDto from(Announcement announcement) {
    return new AnnouncementDto(
        announcement.getId(),
        announcement.getCohortId(),
        announcement.getBody(),
        announcement.getExternalLink(),
        announcement.getCreatedAt());
  }
}
