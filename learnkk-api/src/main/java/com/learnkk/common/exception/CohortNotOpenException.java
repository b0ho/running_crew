package com.learnkk.common.exception;

/** 종료됨(CLOSED) 코호트에 신규 참여 시도 (R-U3-06/21d → 409 COHORT_NOT_OPEN). */
public class CohortNotOpenException extends RuntimeException {

  public CohortNotOpenException(String message) {
    super(message);
  }
}
