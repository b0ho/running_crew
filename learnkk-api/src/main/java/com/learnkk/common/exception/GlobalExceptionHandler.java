package com.learnkk.common.exception;

import com.learnkk.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러 (business-rules §4 / §4.1, R-U1-17a~17i).
 *
 * <p>서비스 레이어가 던진 도메인 예외를 공통 에러 DTO(code·message·timestamp·path)로 정규화한다. 내부 상세는 클라이언트에 노출하지
 * 않는다(R-U1-19).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // R-U1-17a — Bean Validation
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining(", "));
    return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message, req);
  }

  // R-U1-17a — 도메인 검증
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      ValidationException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, ex.getMessage(), req);
  }

  // R-U1-17b — email 중복
  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateEmail(
      DuplicateEmailException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_EMAIL, ex.getMessage(), req);
  }

  // R-U1-17c — email UNIQUE 위반(동시 가입 경쟁의 최종 방어선)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다", req);
  }

  // R-U1-17d — 로그인 실패(미존재/불일치 동일)
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), req);
  }

  // R-U1-17e — 미인증 세션
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(
      AuthenticationException ex, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "인증이 필요합니다", req);
  }

  // R-U1-17f — 권한 부족
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "접근 권한이 없습니다", req);
  }

  // R-U1-17g — 미존재
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      EntityNotFoundException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, ex.getMessage(), req);
  }

  // R-U1-17h — 파일 제약 위반
  @ExceptionHandler(FileConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleFileConstraint(
      FileConstraintViolationException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ErrorCode.FILE_CONSTRAINT_VIOLATION, ex.getMessage(), req);
  }

  // R-U1-17i — 그 외 미처리 예외 (내부 상세 비노출)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
    log.error("처리되지 않은 예외", ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "요청을 처리하는 중 오류가 발생했습니다", req);
  }

  private String formatFieldError(FieldError fieldError) {
    return fieldError.getField() + ": " + fieldError.getDefaultMessage();
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String code, String message, HttpServletRequest req) {
    return ResponseEntity.status(status).body(ErrorResponse.of(code, message, req.getRequestURI()));
  }
}
