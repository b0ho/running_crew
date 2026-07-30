package com.learnkk.common.exception;

/** 파일 제약 위반 — MIME/크기/확장자 (R-U1-17h → 400 FILE_CONSTRAINT_VIOLATION). */
public class FileConstraintViolationException extends RuntimeException {

  public FileConstraintViolationException(String message) {
    super(message);
  }
}
