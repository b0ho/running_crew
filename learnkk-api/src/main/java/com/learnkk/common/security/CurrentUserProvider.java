package com.learnkk.common.security;

import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 현재 인증 사용자 해석기 (security-design.md §1, 신뢰 경계).
 *
 * <p>세션 쿠키 인증의 principal name 은 email 이다. 현재 사용자 id 는 요청 바디가 아니라 SecurityContext 의 email 을
 * UserRepository 로 조회해 해석한다(요청 바디로 mentorId 를 받지 않음). 미인증이거나 사용자를 찾지 못하면 {@link
 * InsufficientAuthenticationException}(→ 401 UNAUTHORIZED, GlobalExceptionHandler)를 던진다.
 */
@Component
public class CurrentUserProvider {

  private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";

  private final UserRepository userRepository;

  public CurrentUserProvider(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** 현재 인증 사용자 엔티티. 미인증/미존재면 401. */
  public User currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || ANONYMOUS_PRINCIPAL.equals(authentication.getPrincipal())) {
      throw new InsufficientAuthenticationException("인증이 필요합니다");
    }
    String email = authentication.getName();
    return userRepository
        .findByEmail(email == null ? "" : email.trim().toLowerCase())
        .orElseThrow(() -> new InsufficientAuthenticationException("인증이 필요합니다"));
  }

  /** 현재 인증 사용자 id. */
  public Long currentUserId() {
    return currentUser().getId();
  }

  /** 현재 인증 사용자가 관리자인지 여부(종료됨 코호트 조회 권한 등에 사용). */
  public boolean isCurrentUserAdmin() {
    return currentUser().isAdmin();
  }
}
