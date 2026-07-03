package com.alef.api.lead.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.alef.api.lead.controller.LeadController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps lead-specific exceptions to RFC 9457 ProblemDetail responses
 * (architecture §3.4, feature 011).
 *
 * Scoped to LeadController only, mirroring PortfolioExceptionHandler's pattern:
 * one @RestControllerAdvice per feature so advice chains never bleed across modules.
 *
 * Handles two cases:
 * - MethodArgumentNotValidException (400): Bean Validation failure with field map.
 * - ResponseStatusException 429: rate-limit breach, must carry Retry-After: 3600
 *   (architecture §3.4 / §10 Decision 2).
 */
@RestControllerAdvice(assignableTypes = LeadController.class)
public class LeadExceptionHandler {

    /** Retry-After window in seconds, matching the Redis rate-limit window in LeadService. */
    private static final String RETRY_AFTER_SECONDS = "3600";

    /**
     * Bean Validation failure on the POST /api/leads body -> 400 with field errors.
     *
     * The 'fields' extension map carries field-name -> [error messages] so the
     * ContactForm component can map server errors to the relevant form fields
     * without parsing the detail string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more fields failed validation.");
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

    /**
     * Rate-limit breach on POST /api/leads -> 429 with Retry-After: 3600 header.
     *
     * Architecture §3.4 and §10 Decision 2 both mandate the Retry-After header so
     * the ContactForm can surface the correct wait-copy to the user. The plain
     * ResponseStatusException thrown by LeadService would propagate without this
     * header, so we handle it explicitly here.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(ResponseStatusException ex) {
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ex.getReason() != null ? ex.getReason() : "Too many requests.");
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(detail);
        }
        return ResponseEntity.status(ex.getStatusCode()).build();
    }
}
