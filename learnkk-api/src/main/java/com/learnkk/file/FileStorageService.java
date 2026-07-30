package com.learnkk.file;

import com.learnkk.common.exception.FileConstraintViolationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 서비스 골격 (business-logic-model §7, business-rules §5).
 *
 * <p>U1 은 계약·검증만 확립한다. 실제 업로드 사용처(증빙 U4, 보고서/증서 U5)는 후속 유닛에서 호출한다.
 *
 * <ul>
 *   <li>store — MIME/크기/확장자 검증(R-U1-22/23) + 서버 UUID 파일명(R-U1-24) + 웹루트 밖 저장(R-U1-21)
 *   <li>load — 경로 이탈 방지(canonical path 검증) 후 Resource 반환
 *   <li>delete — 경로 이탈 방지 후 삭제, 대상 없으면 no-op(멱등, U4/U5 보상용)
 * </ul>
 */
@Service
public class FileStorageService {

  private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

  /** 허용 MIME (R-U1-22). */
  static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "application/pdf");

  /** 허용 확장자 (R-U1-22/24). */
  static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");

  /** 최대 크기 10MB (R-U1-23). */
  static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

  private final Path root;

  public FileStorageService(FileStorageProperties properties) {
    this.root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
  }

  /** 파일 저장 — 검증 통과 시 저장 루트 기준 상대 경로(메타)를 반환한다. */
  public String store(MultipartFile file) {
    validate(file);
    String extension = extensionOf(file.getOriginalFilename());
    String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
    try {
      Files.createDirectories(root);
      Path target = root.resolve(storedName).normalize();
      ensureWithinRoot(target);
      file.transferTo(target.toFile());
      return storedName;
    } catch (IOException e) {
      throw new UncheckedIOException("파일 저장에 실패했습니다", e);
    }
  }

  /** 저장 파일 로드 — 경로 이탈 방지 후 Resource 반환. */
  public Resource load(String storedName) {
    Path target = resolveSafely(storedName);
    if (!Files.exists(target)) {
      throw new FileConstraintViolationException("파일을 찾을 수 없습니다");
    }
    try {
      return new UrlResource(target.toUri());
    } catch (IOException e) {
      throw new UncheckedIOException("파일 로드에 실패했습니다", e);
    }
  }

  /** 저장 파일 삭제 — 멱등(대상 없으면 no-op). 상위 유닛 트랜잭션 롤백 보상용. */
  public void delete(String storedName) {
    Path target = resolveSafely(storedName);
    try {
      Files.deleteIfExists(target);
    } catch (IOException e) {
      // 삭제 실패는 예외를 던지되 호출측이 로깅 후 진행 가능(business-logic-model §7)
      log.warn("파일 삭제 실패: {}", storedName, e);
      throw new UncheckedIOException("파일 삭제에 실패했습니다", e);
    }
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new FileConstraintViolationException("빈 파일은 허용되지 않습니다");
    }
    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new FileConstraintViolationException("파일 크기는 최대 10MB 입니다");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
      throw new FileConstraintViolationException("허용되지 않는 파일 형식입니다 (jpg/png/pdf)");
    }
    String extension = extensionOf(file.getOriginalFilename());
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new FileConstraintViolationException("허용되지 않는 확장자입니다 (jpg/png/pdf)");
    }
  }

  private Path resolveSafely(String storedName) {
    if (storedName == null || storedName.isBlank()) {
      throw new FileConstraintViolationException("잘못된 파일 경로입니다");
    }
    Path target = root.resolve(storedName).normalize();
    ensureWithinRoot(target);
    return target;
  }

  private void ensureWithinRoot(Path target) {
    // 경로 이탈(path traversal) 방지 — 저장 루트 하위인지 검증(security-design.md §6)
    if (!target.startsWith(root)) {
      throw new FileConstraintViolationException("허용되지 않는 파일 경로입니다");
    }
  }

  private String extensionOf(String filename) {
    if (filename == null) {
      return "";
    }
    int dot = filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) {
      return "";
    }
    return filename.substring(dot + 1).toLowerCase();
  }
}
