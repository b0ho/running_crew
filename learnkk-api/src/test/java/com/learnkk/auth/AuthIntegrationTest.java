package com.learnkk.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.auth.dto.UserDto;
import com.learnkk.support.IntegrationTestBase;
import com.learnkk.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 인증 통합 테스트 (Testcontainers) — 가입→로그인→/me 관통, 보안 필터, 에러 매핑, email UNIQUE 경쟁, 시드 멱등/fail-fast.
 *
 * <p>워킹 스켈레톤 관통(business-logic-model §9): 가입 → 로그인(세션) → /me 200 → 관리자 로그인 → 관리자 스텁 200.
 */
class AuthIntegrationTest extends IntegrationTestBase {

  @Autowired private TestRestTemplate rest;
  @Autowired private UserRepository userRepository;

  private HttpHeaders json() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders withCookie(String cookie) {
    HttpHeaders headers = json();
    headers.add(HttpHeaders.COOKIE, cookie);
    return headers;
  }

  @Test
  void 워킹_스켈레톤_관통_가입_로그인_me_그리고_관리자_스텁() {
    // 1) 가입 → 201
    String signup =
        "{\"email\":\"alice@learnkk.local\",\"name\":\"앨리스\",\"nickname\":\"al\",\"password\":\"password123\"}";
    ResponseEntity<UserDto> signupRes =
        rest.postForEntity("/api/auth/signup", new HttpEntity<>(signup, json()), UserDto.class);
    assertThat(signupRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(signupRes.getBody()).isNotNull();
    assertThat(signupRes.getBody().isAdmin()).isFalse();

    // 2) 로그인 → 200 + 세션 쿠키
    String login = "{\"email\":\"alice@learnkk.local\",\"password\":\"password123\"}";
    ResponseEntity<UserDto> loginRes =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(login, json()), UserDto.class);
    assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    String sessionCookie = loginRes.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(sessionCookie).isNotNull().contains("JSESSIONID");

    // 3) /me → 200 (세션 쿠키 사용)
    ResponseEntity<UserDto> meRes =
        rest.exchange(
            "/api/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(withCookie(sessionCookie)),
            UserDto.class);
    assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meRes.getBody().email()).isEqualTo("alice@learnkk.local");

    // 4) 관리자 로그인 (시드 계정)
    String adminLogin = "{\"email\":\"admin@learnkk.local\",\"password\":\"adminpass123\"}";
    ResponseEntity<UserDto> adminRes =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(adminLogin, json()), UserDto.class);
    assertThat(adminRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(adminRes.getBody().isAdmin()).isTrue();
    String adminCookie = adminRes.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

    // 5) 관리자 스텁 → 200
    ResponseEntity<Map> adminPing =
        rest.exchange(
            "/api/admin/ping",
            HttpMethod.GET,
            new HttpEntity<>(withCookie(adminCookie)),
            Map.class);
    assertThat(adminPing.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void 미인증_보호경로_접근시_401() {
    ResponseEntity<Map> res = rest.getForEntity("/api/admin/ping", Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(res.getBody().get("code")).isEqualTo("UNAUTHORIZED");
  }

  @Test
  void 일반사용자_관리자스텁_접근시_403() {
    String signup =
        "{\"email\":\"bob@learnkk.local\",\"name\":\"밥\",\"nickname\":\"bob\",\"password\":\"password123\"}";
    rest.postForEntity("/api/auth/signup", new HttpEntity<>(signup, json()), UserDto.class);
    String login = "{\"email\":\"bob@learnkk.local\",\"password\":\"password123\"}";
    ResponseEntity<UserDto> loginRes =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(login, json()), UserDto.class);
    String cookie = loginRes.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

    ResponseEntity<Map> res =
        rest.exchange(
            "/api/admin/ping", HttpMethod.GET, new HttpEntity<>(withCookie(cookie)), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(res.getBody().get("code")).isEqualTo("FORBIDDEN");
  }

  @Test
  void 로그인_실패시_401_INVALID_CREDENTIALS() {
    String login = "{\"email\":\"nouser@learnkk.local\",\"password\":\"whatever1\"}";
    ResponseEntity<Map> res =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(login, json()), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(res.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  void 검증오류시_400_VALIDATION_ERROR() {
    String bad =
        "{\"email\":\"not-an-email\",\"name\":\"\",\"nickname\":\"x\",\"password\":\"short\"}";
    ResponseEntity<Map> res =
        rest.postForEntity("/api/auth/signup", new HttpEntity<>(bad, json()), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(res.getBody().get("code")).isEqualTo("VALIDATION_ERROR");
  }

  @Test
  void 중복_email_가입시_409_DUPLICATE_EMAIL() {
    String signup =
        "{\"email\":\"dup@learnkk.local\",\"name\":\"중복\",\"nickname\":\"dup\",\"password\":\"password123\"}";
    rest.postForEntity("/api/auth/signup", new HttpEntity<>(signup, json()), UserDto.class);

    // 대소문자만 다른 email 로 재가입 → 정규화 후 중복 → 409
    String signupUpper =
        "{\"email\":\"DUP@learnkk.local\",\"name\":\"중복2\",\"nickname\":\"dup2\",\"password\":\"password123\"}";
    ResponseEntity<Map> res =
        rest.postForEntity("/api/auth/signup", new HttpEntity<>(signupUpper, json()), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().get("code")).isEqualTo("DUPLICATE_EMAIL");
  }

  @Test
  void 시드_관리자는_멱등_생성되어_존재한다() {
    // 부팅 시 AdminSeeder 가 1회 삽입, 재기동/재실행에도 중복 생성되지 않음(멱등).
    assertThat(userRepository.existsByEmail("admin@learnkk.local")).isTrue();
    List<?> all = userRepository.findAll();
    long adminCount =
        all.stream().filter(u -> u instanceof com.learnkk.user.User user && user.isAdmin()).count();
    assertThat(adminCount).isEqualTo(1);
  }
}
