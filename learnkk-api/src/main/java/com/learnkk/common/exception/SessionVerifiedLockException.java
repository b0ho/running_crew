package com.learnkk.common.exception;

/** 이미 인증된 회차를 잘라내는 회차 수 축소 시도 (R-U2-10 → 409 SESSION_VERIFIED_LOCK). */
public class SessionVerifiedLockException extends RuntimeException {

  public SessionVerifiedLockException(String message) {
    super(message);
  }
}
