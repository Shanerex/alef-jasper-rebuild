package com.alef.api.admin.security;

import com.alef.api.admin.security.dto.PasswordChangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/admin/password -- self-service password change (design.md §A.7, F12-AC28).
 *
 * Reachable only when authenticated + CSRF-valid (falls under the general
 * /api/admin/** hasRole(ADMIN) rule in AdminSecurityConfig; POST is a
 * CSRF-protected method by default). No plaintext is ever logged here or in
 * PasswordChangeService (F12-AC29).
 */
@RestController
@RequestMapping("/api/admin/password")
public class PasswordChangeController {

    private final PasswordChangeService service;

    public PasswordChangeController(PasswordChangeService service) {
        this.service = service;
    }

    /**
     * Changes the single admin's password. 204 on success; the current
     * session is left valid (F12-AC28: takes effect on next login).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        service.changePassword(request.currentPassword(), request.newPassword());
    }
}
