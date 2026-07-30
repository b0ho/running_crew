package com.learnkk.enrollment.dto;

import com.learnkk.enrollment.Enrollment;
import java.time.Instant;

/**
 * 관리자 대기 목록 행 DTO (W-U3-4, frontend-components §2.4).
 *
 * <p>대기중(WAITING) 참여에 코호트 제목·멘티 요약(이름·닉네임)을 조합해 관리자 승인 화면에 노출한다. Entity 를 직접 노출하지 않는다.
 */
public record WaitingEnrollmentDto(
    Long enrollmentId,
    Long cohortId,
    String cohortTitle,
    Long menteeId,
    String menteeName,
    String menteeNickname,
    Instant createdAt) {

  public static WaitingEnrollmentDto from(
      Enrollment enrollment, String cohortTitle, String menteeName, String menteeNickname) {
    return new WaitingEnrollmentDto(
        enrollment.getId(),
        enrollment.getCohortId(),
        cohortTitle,
        enrollment.getMenteeId(),
        menteeName,
        menteeNickname,
        enrollment.getCreatedAt());
  }
}
