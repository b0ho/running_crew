package com.learnkk.seed;

import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 부트스트랩 시더 (business-rules §6, R-U1-25~27).
 *
 * <p>Flyway 마이그레이션 이후 실행되는 Spring {@link ApplicationRunner}. 규칙:
 *
 * <ul>
 *   <li>멱등 — 관리자 email 존재 시 no-op(R-U1-25)
 *   <li>env 주입 비밀번호 BCrypt 해싱 후 isAdmin=true 삽입(R-U1-26)
 *   <li>ADMIN_EMAIL/ADMIN_PASSWORD 미설정 시 부팅 중단(fail-fast, R-U1-27)
 * </ul>
 */
@Component
public class AdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

  private final AdminSeedProperties properties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminSeeder(
      AdminSeedProperties properties,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    seed();
  }

  /** 시드 로직(테스트에서 직접 호출 가능). */
  @Transactional
  public void seed() {
    String email = properties.getEmail() == null ? "" : properties.getEmail().trim().toLowerCase();
    String password = properties.getPassword() == null ? "" : properties.getPassword();

    // R-U1-27 — 필수 env 미설정 시 명시적 실패(조용한 스킵 금지)
    if (email.isEmpty() || password.isEmpty()) {
      throw new IllegalStateException("관리자 시드 실패: ADMIN_EMAIL 과 ADMIN_PASSWORD 환경변수를 모두 설정해야 합니다");
    }

    // R-U1-25 — 멱등
    if (userRepository.existsByEmail(email)) {
      log.info("관리자 계정이 이미 존재합니다 — 시드 no-op");
      return;
    }

    // R-U1-26 — env 비밀번호 BCrypt 해싱 후 isAdmin=true 삽입
    String hash = passwordEncoder.encode(password);
    User admin = User.newAdmin(email, "관리자", "admin", hash);
    userRepository.save(admin);
    log.info("관리자 계정 시드 완료: {}", email);
  }
}
