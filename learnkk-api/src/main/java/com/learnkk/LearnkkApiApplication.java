package com.learnkk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** LearnKK 백엔드 진입점. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LearnkkApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(LearnkkApiApplication.class, args);
  }
}
