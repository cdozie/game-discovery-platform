package com.madre.excelweb.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class IgdbAccessTokenService {
    private static final String TWITCH_OAUTH_PATH = "/oauth2/token";
    private static final String IGDB_CLIENT_ID = "web.app.igdb-sync.client-id";
    private static final String IGDB_CLIENT_SECRET = "web.app.igdb-sync.client-secret";
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60L;

    private final WebClient igdbAuthWebClient;
    private final Environment environment;
    private volatile CachedIgdbToken cachedToken;

    public boolean isConfigured() {
        return !getClientId().isBlank() && !getClientSecret().isBlank();
    }

    public String getClientId() {
        return environment.getProperty(IGDB_CLIENT_ID, "").trim();
    }

    public synchronized String getAccessToken() {
        if (!isConfigured()) {
            throw new IllegalStateException("IGDB sync is not configured. Missing client credentials.");
        }

        if (cachedToken != null && cachedToken.expiresAt().isAfter(Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
            return cachedToken.accessToken();
        }

        final JsonNode response = igdbAuthWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(TWITCH_OAUTH_PATH)
                        .queryParam("client_id", getClientId())
                        .queryParam("client_secret", getClientSecret())
                        .queryParam("grant_type", "client_credentials")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || response.path("access_token").asText("").isBlank()) {
            throw new IllegalStateException("Could not retrieve an IGDB access token.");
        }

        final String accessToken = response.path("access_token").asText();
        final long expiresIn = response.path("expires_in").asLong(0L);
        cachedToken = new CachedIgdbToken(accessToken, Instant.now().plusSeconds(Math.max(0L, expiresIn)));
        log.info("Refreshed IGDB access token. expiresInSeconds={}", expiresIn);
        return accessToken;
    }

    private String getClientSecret() {
        return environment.getProperty(IGDB_CLIENT_SECRET, "").trim();
    }

    private record CachedIgdbToken(String accessToken, Instant expiresAt) {
    }
}
