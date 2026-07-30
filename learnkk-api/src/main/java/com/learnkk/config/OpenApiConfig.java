package com.learnkk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** springdoc-openapi 기본 메타 (team.md Mandated — OpenAPI 자동 생성). */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI learnkkOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("LearnKK API")
                .version("0.0.1")
                .description("LearnKK 파일럿 — U1 foundation (인증·RBAC 골격)"));
  }
}
