package com.alef.api.portfolio.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps portfolio-specific exceptions to HTTP error responses.
 *
 * Uses RFC 9457 Problem Detail format (Spring 6+). Scoped to the portfolio
 * feature's exception types. If a global exception handler is introduced later,
 * these mappings should fold into it to avoid parallel advice chains.
 */
@RestControllerAdvice
public class PortfolioExceptionHandler {

    /** Unknown vocabulary value in a filter parameter -> 400 Bad Request. */
    @ExceptionHandler(InvalidFilterValueException.class)
    public ProblemDetail handleInvalidFilter(InvalidFilterValueException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid Filter Value");
        return detail;
    }

    /** Slug does not match any project -> 404 Not Found. */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleNotFound(ProjectNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Project Not Found");
        return detail;
    }
}
