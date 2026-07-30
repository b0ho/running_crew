package com.learnkk.auth;

import com.learnkk.auth.dto.LoginRequest;
import com.learnkk.auth.dto.SignupRequest;
import com.learnkk.auth.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API (frontend-components §3, business-logic-model §2/§3).
 *
 * <ul>
 *   <li>POST /api/auth/signup → 201 UserDto
 *   <li>POST /api/auth/login → 200 UserDto + 세션 쿠키(세션 재발급)
 *   <li>GET /api/auth/me → 200 UserDto / 401
 *   <li>POST /api/auth/logout → 204 (세션 무효화)
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public ResponseEntity<UserDto> signup(@Valid @RequestBody SignupRequest request) {
    UserDto created = authService.signup(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PostMapping("/login")
  public ResponseEntity<UserDto> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication authentication = authService.authenticate(request.email(), request.password());

    // NFR-SEC-3 — 세션 고정 방지: 인증 성공 시 세션 ID 재발급
    HttpSession existing = httpRequest.getSession(false);
    if (existing != null) {
      httpRequest.changeSessionId();
    } else {
      httpRequest.getSession(true);
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);

    return ResponseEntity.ok(authService.currentUser(authentication.getName()));
  }

  @GetMapping("/me")
  public ResponseEntity<UserDto> me() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      throw new InsufficientAuthenticationException("인증이 필요합니다");
    }
    return ResponseEntity.ok(authService.currentUser(authentication.getName()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }
}
