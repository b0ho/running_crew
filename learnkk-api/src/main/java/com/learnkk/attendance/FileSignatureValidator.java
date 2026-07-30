package com.learnkk.attendance;

import com.learnkk.common.exception.FileConstraintViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 시그니처(매직 바이트) 검증기 (security-design.md §2, R-U4-02, INV-U4-3).
 *
 * <p>선언 MIME(Content-Type 헤더)은 클라이언트가 위조 가능하므로 신뢰하지 않고, 파일 앞부분의 매직 바이트로 실제 형식을 확인한 뒤 선언 MIME 과 교차
 * 검증한다. 이 검증은 {@code AttendanceService.uploadEvidence} 에서 {@code FileStorageService.store} 호출
 * **전**에 수행하는 U4 특화 강화 규칙이다(검증 책임 경계 — U1 store 는 기본 검증만 담당).
 *
 * <ul>
 *   <li>JPEG — {@code FF D8 FF}
 *   <li>PNG — {@code 89 50 4E 47}
 *   <li>PDF — {@code 25 50 44 46}
 * </ul>
 *
 * 불일치 시 {@link FileConstraintViolationException}(→ 400 FILE_CONSTRAINT_VIOLATION, U1 핸들러 재사용).
 */
@Component
public class FileSignatureValidator {

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46};

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

  /** 업로드 파일의 형식·크기를 저장 전에 검증한다. 빈 파일·크기 초과·형식 불일치(매직바이트 또는 선언 MIME 교차 불일치) 시 예외를 던진다. */
  public void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new FileConstraintViolationException("빈 파일은 허용되지 않습니다");
    }
    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new FileConstraintViolationException("파일 크기는 최대 10MB 입니다");
    }

    String actualMime = detectByMagicBytes(readHeader(file));
    if (actualMime == null) {
      throw new FileConstraintViolationException("허용되지 않는 파일 형식입니다 (jpg/png/pdf)");
    }

    // 선언 MIME 과 실제 시그니처 교차 검증(방어 심층화). 선언 MIME 이 없거나 실제와 다르면 거부.
    String declared = file.getContentType();
    if (declared == null || !declared.equalsIgnoreCase(actualMime)) {
      throw new FileConstraintViolationException("파일 형식이 선언된 형식과 일치하지 않습니다");
    }
  }

  private byte[] readHeader(MultipartFile file) {
    // 전체 메모리 로딩 없이 앞부분만 읽는다(performance-design.md §2 스트리밍 정합).
    byte[] header = new byte[8];
    try (InputStream in = file.getInputStream()) {
      int read = in.readNBytes(header, 0, header.length);
      if (read <= 0) {
        throw new FileConstraintViolationException("빈 파일은 허용되지 않습니다");
      }
      return Arrays.copyOf(header, read);
    } catch (IOException e) {
      throw new FileConstraintViolationException("파일을 읽을 수 없습니다");
    }
  }

  private String detectByMagicBytes(byte[] header) {
    if (startsWith(header, JPEG)) {
      return "image/jpeg";
    }
    if (startsWith(header, PNG)) {
      return "image/png";
    }
    if (startsWith(header, PDF)) {
      return "application/pdf";
    }
    return null;
  }

  private boolean startsWith(byte[] data, byte[] prefix) {
    if (data.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (data[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
