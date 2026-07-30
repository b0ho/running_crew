package com.learnkk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.common.dto.ErrorResponse;
import com.learnkk.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 권한 부족(R-U1-12/17f)을 공통 에러 DTO 로 정규화하는 핸들러. 403 FORBIDDEN 을 직접 직렬화한다. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    ErrorResponse body =
        ErrorResponse.of(ErrorCode.FORBIDDEN, "접근 권한이 없습니다", request.getRequestURI());
    objectMapper.writeValue(response.getWriter(), body);
  }
}
