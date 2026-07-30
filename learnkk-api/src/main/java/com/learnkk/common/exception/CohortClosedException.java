package com.learnkk.common.exception;

/** 종료됨 코호트 수정 시도 (R-U2-08 → 409 COHORT_CLOSED). */
public class CohortClosedException extends RuntimeException {

  public CohortClosedException(String message) {
    super(message);
  }
}
