package com.learnkk.completion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 최종 보고서 제출 요청 DTO (R-U5-15).
 *
 * <p>{@code body}(자유 서식 본문)는 필수이며, 파일 첨부는 multipart 의 별도 파트로 선택 전송한다(요청 바디에 포함하지 않음). Bean
 * Validation 위반은 U1 GlobalExceptionHandler 가 400 VALIDATION_ERROR 로 매핑한다.
 */
public record ReportSubmitRequest(
    @NotBlank(message = "보고서 본문은 필수입니다") @Size(max = 20000, message = "보고서 본문은 최대 20000자입니다")
        String body) {}
