package com.fdmgroup.sprint4Deliverable.Config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fdmgroup.sprint4Deliverable.Exception.AdminAuthenticationException;
import com.fdmgroup.sprint4Deliverable.Exception.ValidationException;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AdminAuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAdminAuthenticationException(
            AdminAuthenticationException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage(), "error", ex.getErrorCode()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage(), "error", "validation_error"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "The request body is missing or invalid.", "error", "validation_error"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessException(DataAccessException ex) {
        LOGGER.error("Database operation failed ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "The service is temporarily unavailable.", "error", "service_unavailable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception ex) {
        LOGGER.error("Unexpected API failure ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "The request could not be completed.", "error", "internal_error"));
    }
}
