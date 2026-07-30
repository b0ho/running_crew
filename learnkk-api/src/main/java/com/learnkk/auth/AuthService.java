package com.learnkk.auth;

import com.learnkk.auth.dto.SignupRequest;
import com.learnkk.auth.dto.UserDto;
import com.learnkk.common.exception.DuplicateEmailException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.InvalidCredentialsException;
import com.learnkk.common.exception.ValidationException;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 도메인 서비스 (business-logic-model §2/§3).
 *
 * <p>세션/HTTP 부수효과(쿠키·세션 재발급)는 AuthController 가 담당하고, 본 서비스는 순수 도메인 로직(가입·자격 검증·조회)만 담당해 단위 테스트가
 * 용이하도록 분리한다.
 */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 회원가입 (business-logic-model §2).
   *
   * <p>정규화(R-U1-02) → 검증 → 중복 조회 → BCrypt 해싱(R-U1-05) → isAdmin=false 저장(R-U1-06). email UNIQUE 위반은
   * GlobalExceptionHandler 가 409 로 매핑(R-U1-17c, 경쟁 안전).
   */
  @Transactional
  public UserDto signup(SignupRequest request) {
    String email = normalizeEmail(request.email());
    validatePassword(request.password());

    if (userRepository.existsByEmail(email)) {
      throw new DuplicateEmailException("이미 사용 중인 이메일입니다");
    }

    String passwordHash = passwordEncoder.encode(request.password());
    User user =
        User.newMember(email, request.name().trim(), request.nickname().trim(), passwordHash);
    User saved = userRepository.save(user);
    return UserDto.from(saved);
  }

  /**
   * 자격 검증 (business-logic-model §3).
   *
   * <p>미존재 계정과 비밀번호 불일치를 동일한 {@link InvalidCredentialsException} 으로 처리한다(R-U1-09, 사용자 열거 방지). 성공 시
   * 세션 수립에 사용할 Authentication 을 반환한다(세션 확립은 컨트롤러).
   */
  @Transactional(readOnly = true)
  public Authentication authenticate(String rawEmail, String rawPassword) {
    String email = normalizeEmail(rawEmail);
    User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (user.isAdmin()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    return UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null, authorities);
  }

  /** 현재 세션 사용자 조회 (GET /me). */
  @Transactional(readOnly = true)
  public UserDto currentUser(String email) {
    return userRepository
        .findByEmail(normalizeEmail(email))
        .map(UserDto::from)
        .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다"));
  }

  private String normalizeEmail(String email) {
    // R-U1-02 — 소문자 정규화(검증·조회·저장 전 항상 선적용)
    return email == null ? "" : email.trim().toLowerCase();
  }

  private void validatePassword(String password) {
    // R-U1-04 — 최소 8자, 공백만으로 구성 불가
    if (password == null || password.length() < 8) {
      throw new ValidationException("비밀번호는 최소 8자입니다");
    }
    if (password.trim().isEmpty()) {
      throw new ValidationException("비밀번호는 공백만으로 구성할 수 없습니다");
    }
  }
}
