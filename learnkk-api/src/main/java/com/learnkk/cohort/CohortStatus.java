package com.learnkk.cohort;

/**
 * 코호트 상태 (domain-entities.md §2.1, INV-U2-1).
 *
 * <p>전이는 단방향: 모집중 → 진행중 → 종료됨. 역전이 없음(R-U2-11). 모집중→진행중 전이는 U2(멘토 시작 액션)가, 진행중→종료됨 전이는 U5(종료
 * 오케스트레이션)가 트리거하되 U2 가 노출하는 상태 가드 UPDATE 경로를 사용한다.
 */
public enum CohortStatus {
  RECRUITING("모집중"),
  ONGOING("진행중"),
  CLOSED("종료됨");

  private final String displayName;

  CohortStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
