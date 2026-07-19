package com.alef.api.admin.security.error;

import com.alef.api.admin.security.PasswordChangeController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps password-change exceptions to RFC 9457 ProblemDetail responses
 * (design.md §A.7). Scoped to PasswordChangeController only.
 */
@RestControllerAdvice(assignableTypes = PasswordChangeController.class)
public class PasswordChangeExceptionHandler {

    /**
     * Wrong current password -> 400, mapped onto the currentPassword field
     * (design §A.7 step 1: "does not touch the session" -- there is nothing
     * session-related here, this is a plain 400).
     */
    @ExceptionHandler(WrongCurrentPasswordException.class)
    public ProblemDetail handleWrongCurrentPassword(WrongCurrentPasswordException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        detail.setTitle("Invalid Credentials");
        detail.setProperty("fields", Map.of("currentPassword", List.of("Current password is incorrect.")));
        return detail;
    }

    /** New-password policy violation -> 400, mapped onto the newPassword field. */
    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ProblemDetail handlePolicyViolation(PasswordPolicyViolationException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "New password does not meet the policy requirements.");
        detail.setTitle("Validation Failed");
        detail.setProperty("fields", Map.of("newPassword", ex.getViolations()));
        return detail;
    }

    /** Bean Validation failure (blank currentPassword/newPassword) -> 400. */
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
}
