package com.ena.mam.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
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
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        ));
        ex.getBindingResult()
                .getGlobalErrors()
                .forEach(error ->
                        errors.put(
                                "_general",
                                error.getDefaultMessage()
                        ));
        return ResponseEntity.badRequest().body(errors);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {
        // This was previously swallowing every unanticipated exception with
        // no server-side trace at all -- if the client saw "An unexpected
        // error occurred." there was nothing to look at in the console to
        // find out why. Logging here doesn't change what the client
        // receives, it just makes the real cause visible on the server.
        log.error("Unhandled exception", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "An unexpected error occurred.",
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid username or password.",
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex
    ){
        ErrorResponse errorResponse = new ErrorResponse("You do not have permission to perform this action.",LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "This record was changed by someone else. Please refresh and try again.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse); // 409
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Database constraint violation occurred.";

        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String causeMessage = mostSpecificCause.getMessage() != null ? mostSpecificCause.getMessage() : "";

        // 1. Foreign Key Violation (Deleting referenced record or invalid foreign ID)
        if (causeMessage.contains("foreign key constraint") || causeMessage.contains("violates foreign key")) {
            message = "Cannot delete this record because it is referenced by other entries. Please remove or reassign those references first.";
        }
        // 2. Unique Constraint Violation (Duplicate entry)
        else if (causeMessage.contains("duplicate key") || causeMessage.contains("already exists")) {
            message = "A record with this value already exists.";
        }

        ErrorResponse errorResponse = new ErrorResponse(message, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse); // 409 Conflict
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request payload format.";

        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String causeMessage = mostSpecificCause.getMessage() != null ? mostSpecificCause.getMessage() : "";

        // 1. Detect float-to-int coercion failure directly from Jackson's error message
        if (causeMessage.contains("ACCEPT_FLOAT_AS_INT") || causeMessage.contains("Floating-point value")) {
            message = "Decimals are not allowed for this field. Please enter a whole number.";
        }
        // 2. Fall back to extracting the field name for general mapping/type errors
        else if (ex.getCause() instanceof JsonMappingException jsonMappingException) {
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

}