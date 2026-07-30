package com.learnkk.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** AdminSeeder 단위 테스트 — 멱등·fail-fast·BCrypt·isAdmin 검증(R-U1-25~27). */
@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private AdminSeeder seeder(String email, String password) {
    AdminSeedProperties props = new AdminSeedProperties();
    props.setEmail(email);
    props.setPassword(password);
    return new AdminSeeder(props, userRepository, passwordEncoder);
  }

  @Test
  void env_미설정이면_부팅_중단() {
    AdminSeeder seeder = seeder("", "");

    assertThatThrownBy(seeder::seed).isInstanceOf(IllegalStateException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void password_누락이면_부팅_중단() {
    AdminSeeder seeder = seeder("admin@learnkk.local", "");

    assertThatThrownBy(seeder::seed).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void 이미_존재하면_no_op_멱등() {
    when(userRepository.existsByEmail("admin@learnkk.local")).thenReturn(true);
    AdminSeeder seeder = seeder("Admin@Learnkk.local", "adminpass");

    seeder.seed();

    verify(userRepository, never()).save(any());
  }

  @Test
  void 미존재시_BCrypt_해싱_isAdmin_true_삽입() {
    when(userRepository.existsByEmail("admin@learnkk.local")).thenReturn(false);
    when(passwordEncoder.encode("adminpass")).thenReturn("$2a$10$adminhash");

    AdminSeeder seeder = seeder("Admin@Learnkk.local", "adminpass");
    seeder.seed();

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.isAdmin()).isTrue();
    assertThat(saved.getEmail()).isEqualTo("admin@learnkk.local");
    assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$adminhash");
  }
}
