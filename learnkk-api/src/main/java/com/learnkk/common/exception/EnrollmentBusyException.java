package com.learnkk.common.exception;

/**
 * 참여 신청 락 경합 타임아웃 (performance-design.md §2 → 409 ENROLLMENT_BUSY).
 *
 * <p>비관적 락(SELECT ... FOR UPDATE) 획득이 {@code jakarta.persistence.lock.timeout}(3000ms) 내에 실패하면
 * 발생한다. 파일럿은 서버 자동 재시도를 두지 않으며 재시도는 클라이언트 몫이다(reliability-design.md §2).
 */
public class EnrollmentBusyException extends RuntimeException {

  public EnrollmentBusyException(String message) {
    super(message);
  }
}
