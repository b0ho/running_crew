package com.learnkk.admin;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 스텁 (R-U1-16a, 워킹 스켈레톤 관통 검증용).
 *
 * <p>관리자 인가는 필터체인 URL 열거가 아니라 메서드 레벨 {@code @PreAuthorize("hasRole('ADMIN')")} 로 강제한다. 후속
 * 유닛(U3/U6)은 자기 컨트롤러 메서드에 동일 애너테이션을 붙이는 것만으로 인가가 걸린다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

  /** 관리자 권한(ROLE_ADMIN) 보유자만 200. 그 외 403 FORBIDDEN(RestAccessDeniedHandler). */
  @GetMapping("/ping")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> ping() {
    return ResponseEntity.ok(Map.of("status", "ok", "scope", "admin"));
  }
}
