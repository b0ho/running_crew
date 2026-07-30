package com.learnkk.enrollment;

import com.learnkk.cohort.port.ConfirmedEnrollmentQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * U2 크로스유닛 포트 실 구현 (business-logic-model.md §3/§7, R-U2-09).
 *
 * <p>U2 {@link ConfirmedEnrollmentQuery} 계약을 실제 확정 인원 집계로 구현한다. 이 {@code @Component} 빈이 존재하면 U2 의
 * 파일럿 기본 빈({@code CohortPortConfig}, 항상 0 반환, {@code @ConditionalOnMissingBean})은 자동으로 대체되어, U2 의
 * 정원 축소 검증(R-U2-09)이 실제 확정 인원 기준으로 동작한다.
 */
@Component
public class EnrollmentQueryAdapter implements ConfirmedEnrollmentQuery {

  private final EnrollmentRepository enrollmentRepository;

  public EnrollmentQueryAdapter(EnrollmentRepository enrollmentRepository) {
    this.enrollmentRepository = enrollmentRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public int confirmedCount(Long cohortId) {
    return enrollmentRepository.countByCohortIdAndStatus(cohortId, EnrollmentStatus.CONFIRMED);
  }
}
