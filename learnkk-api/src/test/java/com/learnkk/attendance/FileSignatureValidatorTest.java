package com.learnkk.attendance;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learnkk.common.exception.FileConstraintViolationException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** FileSignatureValidator 단위 테스트 — 매직바이트 검증(security-design.md §2, R-U4-02). */
class FileSignatureValidatorTest {

  private final FileSignatureValidator validator = new FileSignatureValidator();

  private static byte[] withHeader(byte[] header, int totalLen) {
    byte[] data = new byte[Math.max(header.length, totalLen)];
    System.arraycopy(header, 0, data, 0, header.length);
    return data;
  }

  @Test
  void jpeg_매직바이트와_선언MIME_일치시_통과() {
    byte[] jpeg = withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 16);
    MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", jpeg);

    assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
  }

  @Test
  void png_매직바이트와_선언MIME_일치시_통과() {
    byte[] png = withHeader(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}, 16);
    MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png);

    assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
  }

  @Test
  void pdf_매직바이트와_선언MIME_일치시_통과() {
    byte[] pdf = withHeader(new byte[] {0x25, 0x50, 0x44, 0x46}, 16);
    MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf);

    assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
  }

  @Test
  void 확장자만_pdf인_텍스트파일은_매직바이트_불일치로_거부() {
    byte[] text = "이것은 PDF 가 아닙니다".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", text);

    assertThatThrownBy(() -> validator.validate(file))
        .isInstanceOf(FileConstraintViolationException.class);
  }

  @Test
  void 매직바이트는_jpeg지만_선언MIME이_pdf면_교차검증_실패로_거부() {
    byte[] jpeg = withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 16);
    MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", jpeg);

    assertThatThrownBy(() -> validator.validate(file))
        .isInstanceOf(FileConstraintViolationException.class);
  }

  @Test
  void 크기_10MB_초과는_거부() {
    byte[] tooBig =
        withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 10 * 1024 * 1024 + 1);
    MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", tooBig);

    assertThatThrownBy(() -> validator.validate(file))
        .isInstanceOf(FileConstraintViolationException.class);
  }

  @Test
  void 빈_파일은_거부() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

    assertThatThrownBy(() -> validator.validate(file))
        .isInstanceOf(FileConstraintViolationException.class);
  }
}
