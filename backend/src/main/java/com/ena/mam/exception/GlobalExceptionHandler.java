package com.ena.mam.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest; // <-- Added import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 400 | IP: {} | Method: {} | URI: {} | Error: Validation Failed",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errors.put("_general", error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 404 | IP: {} | Method: {} | URI: {} | Error: {}",
                getClientIp(request), request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 409 | IP: {} | Method: {} | URI: {} | Error: Duplicate Resource",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("AUDIT_FAIL | Status: 500 | IP: {} | Method: {} | URI: {} | Error: Unexpected Server Error",
                getClientIp(request), request.getMethod(), request.getRequestURI(), ex);

        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred.", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("AUDIT_SECURITY | Status: 401 | IP: {} | Method: {} | URI: {} | Error: Authentication Failed",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse("Invalid username or password.", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("AUDIT_SECURITY | Status: 403 | IP: {} | Method: {} | URI: {} | Error: Access Denied",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse("You do not have permission to perform this action.", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 409 | IP: {} | Method: {} | URI: {} | Error: Optimistic Lock Failure",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                "This record was changed by someone else. Please refresh and try again.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 400 | IP: {} | Method: {} | URI: {} | Error: {}",
                getClientIp(request), request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 409 | IP: {} | Method: {} | URI: {} | Error: Database Constraint Violation",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        String message = "Database constraint violation occurred.";
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String causeMessage = mostSpecificCause.getMessage() != null ? mostSpecificCause.getMessage() : "";

        if (causeMessage.contains("foreign key constraint") || causeMessage.contains("violates foreign key")) {
            message = "Cannot delete this record because it is referenced by other entries. Please remove or reassign those references first.";
        } else if (causeMessage.contains("duplicate key") || causeMessage.contains("already exists")) {
            message = "A record with this value already exists.";
        }

        ErrorResponse errorResponse = new ErrorResponse(message, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("AUDIT_FAIL | Status: 400 | IP: {} | Method: {} | URI: {} | Error: Malformed JSON/Payload",
                getClientIp(request), request.getMethod(), request.getRequestURI());

        String message = "Invalid request payload format.";
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String causeMessage = mostSpecificCause.getMessage() != null ? mostSpecificCause.getMessage() : "";

        if (causeMessage.contains("ACCEPT_FLOAT_AS_INT") || causeMessage.contains("Floating-point value")) {
            message = "Decimals are not allowed for this field. Please enter a whole number.";
        } else if (ex.getCause() instanceof JsonMappingException jsonMappingException) {
            String fieldName = "field";
            if (!jsonMappingException.getPath().isEmpty()) {
                var lastPath = jsonMappingException.getPath().get(jsonMappingException.getPath().size() - 1);
                if (lastPath.getFieldName() != null) {
                    fieldName = lastPath.getFieldName();
                }
            }
            message = String.format("Invalid value provided for '%s'. Please check the input format.", fieldName);
        }

        ErrorResponse errorResponse = new ErrorResponse(message, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // --- Helper Method for IP Extraction ---
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}