package com.learnkk.common.exception;

/** 본인이 개설한 코호트에 멘티로 참여 시도 (R-U3-05/21c → 409 SELF_ENROLLMENT). */
public class SelfEnrollmentException extends RuntimeException {

  public SelfEnrollmentException(String message) {
    super(message);
  }
}
