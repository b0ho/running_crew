package com.learnkk.common.dto;

import java.time.Instant;

/**
 * 공통 에러 응답 DTO (R-U1-17).
 *
 * <p>모든 오류 응답은 이 형식으로 정규화된다. 내부 상세(스택트레이스 등)는 포함하지 않는다(R-U1-19).
 */
public record ErrorResponse(String code, String message, Instant timestamp, String path) {

  public static ErrorResponse of(String code, String message, String path) {
    return new ErrorResponse(code, message, Instant.now(), path);
  }
}
