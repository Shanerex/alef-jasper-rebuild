package com.alef.api.admin.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps login-specific exceptions to RFC 9457 ProblemDetail responses
 * (design.md §A.1), scoped to SessionController only -- mirrors
 * LeadExceptionHandler's per-feature advice pattern.
 */
@RestControllerAdvice(assignableTypes = SessionController.class)
public class AdminAuthExceptionHandler {

    /** Retry-After window in seconds, matching AdminSessionService's rate-limit window. */
    private static final String RETRY_AFTER_SECONDS = "900";

    /**
     * Bad username or password -> 401. Deliberately does not reveal which of
     * the two was wrong (design §A.1: "do not reveal which one").
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleBadCredentials(AuthenticationException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Username or password is incorrect.");
        detail.setTitle("Invalid Credentials");
        return detail;
    }

    /** Bean Validation failure on the login body (e.g. blank username/password) -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields failed validation.");
        detail.setTitle("Validation Failed");
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())));
        detail.setProperty("fields", fieldErrors);
        return detail;
    }

    /** Login rate-limit breach -> 429 with Retry-After (design §A.1). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(ResponseStatusException ex) {
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ex.getReason() != null ? ex.getReason() : "Too many requests.");
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(detail);
        }
        return ResponseEntity.status(ex.getStatusCode()).build();
    }
}
