package com.guestbook.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.guestbook.dto.GoogleProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoogleIdentityService {

    private static final int MAX_DISPLAY_NAME = 200;
    private static final int MAX_EMAIL = 254;

    private final List<String> clientIds;

    public GoogleIdentityService(@Value("${guestbook.google.client-ids:}") String clientIdsCsv) {
        this.clientIds = Arrays.stream(clientIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Verifies a Google Sign-In ID token and extracts display name and email when present.
     */
    public GoogleProfile resolveProfile(String idToken) {
        if (clientIds.isEmpty()) {
            throw new IllegalStateException("Google sign-in is not configured (missing guestbook.google.client-ids)");
        }
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(clientIds)
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String rawName = (String) payload.get("name");
            String displayName = (rawName == null || rawName.isBlank())
                    ? null
                    : truncate(rawName.trim(), MAX_DISPLAY_NAME);
            String rawEmail = (String) payload.get("email");
            String email = (rawEmail == null || rawEmail.isBlank())
                    ? null
                    : truncate(rawEmail.trim(), MAX_EMAIL);
            if (displayName == null && email == null) {
                throw new IllegalArgumentException("Google profile has no name or email");
            }
            return new GoogleProfile(displayName, email);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Google ID token", e);
        }
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
