package com.learnkk.cohort.port;

/**
 * 확정 참여 인원 조회 포트 (business-logic-model.md §8, R-U2-09).
 *
 * <p>U2 는 정원 축소 검증 시 이 계약으로 현재 확정 인원을 읽는다(U2→U3 읽기 전용, 순환 없음). 파일럿 단계에서 U3(enrollment)가 아직 빌드되지 않았을
 * 때는 기본 구현({@code CohortPortConfig})이 0 을 반환하며, U3 빌드 시 실제 빈이 이를 대체한다.
 */
public interface ConfirmedEnrollmentQuery {

  /** 해당 코호트의 확정(승인) 참여 인원 수. */
  int confirmedCount(Long cohortId);
}
