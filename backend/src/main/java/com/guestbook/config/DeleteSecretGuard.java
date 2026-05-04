package com.guestbook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

@Component
public class DeleteSecretGuard {

    private final String secret;

    public DeleteSecretGuard(@Value("${guestbook.delete-secret:}") String secret) {
        this.secret = secret != null ? secret : "";
    }

    /** When blank, DELETE is not protected (local dev convenience). */
    public boolean requiresDeleteSecret() {
        return !secret.isBlank();
    }

    public boolean matches(String provided) {
        if (!requiresDeleteSecret()) {
            return true;
        }
        String p = provided != null ? provided : "";
        return MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                p.getBytes(StandardCharsets.UTF_8));
    }

    public boolean headerMatches(String headerValue) {
        return matches(Objects.requireNonNullElse(headerValue, ""));
    }
}
