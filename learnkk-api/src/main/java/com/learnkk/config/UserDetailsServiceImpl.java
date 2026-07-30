package com.learnkk.config;

import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * email 로 사용자를 조회해 Spring Security UserDetails 로 변환한다.
 *
 * <p>역할 매핑(security-design.md §2): 인증 사용자 → ROLE_USER, isAdmin==true → ROLE_ADMIN 추가.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String normalized = username == null ? "" : username.trim().toLowerCase();
    User user =
        userRepository
            .findByEmail(normalized)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다"));

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (user.isAdmin()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .build();
  }
}
