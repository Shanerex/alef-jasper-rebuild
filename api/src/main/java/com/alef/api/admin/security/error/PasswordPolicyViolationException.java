package com.alef.api.admin.security.error;

import java.util.List;

/**
 * Thrown when a new password fails the policy check (design §A.7: non-blank,
 * minimum 12 characters, must differ from the current password). Carries every
 * violated rule's message so the form can show them all at once rather than
 * one-at-a-time.
 */
public class PasswordPolicyViolationException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super("New password does not meet the policy requirements.");
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
