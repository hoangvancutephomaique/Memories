package com.guestbook.controller;

import com.guestbook.config.DeleteSecretGuard;
import com.guestbook.dto.DeleteSecretRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DeleteSecretGuard deleteSecretGuard;

    public AuthController(DeleteSecretGuard deleteSecretGuard) {
        this.deleteSecretGuard = deleteSecretGuard;
    }

    @PostMapping("/verify-delete-secret")
    public ResponseEntity<Void> verifyDeleteSecret(@RequestBody(required = false) DeleteSecretRequest body) {
        String provided = body != null && body.secret() != null ? body.secret() : "";
        if (!deleteSecretGuard.requiresDeleteSecret()) {
            return ResponseEntity.noContent().build();
        }
        if (!deleteSecretGuard.matches(provided)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.noContent().build();
    }
}
