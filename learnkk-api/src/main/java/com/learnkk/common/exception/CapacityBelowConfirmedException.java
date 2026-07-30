package com.learnkk.common.exception;

/** 확정 인원 미만으로 정원 축소 시도 (R-U2-09 → 409 CAPACITY_BELOW_CONFIRMED). */
public class CapacityBelowConfirmedException extends RuntimeException {

  public CapacityBelowConfirmedException(String message) {
    super(message);
  }
}
