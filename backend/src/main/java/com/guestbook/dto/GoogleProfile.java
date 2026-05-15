package com.guestbook.dto;

/**
 * Verified Google account fields from an ID token (never returned in public API JSON).
 */
public record GoogleProfile(String displayName, String email) {
}
