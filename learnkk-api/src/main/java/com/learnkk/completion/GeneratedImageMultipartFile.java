package com.learnkk.completion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * 서버 생성 이미지(수료증 PNG)를 U1 {@code FileStorageService.store}에 그대로 전달하기 위한 인메모리 MultipartFile 어댑터.
 *
 * <p>수료증 이미지는 사용자 업로드가 아닌 서버 생성 신뢰 파일이지만(security-design.md §3), U1 FileStorageService 를 재사용해 웹루트
 * 밖·서버 UUID 파일명·경로 이탈 방지 규약을 동일하게 적용받기 위해 MultipartFile 계약으로 감싼다. content-type/확장자는 PNG 로 고정되어 U1
 * store 의 화이트리스트 검증(image/png, png)을 통과한다.
 */
final class GeneratedImageMultipartFile implements MultipartFile {

  private final byte[] content;
  private final String filename;
  private final String contentType;

  GeneratedImageMultipartFile(byte[] content, String filename, String contentType) {
    this.content = content;
    this.filename = filename;
    this.contentType = contentType;
  }

  @Override
  public String getName() {
    return "file";
  }

  @Override
  public String getOriginalFilename() {
    return filename;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean isEmpty() {
    return content.length == 0;
  }

  @Override
  public long getSize() {
    return content.length;
  }

  @Override
  public byte[] getBytes() {
    return content.clone();
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(content);
  }

  @Override
  public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
    Files.write(dest.toPath(), content);
  }

  @Override
  public void transferTo(Path dest) throws IOException, IllegalStateException {
    Files.write(dest, content);
  }
}
