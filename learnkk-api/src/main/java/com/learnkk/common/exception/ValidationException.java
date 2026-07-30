package com.learnkk.common.exception;

/** 도메인 검증 실패 (R-U1-17a → 400 VALIDATION_ERROR). */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}
