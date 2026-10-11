package com.bugtracker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", message,
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, message.isBlank() ? "Invalid request" : message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        if ("Email already registered".equals(ex.getMessage()))
            return response(HttpStatus.CONFLICT, "Email already registered");
        return response(HttpStatus.BAD_REQUEST, ex.getMessage() == null ? "Invalid request" : ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        String reason = ex.getReason();
        return response(HttpStatus.valueOf(ex.getStatusCode().value()), reason == null ? "Request failed" : reason);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, Object>> unauthorized(BadCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, Object>> runtime(RuntimeException ex) {
        // Known domain errors are client errors; unexpected runtime errors are logged and returned as 500.
        String message = ex.getMessage();
        if (message != null && (message.endsWith("not found") || message.equals("Email already registered"))) {
            return response(HttpStatus.NOT_FOUND, message);
        }
        log.error("Unhandled application error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> other(Exception ex) {
        log.error("Unhandled application error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");
    }
}
