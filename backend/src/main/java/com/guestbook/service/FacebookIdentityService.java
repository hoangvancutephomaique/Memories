package com.guestbook.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FacebookIdentityService {

    private static final int MAX_NAME_LENGTH = 200;

    private final RestClient graphClient = RestClient.builder()
            .baseUrl("https://graph.facebook.com")
            .build();

    @Value("${guestbook.facebook.app-id:}")
    private String appId;

    @Value("${guestbook.facebook.app-secret:}")
    private String appSecret;

    public String resolveName(String userAccessToken) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("Facebook app id/secret not configured");
        }
        if (userAccessToken == null || userAccessToken.isBlank()) {
            throw new IllegalArgumentException("Missing access token");
        }

        DebugTokenResponse debug = graphClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/debug_token")
                        .queryParam("input_token", userAccessToken)
                        .queryParam("access_token", appId + "|" + appSecret)
                        .build())
                .retrieve()
                .body(DebugTokenResponse.class);

        if (debug == null || debug.data == null || !debug.data.isValid || !appId.equals(debug.data.appId)) {
            throw new IllegalArgumentException("Invalid Facebook token");
        }

        MeResponse me = graphClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/me")
                        .queryParam("fields", "name")
                        .queryParam("access_token", userAccessToken)
                        .build())
                .retrieve()
                .body(MeResponse.class);

        if (me == null || me.name == null || me.name.isBlank()) {
            throw new IllegalArgumentException("Facebook profile has no name");
        }
        String name = me.name.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        return name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class DebugTokenResponse {
        @JsonProperty("data")
        DebugData data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class DebugData {
        @JsonProperty("is_valid")
        boolean isValid;

        @JsonProperty("app_id")
        String appId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class MeResponse {
        String name;
    }
}
