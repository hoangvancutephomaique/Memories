package com.guestbook.controller;

import com.guestbook.config.DeleteSecretGuard;
import com.guestbook.dto.DeleteSecretRequest;
import com.guestbook.dto.VerifyGoogleTokenRequest;
import com.guestbook.service.GoogleIdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DeleteSecretGuard deleteSecretGuard;
    private final GoogleIdentityService googleIdentityService;

    public AuthController(
            DeleteSecretGuard deleteSecretGuard,
            GoogleIdentityService googleIdentityService) {
        this.deleteSecretGuard = deleteSecretGuard;
        this.googleIdentityService = googleIdentityService;
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

    @PostMapping("/verify-google-token")
    public ResponseEntity<Void> verifyGoogleToken(@Valid @RequestBody VerifyGoogleTokenRequest body) {
        try {
            googleIdentityService.resolveProfile(body.idToken());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
