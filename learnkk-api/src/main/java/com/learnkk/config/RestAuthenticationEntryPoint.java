package com.learnkk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.common.dto.ErrorResponse;
import com.learnkk.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 미인증 접근(R-U1-13/17e)을 공통 에러 DTO 로 정규화하는 진입점.
 *
 * <p>보안 필터체인은 DispatcherServlet 이전에 동작하므로 @RestControllerAdvice 로 잡히지 않는다. 401 UNAUTHORIZED 를 여기서
 * 직접 직렬화한다.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    ErrorResponse body =
        ErrorResponse.of(ErrorCode.UNAUTHORIZED, "인증이 필요합니다", request.getRequestURI());
    objectMapper.writeValue(response.getWriter(), body);
  }
}
