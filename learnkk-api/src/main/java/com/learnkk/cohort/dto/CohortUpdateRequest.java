package com.learnkk.cohort.dto;

import com.learnkk.common.validation.DateRange;
import com.learnkk.common.validation.EndDateAfterStartDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 코호트 수정 요청 (W-U2-2, R-U2-01~04/09/10).
 *
 * <p>개설 요청과 동일한 필드 제약을 적용한다. capacity 축소·sessionCount 변경에 따른 도메인 규칙(확정 인원·인증 회차 락)은 서비스가 검증한다.
 */
@EndDateAfterStartDate
public record CohortUpdateRequest(
    @NotBlank(message = "제목은 필수입니다") @Size(max = 200, message = "제목은 최대 200자입니다") String title,
    String description,
    @NotNull(message = "정원은 필수입니다") @Min(value = 1, message = "정원은 1 이상이어야 합니다") Integer capacity,
    @NotNull(message = "시작일은 필수입니다") LocalDate startDate,
    @NotNull(message = "종료일은 필수입니다") LocalDate endDate,
    @NotNull(message = "회차 수는 필수입니다")
        @Min(value = 1, message = "회차 수는 1 이상이어야 합니다")
        @Max(value = 100, message = "회차 수는 최대 100 입니다")
        Integer sessionCount)
    implements DateRange {}
