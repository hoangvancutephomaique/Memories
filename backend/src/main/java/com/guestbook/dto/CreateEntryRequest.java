package com.guestbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEntryRequest(
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @NotBlank(message = "Message is required")
        @Size(max = 500000, message = "Message must be 500000 characters or fewer")
        String message,

        @NotBlank(message = "Facebook access token is required")
        String facebookAccessToken
) {}
