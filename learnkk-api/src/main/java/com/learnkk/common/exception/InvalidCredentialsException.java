package com.learnkk.common.exception;

/**
 * 로그인 실패 (R-U1-17d → 401 INVALID_CREDENTIALS).
 *
 * <p>미존재 계정과 비밀번호 불일치를 동일하게 처리해 사용자 열거를 방지한다(R-U1-09).
 */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("이메일 또는 비밀번호가 올바르지 않습니다");
  }
}
