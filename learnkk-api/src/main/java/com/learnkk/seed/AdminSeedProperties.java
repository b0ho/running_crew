package com.learnkk.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 관리자 시드 설정 (app.admin.*, 환경변수 ADMIN_EMAIL/ADMIN_PASSWORD 주입). */
@ConfigurationProperties(prefix = "app.admin")
public class AdminSeedProperties {

  private String email = "";
  private String password = "";

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
