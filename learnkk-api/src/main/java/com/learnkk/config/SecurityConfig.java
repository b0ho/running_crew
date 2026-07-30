package com.learnkk.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정 (business-rules §3, security-design.md §1/§2).
 *
 * <ul>
 *   <li>BCryptPasswordEncoder — cost 는 {@code security.bcrypt.cost}(기본 10, 하한 8, NFR-SEC-1)
 *   <li>세션 인증 + {@code sessionFixation().changeSessionId()} (NFR-SEC-3)
 *   <li>{@code @EnableMethodSecurity} — 관리자 인가는 메서드 레벨 @PreAuthorize (R-U1-16a)
 *   <li>permitAll: /api/auth/**, springdoc, /actuator/health · 그 외 authenticated
 *   <li>CORS: FE 오리진 + allowCredentials(세션 쿠키)
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final int MIN_BCRYPT_COST = 8;

  private final int bcryptCost;
  private final List<String> allowedOrigins;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      @Value("${security.bcrypt.cost:10}") int bcryptCost,
      @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler) {
    this.bcryptCost = bcryptCost;
    this.allowedOrigins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    if (bcryptCost < MIN_BCRYPT_COST) {
      throw new IllegalStateException(
          "security.bcrypt.cost 는 최소 " + MIN_BCRYPT_COST + " 이상이어야 합니다 (현재 " + bcryptCost + ")");
    }
    return new BCryptPasswordEncoder(bcryptCost);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // 세션 쿠키 기반 REST API — CSRF 는 파일럿에서 비활성(SameSite=Lax + 세션 쿠키 정책으로 완화).
        // 확장 시 CSRF 토큰 도입 검토(security-design.md §3).
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    // NFR-SEC-3 — 로그인 성공 시 세션 ID 재발급(세션 고정 방지)
                    .sessionFixation(fixation -> fixation.changeSessionId()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
