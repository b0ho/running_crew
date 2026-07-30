package com.learnkk.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers 실 PostgreSQL 기반 통합 테스트 베이스.
 *
 * <p>@Tag("integration") — Docker(-in-docker) 미가용 CI 에서는 -PexcludeIntegration 로 제외한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
public abstract class IntegrationTestBase {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("learnkk")
          .withUsername("learnkk")
          .withPassword("learnkk");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // 시드 관리자 계정(부팅 시 필요, R-U1-27)
    registry.add("app.admin.email", () -> "admin@learnkk.local");
    registry.add("app.admin.password", () -> "adminpass123");
    // 테스트 속도를 위해 BCrypt cost 하한(8)
    registry.add("security.bcrypt.cost", () -> "8");
  }
}
