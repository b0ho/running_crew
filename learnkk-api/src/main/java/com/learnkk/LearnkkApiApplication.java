package com.learnkk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** LearnKK 백엔드 진입점. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LearnkkApiApplication {

  public static void main(String[] args) {
    // Java2D 수료증 렌더링(U5)은 헤드리스 환경에서 동작해야 한다(mandatory convention).
    System.setProperty("java.awt.headless", "true");
    SpringApplication.run(LearnkkApiApplication.class, args);
  }
}
