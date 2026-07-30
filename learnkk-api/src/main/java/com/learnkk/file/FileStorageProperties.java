package com.learnkk.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 파일 저장 설정 (app.storage.*). */
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {

  /** 업로드 루트 — 웹루트 밖 볼륨(R-U1-21). */
  private String uploadDir = "./var/uploads";

  public String getUploadDir() {
    return uploadDir;
  }

  public void setUploadDir(String uploadDir) {
    this.uploadDir = uploadDir;
  }
}
