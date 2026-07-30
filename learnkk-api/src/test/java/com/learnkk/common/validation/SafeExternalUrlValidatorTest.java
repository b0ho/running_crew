package com.learnkk.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SafeExternalUrlValidator 단위 테스트 — 스킴 화이트리스트(security-design.md §3, R-U2-17). */
class SafeExternalUrlValidatorTest {

  private final SafeExternalUrlValidator validator = new SafeExternalUrlValidator();

  private boolean valid(String value) {
    return validator.isValid(value, null);
  }

  @Test
  void http_https_는_허용() {
    assertThat(valid("http://example.com/room")).isTrue();
    assertThat(valid("https://meet.example.com/abc")).isTrue();
    assertThat(valid("HTTPS://Meet.Example.com")).isTrue();
  }

  @Test
  void null_또는_blank_는_선택필드로_허용() {
    assertThat(valid(null)).isTrue();
    assertThat(valid("")).isTrue();
    assertThat(valid("   ")).isTrue();
  }

  @Test
  void 위험_스킴과_상대URL_은_거부() {
    assertThat(valid("javascript:alert(1)")).isFalse();
    assertThat(valid("data:text/html;base64,PHN2Zz4=")).isFalse();
    assertThat(valid("file:///etc/passwd")).isFalse();
    // 상대 URL — host 없음
    assertThat(valid("/relative/path")).isFalse();
    assertThat(valid("ftp://example.com")).isFalse();
  }
}
