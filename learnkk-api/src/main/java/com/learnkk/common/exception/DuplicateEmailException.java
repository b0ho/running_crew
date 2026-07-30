package com.learnkk.common.exception;

/** email 중복 (R-U1-17b → 409 DUPLICATE_EMAIL). */
public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException(String message) {
    super(message);
  }
}
