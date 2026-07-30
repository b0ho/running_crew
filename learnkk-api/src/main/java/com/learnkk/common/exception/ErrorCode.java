package com.learnkk.common.exception;

/** 표준 에러 code 값 (business-rules §4). */
public final class ErrorCode {

  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
  public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
  public static final String UNAUTHORIZED = "UNAUTHORIZED";
  public static final String FORBIDDEN = "FORBIDDEN";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String FILE_CONSTRAINT_VIOLATION = "FILE_CONSTRAINT_VIOLATION";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private ErrorCode() {}
}
