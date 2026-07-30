package com.learnkk.attendance.dto;

import com.learnkk.cohort.SessionStatus;
import java.util.List;

/**
 * 코호트 진도·출석 응답 DTO (R-U4-10, frontend-components §2.1).
 *
 * <p>진도율 = 인증(VERIFIED) 회차 수 / 전체 회차 수. 인증 수·전체 수·진도율(0.0~1.0)과 회차별 출석 목록을 함께 제공한다. 전체 회차가 0 이면
 * 진도율은 0.0 으로 정의한다.
 */
public record CohortAttendanceDto(
    Long cohortId,
    int verifiedCount,
    int totalCount,
    double progressRate,
    List<SessionAttendanceDto> sessions) {

  public static CohortAttendanceDto of(Long cohortId, List<SessionAttendanceDto> sessions) {
    int total = sessions.size();
    int verified =
        (int) sessions.stream().filter(s -> s.status() == SessionStatus.VERIFIED).count();
    double rate = total == 0 ? 0.0 : (double) verified / total;
    return new CohortAttendanceDto(cohortId, verified, total, rate, sessions);
  }
}
