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
 * 코호트 개설 요청 (R-U2-01~04, performance-design.md §3 상한).
 *
 * <p>필드 검증: title 필수·≤200, capacity≥1, sessionCount 1..100(원자 트랜잭션 보호 상한), 날짜 필수. 종료일≥시작일은 클래스 레벨
 * {@link EndDateAfterStartDate} 교차 검증(R-U2-04).
 */
@EndDateAfterStartDate
public record CohortCreateRequest(
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
