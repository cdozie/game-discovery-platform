package com.madre.excelweb.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.madre.excelweb.service.CatalogImportGame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
public class IgdbApiReader {
    private static final String IGDB_GAMES_PATH = "/games";
    private static final String IGDB_PLATFORM_IDS = "web.app.igdb-sync.platform-ids";
    private static final String DEFAULT_IGDB_PLATFORM_IDS = "6,49,169";
    private static final DateTimeFormatter RELEASE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US).withZone(ZoneOffset.UTC);

    private final WebClient igdbApiWebClient;
    private final IgdbAccessTokenService igdbAccessTokenService;
    private final Environment environment;

    public IgdbGameBatch getGamesPage(final int pageSize, final int start) {
        final String requestBody = """
                fields id,name,summary,slug,cover.url,platforms.name,genres.name,involved_companies.company.name,first_release_date;
                where version_parent = null & game_type = 0 & release_dates.platform = (%s);
                sort first_release_date desc;
                limit %s;
                offset %s;
                """.formatted(resolvePlatformIds(), pageSize, start);

        final JsonNode jsonNode = igdbApiWebClient.post()
                .uri(IGDB_GAMES_PATH)
                .header("Client-ID", igdbAccessTokenService.getClientId())
                .header("Authorization", "Bearer " + igdbAccessTokenService.getAccessToken())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .blockOptional()
                .orElse(NullNode.instance);

        if (!(jsonNode instanceof ArrayNode gamesNode)) {
            return new IgdbGameBatch(List.of(), start, pageSize);
        }

        final List<CatalogImportGame> games = StreamSupport.stream(gamesNode.spliterator(), false)
                .map(this::jsonNodeToGameDetails)
                .toList();
        log.info("Fetched {} IGDB games at offset {}", games.size(), start);
        return new IgdbGameBatch(games, start, pageSize);
    }

    private String resolvePlatformIds() {
        return environment.getProperty(IGDB_PLATFORM_IDS, DEFAULT_IGDB_PLATFORM_IDS).trim();
    }

    private CatalogImportGame jsonNodeToGameDetails(final JsonNode game) {
        final long igdbId = game.path("id").asLong();
        final String slug = game.path("slug").asText("");

        return CatalogImportGame.builder()
                .externalId("IGDB:" + igdbId)
                .name(game.path("name").asText(""))
                .providerName(extractProviderName(game))
                .releaseDate(formatReleaseDate(game.path("first_release_date")))
                .url(slug.isBlank() ? "" : "https://www.igdb.com/games/" + slug)
                .price("")
                .imageUrl(normalizeImageUrl(game.path("cover").path("url").asText("")))
                .platforms(String.join(", ", extractNames(game.path("platforms"))))
                .categories(String.join(", ", extractNames(game.path("genres"))))
                .description(game.path("summary").asText(""))
                .build();
    }

    private List<String> extractNames(final JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(node.spliterator(), false)
                .map(child -> child.path("name").asText(""))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String extractProviderName(final JsonNode game) {
        final JsonNode involvedCompanies = game.path("involved_companies");
        if (!involvedCompanies.isArray()) {
            return "";
        }

        return StreamSupport.stream(involvedCompanies.spliterator(), false)
                .map(companyNode -> companyNode.path("company").path("name").asText(""))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private String normalizeImageUrl(final String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        if (imageUrl.startsWith("//")) {
            return "https:" + imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return "https://images.igdb.com" + imageUrl;
        }
        return imageUrl;
    }

    private String formatReleaseDate(final JsonNode releaseDateNode) {
        if (releaseDateNode == null || releaseDateNode.isNull() || releaseDateNode.asLong(0L) <= 0L) {
            return "";
        }
        return RELEASE_DATE_FORMATTER.format(Instant.ofEpochSecond(releaseDateNode.asLong()));
    }
}
