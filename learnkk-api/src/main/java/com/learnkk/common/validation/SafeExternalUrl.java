package com.learnkk.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 외부 링크 안전성 검증 (security-design.md §3, R-U2-17).
 *
 * <p>null/blank 는 허용(선택 필드). 값이 있으면 스킴 화이트리스트({@code http}, {@code https})만 통과하며 {@code
 * javascript:}·{@code data:}·{@code file:}·상대 URL 등은 거부한다. 위반 시 400 VALIDATION_ERROR.
 */
@Documented
@Constraint(validatedBy = SafeExternalUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeExternalUrl {

  String message() default "외부 링크는 http/https URL 형식이어야 합니다";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
