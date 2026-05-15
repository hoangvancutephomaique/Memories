package com.guestbook.controller;

import com.guestbook.config.DeleteSecretGuard;
import com.guestbook.dto.DeleteSecretRequest;
import com.guestbook.dto.VerifyFacebookTokenRequest;
import com.guestbook.service.FacebookIdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DeleteSecretGuard deleteSecretGuard;
    private final FacebookIdentityService facebookIdentityService;

    public AuthController(
            DeleteSecretGuard deleteSecretGuard,
            FacebookIdentityService facebookIdentityService) {
        this.deleteSecretGuard = deleteSecretGuard;
        this.facebookIdentityService = facebookIdentityService;
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

    @PostMapping("/verify-facebook-token")
    public ResponseEntity<Void> verifyFacebookToken(@Valid @RequestBody VerifyFacebookTokenRequest body) {
        try {
            facebookIdentityService.resolveName(body.facebookAccessToken());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
