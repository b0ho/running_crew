package com.learnkk.common.exception;

/** 리소스 미존재 (R-U1-17g → 404 NOT_FOUND). */
public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException(String message) {
    super(message);
  }
}
