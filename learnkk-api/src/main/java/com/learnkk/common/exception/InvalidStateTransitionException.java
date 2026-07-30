package com.learnkk.common.exception;

/** 허용되지 않은 상태 전이(가드 UPDATE 영향 행 0 포함) (R-U2-11 → 409 INVALID_STATE_TRANSITION). */
public class InvalidStateTransitionException extends RuntimeException {

  public InvalidStateTransitionException(String message) {
    super(message);
  }
}
