package com.learnkk.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learnkk.common.exception.FileConstraintViolationException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** FileStorageService 골격 테스트 — 제약 검증·경로 이탈 방지·멱등 삭제(R-U1-21~24). */
class FileStorageServiceTest {

  private FileStorageService service;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    FileStorageProperties props = new FileStorageProperties();
    props.setUploadDir(tempDir.resolve("uploads").toString());
    service = new FileStorageService(props);
  }

  @Test
  void 허용된_이미지_저장시_서버_UUID_파일명_반환() {
    MockMultipartFile file =
        new MockMultipartFile("file", "evidence.png", "image/png", new byte[] {1, 2, 3});

    String stored = service.store(file);

    // R-U1-24 — 원본 파일명이 아닌 서버 생성 UUID 파일명
    assertThat(stored).isNotEqualTo("evidence.png").endsWith(".png");
  }

  @Test
  void 허용되지_않는_MIME이면_거부() {
    MockMultipartFile file =
        new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[] {1});

    assertThatThrownBy(() -> service.store(file))
        .isInstanceOf(FileConstraintViolationException.class);
  }

  @Test
  void 크기_초과시_거부() {
    byte[] big = new byte[(int) FileStorageService.MAX_FILE_SIZE_BYTES + 1];
    MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", big);

    assertThatThrownBy(() -> service.store(file))
        .isInstanceOf(FileConstraintViolationException.class);
  }

  @Test
  void 저장후_load_가능() {
    MockMultipartFile file =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {9, 9});
    String stored = service.store(file);

    assertThat(service.load(stored).exists()).isTrue();
  }

  @Test
  void 경로_이탈_시도는_거부() {
    assertThatThrownBy(() -> service.load("../../etc/passwd"))
        .isInstanceOf(FileConstraintViolationException.class);
  }

  @Test
  void delete_는_멱등_대상_없어도_예외없음() {
    assertThatCode(() -> service.delete("nonexistent-uuid.pdf")).doesNotThrowAnyException();
  }

  @Test
  void 저장_파일_삭제() {
    MockMultipartFile file =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {9, 9});
    String stored = service.store(file);

    service.delete(stored);

    assertThatThrownBy(() -> service.load(stored))
        .isInstanceOf(FileConstraintViolationException.class);
  }
}
