package com.jk.paper_trading_dashboard.shared.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(AlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> handleAlreadyExists(AlreadyExistsException exception,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(
      ObjectOptimisticLockingFailureException exception,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, "Resource was updated by another request; refresh and try again", request);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException exception,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
      HttpServletRequest request) {
    String message = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(this::formatFieldError)
        .collect(Collectors.joining("; "));

    if (message.isBlank()) {
      message = "Validation failed";
    }

    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
    ApiErrorResponse response = new ApiErrorResponse(
        message,
        request.getRequestURI(),
        Instant.now());

    return ResponseEntity.status(status).body(response);
  }

  private String formatFieldError(FieldError error) {
    return error.getField() + ": " + error.getDefaultMessage();
  }
}
