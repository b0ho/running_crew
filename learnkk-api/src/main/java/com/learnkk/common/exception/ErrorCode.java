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

  // U2 cohort — 상태/정원/회차 충돌 (business-rules §6, R-U2-21a~d)
  public static final String COHORT_CLOSED = "COHORT_CLOSED";
  public static final String CAPACITY_BELOW_CONFIRMED = "CAPACITY_BELOW_CONFIRMED";
  public static final String SESSION_VERIFIED_LOCK = "SESSION_VERIFIED_LOCK";
  public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";

  // U3 enrollment — 선착순 참여/승인 충돌 (business-rules §5, R-U3-21a~e)
  public static final String ALREADY_ENROLLED = "ALREADY_ENROLLED";
  public static final String SELF_ENROLLMENT = "SELF_ENROLLMENT";
  public static final String COHORT_NOT_OPEN = "COHORT_NOT_OPEN";
  public static final String ENROLLMENT_BUSY = "ENROLLMENT_BUSY";

  private ErrorCode() {}
}
