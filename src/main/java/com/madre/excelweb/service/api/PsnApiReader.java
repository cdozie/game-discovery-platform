package com.madre.excelweb.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.madre.excelweb.jpa.entity.GameData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
public class PsnApiReader {
    private final WebClient webClient;

    public List<GameData> getGames(String url) {
        try {
            log.info("Requesting PSN API using uri: {}", url);

            final Optional<JsonNode> jsonData = Optional.ofNullable(webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block());

            if (jsonData.isEmpty()) {
                log.warn("No games present in call to PSN API from url: {}", url);
                return List.of();
            }

            List<GameData> gameData = jsonNodeToGameDetails(jsonData.get());
            log.info("Successfully retrieved games from uri: {}", url);

            return gameData;
        } catch (Exception e) {
            log.error("Error getting games from url: {}", url, e);
            throw e;
        }

    }

    private <T> List<T> iteratorToList(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .collect(Collectors.toList());

    }

    private List<GameData> jsonNodeToGameDetails(JsonNode jsonNode) {
        List<JsonNode> games = iteratorToList(jsonNode.get("links").elements());

        final AtomicInteger gameNumber = new AtomicInteger(0);

        try {
            return games.parallelStream().map(game -> {
                final String release_date = safeGetAsText(game, "release_date");
                final String name = safeGetAsText(game, "name");
                log.info("Converting JsonNode to GameDetails for Game #{} of this batch, {}", gameNumber.incrementAndGet(), name);
                final String providerName = safeGetAsText(game, "provider_name");
                final String id = safeGetAsText(game, "id");
                final String url = safeGetAsText(game, "url");
                final String price = safeGet(game, "default_sku") != NullNode.instance ? safeGetAsText(game.get("default_sku"), "display_price") : "";
                final String description = safeGet(game, "default_sku") != NullNode.instance ? safeGetAsText(game.get("default_sku"), "augmented_description") : "";

                final List<JsonNode> platformNodes = iteratorToList(safeGet(game, "playable_platform").elements());
                final List<String> platforms = extractPlatforms(game);
                final List<JsonNode> imagesNodes = iteratorToList(safeGet(game, "images").elements());
                final String imageUrl = !imagesNodes.isEmpty() ? safeGetAsText(imagesNodes.get(0), "url") : "";


                return GameData.builder()
                        .psnId(id)
                        .name(name)
                        .providerName(providerName)
                        .release_date(release_date)
                        .url(url)
                        .price(price)
                        .imageUrl(imageUrl)
                        .platforms(String.join(", ", platforms))
                        .categories("")
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
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}


