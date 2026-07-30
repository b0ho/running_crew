package com.learnkk.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link EndDateAfterStartDate} 검증기 (R-U2-04).
 *
 * <p>두 날짜 중 하나라도 null 이면 통과시킨다(개별 {@code @NotNull} 이 그 위반을 담당). 둘 다 있으면 endDate >= startDate 를
 * 요구한다.
 */
public class EndDateAfterStartDateValidator
    implements ConstraintValidator<EndDateAfterStartDate, DateRange> {

  @Override
  public boolean isValid(DateRange value, ConstraintValidatorContext context) {
    if (value == null || value.startDate() == null || value.endDate() == null) {
      return true;
    }
    return !value.endDate().isBefore(value.startDate());
  }
}
