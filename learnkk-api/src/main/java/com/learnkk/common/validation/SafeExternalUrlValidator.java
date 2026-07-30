package com.learnkk.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.util.Set;

/**
 * {@link SafeExternalUrl} 검증기 (security-design.md §3).
 *
 * <ol>
 *   <li>null·빈 문자열 허용(선택 필드).
 *   <li>{@code java.net.URI} 파싱(파싱 예외 시 invalid).
 *   <li>scheme 을 소문자화해 화이트리스트 {@code {http, https}} 포함 여부 확인.
 *   <li>host 존재(절대 URL) 확인.
 * </ol>
 *
 * <p>별도 URL 라이브러리 없이 표준 {@code java.net.URI} 로 구현해 의존성을 최소화한다.
 */
public class SafeExternalUrlValidator implements ConstraintValidator<SafeExternalUrl, String> {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true; // 선택 필드
    }
    try {
      URI uri = URI.create(value.trim());
      String scheme = uri.getScheme();
      return scheme != null
          && ALLOWED_SCHEMES.contains(scheme.toLowerCase())
          && uri.getHost() != null;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
