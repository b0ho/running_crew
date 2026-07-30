package com.learnkk.common.exception;

import com.learnkk.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
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

  // R-U2-21a — 종료됨 코호트 수정
  @ExceptionHandler(CohortClosedException.class)
  public ResponseEntity<ErrorResponse> handleCohortClosed(
      CohortClosedException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.COHORT_CLOSED, ex.getMessage(), req);
  }

  // R-U2-21b — 확정 인원 미만 정원 축소
  @ExceptionHandler(CapacityBelowConfirmedException.class)
  public ResponseEntity<ErrorResponse> handleCapacityBelowConfirmed(
      CapacityBelowConfirmedException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.CAPACITY_BELOW_CONFIRMED, ex.getMessage(), req);
  }

  // R-U2-21c — 인증 회차 절단 축소
  @ExceptionHandler(SessionVerifiedLockException.class)
  public ResponseEntity<ErrorResponse> handleSessionVerifiedLock(
      SessionVerifiedLockException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.SESSION_VERIFIED_LOCK, ex.getMessage(), req);
  }

  // R-U2-21d — 허용되지 않은 상태 전이(가드 UPDATE 영향 행 0 포함)
  @ExceptionHandler(InvalidStateTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
      InvalidStateTransitionException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION, ex.getMessage(), req);
  }

  // R-U3-21a/21b — 이미 신청/동시 이중 제출(UNIQUE 위반은 서비스에서 이 예외로 변환)
  @ExceptionHandler(AlreadyEnrolledException.class)
  public ResponseEntity<ErrorResponse> handleAlreadyEnrolled(
      AlreadyEnrolledException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.ALREADY_ENROLLED, ex.getMessage(), req);
  }

  // R-U3-21c — 자기 개설 코호트 참여 시도
  @ExceptionHandler(SelfEnrollmentException.class)
  public ResponseEntity<ErrorResponse> handleSelfEnrollment(
      SelfEnrollmentException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.SELF_ENROLLMENT, ex.getMessage(), req);
  }

  // R-U3-21d — 종료됨 코호트 신규 참여
  @ExceptionHandler(CohortNotOpenException.class)
  public ResponseEntity<ErrorResponse> handleCohortNotOpen(
      CohortNotOpenException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.COHORT_NOT_OPEN, ex.getMessage(), req);
  }

  // performance-design.md §2 — 참여 신청 락 경합 타임아웃
  @ExceptionHandler(EnrollmentBusyException.class)
  public ResponseEntity<ErrorResponse> handleEnrollmentBusy(
      EnrollmentBusyException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.ENROLLMENT_BUSY, ex.getMessage(), req);
  }

  // performance-design.md §2 — 비관적 락 획득 실패(락 타임아웃)의 안전망.
  // 서비스가 EnrollmentBusyException 으로 변환하지 못하고 전파된 경우에도 409 ENROLLMENT_BUSY 로 매핑한다.
  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handlePessimisticLock(
      PessimisticLockingFailureException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ErrorCode.ENROLLMENT_BUSY, "신청이 몰려 잠시 후 다시 시도해 주세요", req);
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
