package com.guestbook.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyGoogleTokenRequest(
        @NotBlank(message = "Google ID token is required")
        String idToken
) {}
