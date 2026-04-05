package com.madre.excelweb.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.madre.excelweb.service.CatalogImportGame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
public class PsnApiReader {
    private static final String DEFAULT_PAGE_QUERY = "/STORE-MSF77008-ALLGAMES?size=%s&start=%s&gameContentType=games&sort=release_date&direction=desc";
    private final WebClient webClient;

    public List<CatalogImportGame> getGames(String url) {
        try {
            final Optional<JsonNode> jsonData = Optional.ofNullable(fetchJson(url));

            if (jsonData.isEmpty()) {
                log.warn("No games present in call to PSN API from url: {}", url);
                return List.of();
            }

            List<CatalogImportGame> gameData = jsonNodeToGameDetails(jsonData.get());
            log.info("Successfully retrieved games from uri: {}", url);

            return gameData;
        } catch (Exception e) {
            log.error("Error getting games from url: {}", url, e);
            throw e;
        }

    }

    public PsnGameBatch getGamesPage(final int pageSize, final int start) {
        final String uri = DEFAULT_PAGE_QUERY.formatted(pageSize, start);
        final JsonNode jsonNode = fetchJson(uri);

        return new PsnGameBatch(
                jsonNodeToGameDetails(jsonNode),
                safeGetAsInt(jsonNode, "total_results"),
                safeGetAsInt(jsonNode, "start"),
                safeGetAsInt(jsonNode, "size")
        );
    }

    private <T> List<T> iteratorToList(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .collect(Collectors.toList());

    }

    private JsonNode fetchJson(final String url) {
        log.info("Requesting PSN API using uri: {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .blockOptional()
                .orElse(NullNode.instance);
    }

    private List<CatalogImportGame> jsonNodeToGameDetails(JsonNode jsonNode) {
        final JsonNode linksNode = safeGet(jsonNode, "links");
        if (!linksNode.isArray()) {
            return List.of();
        }

        List<JsonNode> games = iteratorToList(linksNode.elements());

        final AtomicInteger gameNumber = new AtomicInteger(0);

        try {
            return games.parallelStream().map(game -> {
                final String release_date = safeGetAsText(game, "release_date");
                final String name = safeGetAsText(game, "name");
                log.info("Converting JsonNode to GameDetails for Game #{} of this batch, {}", gameNumber.incrementAndGet(), name);
                final String providerName = safeGetAsText(game, "provider_name");
                final String id = safeGetAsText(game, "id");
                final String url = safeGetAsText(game, "url");
                final String price = extractPrice(game);
                final String description = extractDescription(game);
                final List<String> platforms = extractPlatforms(game);
                final String imageUrl = extractImageUrl(game);
                final List<String> categories = extractCategories(game);


                return CatalogImportGame.builder()
                        .externalId(id)
                        .name(name)
                        .providerName(providerName)
                        .releaseDate(release_date)
                        .url(url)
                        .price(price)
                        .imageUrl(imageUrl)
                        .platforms(String.join(", ", platforms))
                        .categories(String.join(", ", categories))
                        .description(description)
                        .build();
            }).collect(Collectors.toList());


        } catch (Exception e) {
            log.error("Error converting JSON to GameDetails", e);
            throw e;
        }
    }

    private String safeGetAsText(JsonNode node, String key) {
        JsonNode result = node.get(key);
        return result != null ? result.asText() : "";
    }

    private int safeGetAsInt(JsonNode node, String key) {
        JsonNode result = node.get(key);
        return result != null ? result.asInt() : 0;
    }

    private String safeAsText(JsonNode node) {
        return node != null ? node.asText() : "";
    }

    private JsonNode safeGet(JsonNode node, String key) {
        return node.get(key) != null ? node.get(key) : NullNode.instance;
    }

    private List<String> extractPlatforms(JsonNode game) {
        JsonNode platformNode = safeGet(game, "playable_platform");
        if (platformNode.isArray()) {
            return StreamSupport.stream(platformNode.spliterator(), false)
                    .map(this::safeAsText)
                    .filter(platform -> !platform.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<String> extractCategories(final JsonNode game) {
        final List<String> categories = new ArrayList<>();
        final JsonNode contentTypes = safeGet(game, "gameContentTypesList");
        if (contentTypes.isArray()) {
            StreamSupport.stream(contentTypes.spliterator(), false)
                    .map(node -> safeGetAsText(node, "name"))
                    .filter(category -> !category.isBlank())
                    .forEach(categories::add);
        }

        final String gameContentType = safeGetAsText(game, "game_contentType");
        if (!gameContentType.isBlank()) {
            categories.add(gameContentType);
        }

        return categories.stream().distinct().toList();
    }

    private String extractImageUrl(final JsonNode game) {
        final JsonNode imagesNode = safeGet(game, "images");
        if (imagesNode.isArray()) {
            return StreamSupport.stream(imagesNode.spliterator(), false)
                    .map(imageNode -> safeGetAsText(imageNode, "url"))
                    .filter(url -> !url.isBlank())
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    private String extractPrice(final JsonNode game) {
        final JsonNode defaultSku = safeGet(game, "default_sku");
        if (defaultSku == NullNode.instance) {
            return "";
        }
        return safeGetAsText(defaultSku, "display_price");
    }

    private String extractDescription(final JsonNode game) {
        final JsonNode defaultSku = safeGet(game, "default_sku");
        if (defaultSku == NullNode.instance) {
            return "";
        }
        final String augmentedDescription = safeGetAsText(defaultSku, "augmented_description");
        if (!augmentedDescription.isBlank()) {
            return augmentedDescription;
        }
        return safeGetAsText(defaultSku, "description");
    }
}
