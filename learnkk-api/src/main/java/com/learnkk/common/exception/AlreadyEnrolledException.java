package com.learnkk.common.exception;

/** 이미 신청한 코호트 재신청·동시 이중 제출 (R-U3-04/21a/21b → 409 ALREADY_ENROLLED). */
public class AlreadyEnrolledException extends RuntimeException {

  public AlreadyEnrolledException(String message) {
    super(message);
  }
}
