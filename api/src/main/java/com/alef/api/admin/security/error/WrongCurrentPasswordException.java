package com.alef.api.admin.security.error;

/**
 * Thrown when a POST /api/admin/password request's currentPassword does not
 * match the stored hash (design §A.7). This is a request-validation failure,
 * not an authentication failure -- the caller's existing session is left
 * untouched (F12-AC28's "does not touch the session" rule).
 */
public class WrongCurrentPasswordException extends RuntimeException {

    public WrongCurrentPasswordException() {
        super("Current password is incorrect.");
    }
}
