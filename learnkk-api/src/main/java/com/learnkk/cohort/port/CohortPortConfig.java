package com.learnkk.cohort.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * U2 크로스유닛 포트 기본 구현 등록 (business-logic-model.md §8).
 *
 * <p>{@link ConfirmedEnrollmentQuery} 의 파일럿 기본 구현(항상 0 반환)을 {@code @ConditionalOnMissingBean} 으로
 * 등록한다. U3(enrollment)가 실제 {@link ConfirmedEnrollmentQuery} 빈을 제공하면 이 기본 구현은 자동으로 대체된다.
 */
@Configuration
public class CohortPortConfig {

  @Bean
  @ConditionalOnMissingBean(ConfirmedEnrollmentQuery.class)
  public ConfirmedEnrollmentQuery defaultConfirmedEnrollmentQuery() {
    // U3 미빌드 시: 확정 인원 0 → 정원 축소가 확정 인원 검증에 걸리지 않음(파일럿 기본값).
    return cohortId -> 0;
  }
}
