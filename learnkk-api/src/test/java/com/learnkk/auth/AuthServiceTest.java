package com.learnkk.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.auth.dto.SignupRequest;
import com.learnkk.auth.dto.UserDto;
import com.learnkk.common.exception.DuplicateEmailException;
import com.learnkk.common.exception.InvalidCredentialsException;
import com.learnkk.common.exception.ValidationException;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

/** AuthService 단위 테스트 — signup/authenticate 도메인 규칙 검증. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @InjectMocks private AuthService authService;

  @Test
  void signup_정상_가입시_UserDto_반환하고_isAdmin_false() {
    when(userRepository.existsByEmail("user@learnkk.local")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedvalue");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    SignupRequest req = new SignupRequest("User@Learnkk.local", "홍길동", "gil", "password123");
    UserDto dto = authService.signup(req);

    assertThat(dto.isAdmin()).isFalse();
    // R-U1-02 — email 소문자 정규화
    assertThat(dto.email()).isEqualTo("user@learnkk.local");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.isAdmin()).isFalse();
    assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashedvalue");
    assertThat(saved.getEmail()).isEqualTo("user@learnkk.local");
  }

  @Test
  void signup_중복_email_이면_DuplicateEmailException() {
    when(userRepository.existsByEmail("dup@learnkk.local")).thenReturn(true);

    SignupRequest req = new SignupRequest("dup@learnkk.local", "홍길동", "gil", "password123");

    assertThatThrownBy(() -> authService.signup(req)).isInstanceOf(DuplicateEmailException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void signup_password_8자_미만이면_ValidationException() {
    SignupRequest req = new SignupRequest("short@learnkk.local", "홍길동", "gil", "short");

    assertThatThrownBy(() -> authService.signup(req)).isInstanceOf(ValidationException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void signup_password_공백만이면_ValidationException() {
    SignupRequest req = new SignupRequest("blank@learnkk.local", "홍길동", "gil", "        ");

    assertThatThrownBy(() -> authService.signup(req)).isInstanceOf(ValidationException.class);
  }

  @Test
  void authenticate_성공시_ROLE_USER_권한() {
    User user = User.newMember("user@learnkk.local", "홍길동", "gil", "$2a$10$hash");
    when(userRepository.findByEmail("user@learnkk.local")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);

    Authentication auth = authService.authenticate("User@Learnkk.local", "password123");

    assertThat(auth.isAuthenticated()).isTrue();
    assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_USER");
  }

  @Test
  void authenticate_관리자면_ROLE_ADMIN_추가() {
    User admin = User.newAdmin("admin@learnkk.local", "관리자", "admin", "$2a$10$hash");
    when(userRepository.findByEmail("admin@learnkk.local")).thenReturn(Optional.of(admin));
    when(passwordEncoder.matches("adminpass", "$2a$10$hash")).thenReturn(true);

    Authentication auth = authService.authenticate("admin@learnkk.local", "adminpass");

    assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void authenticate_미존재_계정이면_InvalidCredentials() {
    when(userRepository.findByEmail("missing@learnkk.local")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.authenticate("missing@learnkk.local", "password123"))
        .isInstanceOf(InvalidCredentialsException.class);
    // 미존재 계정은 비밀번호 비교조차 하지 않고 동일 예외
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void authenticate_비밀번호_불일치면_동일한_InvalidCredentials() {
    User user = User.newMember("user@learnkk.local", "홍길동", "gil", "$2a$10$hash");
    when(userRepository.findByEmail("user@learnkk.local")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.authenticate("user@learnkk.local", "wrong"))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
