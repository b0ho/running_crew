package com.learnkk.common.exception;

/**
 * 데이터 정합 오류 (R-U5-21d → 500 INTERNAL_ERROR).
 *
 * <p>정상 흐름에서 발생해서는 안 되는 불변식 위반(예: 종료 판정 시 전체 회차 수가 0)을 나타낸다. 종료 전 회차 존재 보장은 U2 R-U2-03 이며, 그럼에도 정합이
 * 깨진 경우 이 예외로 안전하게 500 을 반환한다(부분 커밋 방지).
 */
public class DataIntegrityException extends RuntimeException {

  public DataIntegrityException(String message) {
    super(message);
  }
}
