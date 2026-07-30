package com.learnkk.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 종료일 >= 시작일 클래스 레벨 교차 검증 (R-U2-04).
 *
 * <p>{@link DateRange} 를 구현하는 요청 타입에 적용한다. 위반 시 400 VALIDATION_ERROR.
 */
@Documented
@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EndDateAfterStartDate {

  String message() default "종료일은 시작일과 같거나 이후여야 합니다";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
