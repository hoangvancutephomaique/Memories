package com.guestbook.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyFacebookTokenRequest(@NotBlank(message = "Facebook access token is required") String facebookAccessToken) {}
