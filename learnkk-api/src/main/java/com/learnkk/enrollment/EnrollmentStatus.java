package com.learnkk.enrollment;

/**
 * 참여 상태 (domain-entities.md §2.1, INV-U3-3).
 *
 * <p>전이: 신청 → (확정|대기중), 대기중 → (확정|거절). 확정·거절은 종결 상태이며 파일럿에서 취소·자동승격·재신청 전이는 없다(R-U3-20,
 * cid:user-stories:c4).
 */
public enum EnrollmentStatus {
  CONFIRMED("확정"),
  WAITING("대기중"),
  REJECTED("거절");

  private final String displayName;

  EnrollmentStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
