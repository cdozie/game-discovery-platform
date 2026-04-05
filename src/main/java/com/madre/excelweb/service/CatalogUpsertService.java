package com.madre.excelweb.service;

import com.madre.excelweb.jpa.entity.GameSourceEntry;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.GameSourceEntryRepository;
import com.madre.excelweb.models.AdminGameField;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.service.util.AdminOverrideUtils;
import com.madre.excelweb.service.util.CatalogGroupingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogUpsertService {
    private final GameRepository gameRepository;
    private final GameSourceEntryRepository gameSourceEntryRepository;
    private final IdGenerationService idGenerationService;

    @Transactional
    public CatalogBatchUpsertResult upsertBatch(final List<CatalogImportGame> incomingGames, final CatalogSource catalogSource) {
        if (incomingGames == null || incomingGames.isEmpty()) {
            return CatalogBatchUpsertResult.empty();
        }

        final List<IncomingAggregate> incomingAggregates = aggregateIncomingGames(incomingGames, catalogSource);

        final Map<String, GameSourceEntry> existingSourceEntriesByExternalId = gameSourceEntryRepository
                .findByCatalogSourceAndExternalIdIn(catalogSource, collectExternalIds(incomingAggregates))
                .stream()
                .collect(Collectors.toMap(
                        GameSourceEntry::getExternalId,
                        Function.identity(),
                        this::selectCanonicalSourceEntry
                ));

        final Map<String, GameData> existingGamesByGroupKey = gameRepository.findByCatalogGroupKeyIn(
                        incomingAggregates.stream()
                                .map(aggregate -> aggregate.game().getCatalogGroupKey())
                                .filter(Objects::nonNull)
                                .filter(value -> !value.isBlank())
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        GameData::getCatalogGroupKey,
                        Function.identity(),
                        this::selectExistingCanonicalGame
                ));

        final Map<String, List<GameData>> existingGamesByNormalizedTitleKey = gameRepository.findByNormalizedTitleKeyIn(
                        incomingAggregates.stream()
                                .map(aggregate -> aggregate.game().getNormalizedTitleKey())
                                .filter(Objects::nonNull)
                                .filter(value -> !value.isBlank())
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.groupingBy(GameData::getNormalizedTitleKey));

        long createdCount = 0L;
        long updatedCount = 0L;
        long unchangedCount = 0L;
        final List<GameSourceEntry> sourceEntriesToPersist = new ArrayList<>();
        final Map<Long, GameData> persistedGamesById = new java.util.LinkedHashMap<>();

        for (IncomingAggregate incomingAggregate : incomingAggregates) {
            final GameData incomingGame = incomingAggregate.game();
            final GameData existingGame = resolveExistingGame(
                    incomingAggregate,
                    existingSourceEntriesByExternalId,
                    existingGamesByGroupKey,
                    existingGamesByNormalizedTitleKey
            );
            if (existingGame == null) {
                initializeStats(incomingGame, catalogSource);
                incomingGame.setId(idGenerationService.nextId());
                incomingGame.setCreated(OffsetDateTime.now(ZoneOffset.UTC));
                incomingGame.setLastModified(OffsetDateTime.now(ZoneOffset.UTC));
                persistedGamesById.put(incomingGame.getId(), incomingGame);
                existingGamesByGroupKey.put(incomingGame.getCatalogGroupKey(), incomingGame);
                addToNormalizedTitleLookup(existingGamesByNormalizedTitleKey, incomingGame);
                attachSourceEntries(incomingAggregate, incomingGame, catalogSource, existingSourceEntriesByExternalId, sourceEntriesToPersist);
                createdCount++;
                continue;
            }

            if (!existingGame.equalsCoreData(incomingGame)
                    || !CatalogGroupingUtils.containsDelimitedValue(existingGame.getCatalogSources(), catalogSource.name())
                    || isMissingSourceEntry(incomingAggregate, existingGame, existingSourceEntriesByExternalId)) {
                applyCatalogUpdate(existingGame, incomingGame, catalogSource);
                existingGame.setLastModified(OffsetDateTime.now(ZoneOffset.UTC));
                persistedGamesById.put(existingGame.getId(), existingGame);
                updatedCount++;
            } else {
                unchangedCount++;
            }

            attachSourceEntries(incomingAggregate, existingGame, catalogSource, existingSourceEntriesByExternalId, sourceEntriesToPersist);
            existingGamesByGroupKey.put(existingGame.getCatalogGroupKey(), existingGame);
            addToNormalizedTitleLookup(existingGamesByNormalizedTitleKey, existingGame);
        }

        if (!persistedGamesById.isEmpty()) {
            gameRepository.saveAll(new ArrayList<>(persistedGamesById.values()));
        }

        if (!sourceEntriesToPersist.isEmpty()) {
            gameSourceEntryRepository.saveAll(sourceEntriesToPersist);
        }

        return new CatalogBatchUpsertResult(incomingGames.size(), createdCount, updatedCount, unchangedCount);
    }

    private List<IncomingAggregate> aggregateIncomingGames(final List<CatalogImportGame> incomingGames,
                                                           final CatalogSource catalogSource) {
        final List<IncomingAggregate> aggregatedGames = new ArrayList<>();

        for (CatalogImportGame incomingGame : incomingGames) {
            final IncomingAggregate nextAggregate = IncomingAggregate.fromImportGame(incomingGame, catalogSource);
            boolean merged = false;

            for (int index = 0; index < aggregatedGames.size(); index++) {
                final IncomingAggregate existingAggregate = aggregatedGames.get(index);
                if (shouldMergeIncomingAggregates(existingAggregate, nextAggregate)) {
                    aggregatedGames.set(index, mergeIncomingAggregates(existingAggregate, nextAggregate, catalogSource));
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                aggregatedGames.add(nextAggregate);
            }
        }

        return aggregatedGames;
    }

    private Collection<String> collectExternalIds(final Collection<IncomingAggregate> incomingAggregates) {
        return incomingAggregates.stream()
                .flatMap(aggregate -> aggregate.externalIds().stream())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private IncomingAggregate mergeIncomingAggregates(final IncomingAggregate leftAggregate,
                                                      final IncomingAggregate rightAggregate,
                                                      final CatalogSource catalogSource) {
        return new IncomingAggregate(
                mergeCatalogVariants(leftAggregate.game(), rightAggregate.game(), catalogSource),
                CatalogGroupingUtils.firstNonBlank(leftAggregate.primaryExternalId(), rightAggregate.primaryExternalId()),
                mergeExternalIds(leftAggregate.externalIds(), rightAggregate.externalIds())
        );
    }

    private boolean shouldMergeIncomingAggregates(final IncomingAggregate leftAggregate,
                                                  final IncomingAggregate rightAggregate) {
        return Objects.equals(leftAggregate.game().getCatalogGroupKey(), rightAggregate.game().getCatalogGroupKey())
                || CatalogGroupingUtils.shouldMergeByTitleIdentity(leftAggregate.game(), rightAggregate.game());
    }

    private LinkedHashSet<String> mergeExternalIds(final Collection<String> leftExternalIds,
                                                   final Collection<String> rightExternalIds) {
        final LinkedHashSet<String> mergedExternalIds = new LinkedHashSet<>();
        mergedExternalIds.addAll(leftExternalIds);
        mergedExternalIds.addAll(rightExternalIds);
        return mergedExternalIds;
    }

    private GameData resolveExistingGame(final IncomingAggregate incomingAggregate,
                                         final Map<String, GameSourceEntry> existingSourceEntriesByExternalId,
                                         final Map<String, GameData> existingGamesByGroupKey,
                                         final Map<String, List<GameData>> existingGamesByNormalizedTitleKey) {
        final List<GameData> sourceMatchedGames = incomingAggregate.externalIds().stream()
                .map(existingSourceEntriesByExternalId::get)
                .filter(Objects::nonNull)
                .map(GameSourceEntry::getGame)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        GameData::getId,
                        Function.identity(),
                        (leftGame, rightGame) -> leftGame
                ))
                .values()
                .stream()
                .sorted(Comparator
                        .comparing(GameData::getCreated, Comparator.nullsLast(OffsetDateTime::compareTo))
                        .thenComparing(GameData::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        if (!sourceMatchedGames.isEmpty()) {
            if (sourceMatchedGames.size() > 1) {
                final List<Long> gameIds = sourceMatchedGames.stream()
                        .map(GameData::getId)
                        .filter(Objects::nonNull)
                        .toList();
                log.warn("Multiple canonical games matched source entries {} for catalog group {}. Using oldest game id {}.",
                        incomingAggregate.externalIds(),
                        incomingAggregate.game().getCatalogGroupKey(),
                        gameIds.isEmpty() ? null : gameIds.get(0));
            }
            return sourceMatchedGames.get(0);
        }

        final GameData groupKeyMatch = existingGamesByGroupKey.get(incomingAggregate.game().getCatalogGroupKey());
        if (groupKeyMatch != null) {
            return groupKeyMatch;
        }

        return existingGamesByNormalizedTitleKey
                .getOrDefault(incomingAggregate.game().getNormalizedTitleKey(), List.of())
                .stream()
                .filter(candidate -> CatalogGroupingUtils.shouldMergeByTitleIdentity(candidate, incomingAggregate.game()))
                .sorted(Comparator
                        .comparing(GameData::getCreated, Comparator.nullsLast(OffsetDateTime::compareTo))
                        .thenComparing(GameData::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
    }

    private boolean isMissingSourceEntry(final IncomingAggregate incomingAggregate,
                                         final GameData targetGame,
                                         final Map<String, GameSourceEntry> existingSourceEntriesByExternalId) {
        return incomingAggregate.externalIds().stream()
                .map(existingSourceEntriesByExternalId::get)
                .anyMatch(sourceEntry -> sourceEntry == null
                        || sourceEntry.getGame() == null
                        || !Objects.equals(sourceEntry.getGame().getId(), targetGame.getId()));
    }

    private void attachSourceEntries(final IncomingAggregate incomingAggregate,
                                     final GameData targetGame,
                                     final CatalogSource catalogSource,
                                     final Map<String, GameSourceEntry> existingSourceEntriesByExternalId,
                                     final List<GameSourceEntry> sourceEntriesToPersist) {
        for (String externalId : incomingAggregate.externalIds()) {
            if (externalId == null || externalId.isBlank()) {
                continue;
            }

            final GameSourceEntry existingSourceEntry = existingSourceEntriesByExternalId.get(externalId);
            if (existingSourceEntry == null) {
                final GameSourceEntry createdSourceEntry = GameSourceEntry.builder()
                        .game(targetGame)
                        .catalogSource(catalogSource)
                        .externalId(externalId)
                        .build();
                sourceEntriesToPersist.add(createdSourceEntry);
                existingSourceEntriesByExternalId.put(externalId, createdSourceEntry);
                continue;
            }

            if (existingSourceEntry.getGame() == null || !Objects.equals(existingSourceEntry.getGame().getId(), targetGame.getId())) {
                existingSourceEntry.setGame(targetGame);
                sourceEntriesToPersist.add(existingSourceEntry);
            }
        }
    }

    private void applyCatalogUpdate(final GameData target,
                                    final GameData source,
                                    final CatalogSource incomingCatalogSource) {
        final boolean alreadyContainsSource = CatalogGroupingUtils.containsDelimitedValue(target.getCatalogSources(), incomingCatalogSource.name());

        target.setCatalogSources(CatalogGroupingUtils.mergeDelimitedValues(target.getCatalogSources(), source.getCatalogSources(), incomingCatalogSource.name()));
        target.setName(resolveAdminManagedValue(target, AdminGameField.NAME, target.getName(),
                CatalogGroupingUtils.pickPreferredTitle(target.getName(), source.getName())));
        target.setDescription(resolveAdminManagedValue(target, AdminGameField.DESCRIPTION, target.getDescription(),
                CatalogGroupingUtils.pickPreferredText(target.getDescription(), source.getDescription())));
        target.setCategories(resolveAdminManagedValue(target, AdminGameField.CATEGORIES, target.getCategories(),
                CatalogGroupingUtils.mergeDelimitedValues(target.getCategories(), source.getCategories())));
        target.setPlatforms(resolveAdminManagedValue(target, AdminGameField.PLATFORMS, target.getPlatforms(),
                CatalogGroupingUtils.mergePlatformValues(target.getPlatforms(), source.getPlatforms())));
        target.setPrice(resolveAdminManagedValue(target, AdminGameField.PRICE, target.getPrice(),
                resolvePreferredPlayStationBackedValue(target.getPrice(), source.getPrice(), target.getCatalogSources(), incomingCatalogSource, alreadyContainsSource)));
        target.setUrl(resolveAdminManagedValue(target, AdminGameField.URL, target.getUrl(),
                resolvePreferredPlayStationBackedValue(target.getUrl(), source.getUrl(), target.getCatalogSources(), incomingCatalogSource, alreadyContainsSource)));
        target.setReleaseDate(resolveAdminManagedValue(target, AdminGameField.RELEASE_DATE, target.getReleaseDate(),
                resolvePreferredPlayStationBackedValue(target.getReleaseDate(), source.getReleaseDate(), target.getCatalogSources(), incomingCatalogSource, alreadyContainsSource)));
        target.setProviderName(resolveAdminManagedValue(target, AdminGameField.PROVIDER_NAME, target.getProviderName(),
                resolvePreferredProviderName(target.getProviderName(), source.getProviderName())));
        target.setImageUrl(resolveAdminManagedValue(target, AdminGameField.IMAGE_URL, target.getImageUrl(),
                resolvePreferredImage(target, source, incomingCatalogSource, alreadyContainsSource)));
        target.setNormalizedTitleKey(CatalogGroupingUtils.buildNormalizedTitleKey(target));
        target.setCatalogGroupKey(CatalogGroupingUtils.buildGroupKey(target));
    }

    private String preferFieldValue(final String existingValue,
                                    final String incomingValue,
                                    final boolean alreadyContainsSource) {
        return alreadyContainsSource
                ? CatalogGroupingUtils.firstNonBlank(incomingValue, existingValue)
                : CatalogGroupingUtils.firstNonBlank(existingValue, incomingValue);
    }

    private String resolvePreferredImage(final GameData target,
                                         final GameData source,
                                         final CatalogSource incomingCatalogSource,
                                         final boolean alreadyContainsSource) {
        if (incomingCatalogSource == CatalogSource.PLAYSTATION_STORE && source.getImageUrl() != null && !source.getImageUrl().isBlank()) {
            return source.getImageUrl();
        }

        if (CatalogGroupingUtils.containsDelimitedValue(target.getCatalogSources(), CatalogSource.PLAYSTATION_STORE.name())
                && target.getImageUrl() != null
                && !target.getImageUrl().isBlank()) {
            return target.getImageUrl();
        }

        return preferFieldValue(target.getImageUrl(), source.getImageUrl(), alreadyContainsSource);
    }

    private String resolvePreferredPlayStationBackedValue(final String existingValue,
                                                          final String incomingValue,
                                                          final String existingCatalogSources,
                                                          final CatalogSource incomingCatalogSource,
                                                          final boolean alreadyContainsSource) {
        if (incomingCatalogSource == CatalogSource.PLAYSTATION_STORE && incomingValue != null && !incomingValue.isBlank()) {
            return incomingValue;
        }

        if (CatalogGroupingUtils.containsDelimitedValue(existingCatalogSources, CatalogSource.PLAYSTATION_STORE.name())
                && existingValue != null
                && !existingValue.isBlank()) {
            return existingValue;
        }

        return preferFieldValue(existingValue, incomingValue, alreadyContainsSource);
    }

    private String resolvePreferredProviderName(final String existingValue, final String incomingValue) {
        return CatalogGroupingUtils.pickPreferredText(existingValue, incomingValue);
    }

    private GameData initializeStats(final GameData game, final CatalogSource catalogSource) {
        game.setInteractions(0L);
        game.setWishlistTotal(0L);
        game.setComments(0L);
        game.setPosts(0L);
        game.setAverageRating(0.0);
        game.setNormalizedTitleKey(CatalogGroupingUtils.buildNormalizedTitleKey(game));
        game.setCatalogGroupKey(CatalogGroupingUtils.buildGroupKey(game));
        game.setCatalogSources(CatalogGroupingUtils.mergeDelimitedValues(game.getCatalogSources(), catalogSource.name()));
        return game;
    }

    private GameData mergeCatalogVariants(final GameData primaryGame,
                                          final GameData variantGame,
                                          final CatalogSource catalogSource) {
        primaryGame.setCatalogSources(CatalogGroupingUtils.mergeDelimitedValues(primaryGame.getCatalogSources(),
                variantGame.getCatalogSources(),
                catalogSource.name()));
        primaryGame.setName(resolveAdminManagedValue(primaryGame, AdminGameField.NAME, primaryGame.getName(),
                CatalogGroupingUtils.pickPreferredTitle(primaryGame.getName(), variantGame.getName())));
        primaryGame.setPlatforms(resolveAdminManagedValue(primaryGame, AdminGameField.PLATFORMS, primaryGame.getPlatforms(),
                CatalogGroupingUtils.mergePlatformValues(primaryGame.getPlatforms(), variantGame.getPlatforms())));
        primaryGame.setCategories(resolveAdminManagedValue(primaryGame, AdminGameField.CATEGORIES, primaryGame.getCategories(),
                CatalogGroupingUtils.mergeDelimitedValues(primaryGame.getCategories(), variantGame.getCategories())));
        primaryGame.setDescription(resolveAdminManagedValue(primaryGame, AdminGameField.DESCRIPTION, primaryGame.getDescription(),
                CatalogGroupingUtils.pickPreferredText(primaryGame.getDescription(), variantGame.getDescription())));
        primaryGame.setPrice(resolveAdminManagedValue(primaryGame, AdminGameField.PRICE, primaryGame.getPrice(),
                CatalogGroupingUtils.firstNonBlank(primaryGame.getPrice(), variantGame.getPrice())));
        primaryGame.setUrl(resolveAdminManagedValue(primaryGame, AdminGameField.URL, primaryGame.getUrl(),
                CatalogGroupingUtils.firstNonBlank(primaryGame.getUrl(), variantGame.getUrl())));
        primaryGame.setReleaseDate(resolveAdminManagedValue(primaryGame, AdminGameField.RELEASE_DATE, primaryGame.getReleaseDate(),
                CatalogGroupingUtils.firstNonBlank(primaryGame.getReleaseDate(), variantGame.getReleaseDate())));
        primaryGame.setProviderName(resolveAdminManagedValue(primaryGame, AdminGameField.PROVIDER_NAME, primaryGame.getProviderName(),
                CatalogGroupingUtils.pickPreferredText(primaryGame.getProviderName(), variantGame.getProviderName())));
        primaryGame.setImageUrl(resolveAdminManagedValue(primaryGame, AdminGameField.IMAGE_URL, primaryGame.getImageUrl(),
                CatalogGroupingUtils.firstNonBlank(primaryGame.getImageUrl(), variantGame.getImageUrl())));
        primaryGame.setNormalizedTitleKey(CatalogGroupingUtils.buildNormalizedTitleKey(primaryGame));
        primaryGame.setCatalogGroupKey(CatalogGroupingUtils.buildGroupKey(primaryGame));
        return primaryGame;
    }

    private String resolveAdminManagedValue(final GameData target,
                                            final AdminGameField field,
                                            final String currentValue,
                                            final String incomingValue) {
        return AdminOverrideUtils.isLocked(target.getAdminLockedFields(), field)
                ? defaultString(currentValue)
                : defaultString(incomingValue);
    }

    private void addToNormalizedTitleLookup(final Map<String, List<GameData>> existingGamesByNormalizedTitleKey,
                                            final GameData gameData) {
        if (gameData == null || gameData.getNormalizedTitleKey() == null || gameData.getNormalizedTitleKey().isBlank()) {
            return;
        }

        final List<GameData> games = existingGamesByNormalizedTitleKey.computeIfAbsent(gameData.getNormalizedTitleKey(), ignored -> new ArrayList<>());
        if (games.stream().noneMatch(existingGame -> Objects.equals(existingGame.getId(), gameData.getId()))) {
            games.add(gameData);
        }
    }

    private GameData selectExistingCanonicalGame(final GameData leftGame, final GameData rightGame) {
        if (leftGame.getCreated() == null) {
            return rightGame;
        }
        if (rightGame.getCreated() == null) {
            return leftGame;
        }
        return leftGame.getCreated().isBefore(rightGame.getCreated()) ? leftGame : rightGame;
    }

    private GameSourceEntry selectCanonicalSourceEntry(final GameSourceEntry leftEntry, final GameSourceEntry rightEntry) {
        final Long leftGameId = leftEntry.getGame() == null ? null : leftEntry.getGame().getId();
        final Long rightGameId = rightEntry.getGame() == null ? null : rightEntry.getGame().getId();

        if (leftGameId == null) {
            return rightEntry;
        }
        if (rightGameId == null) {
            return leftEntry;
        }
        return leftGameId <= rightGameId ? leftEntry : rightEntry;
    }

    private record IncomingAggregate(GameData game, String primaryExternalId, LinkedHashSet<String> externalIds) {
        private static IncomingAggregate fromImportGame(final CatalogImportGame importGame, final CatalogSource catalogSource) {
            final GameData game = GameData.builder()
                    .catalogSources(catalogSource.name())
                    .name(importGame.getName())
                    .description(defaultString(importGame.getDescription()))
                    .categories(defaultString(importGame.getCategories()))
                    .platforms(CatalogGroupingUtils.mergePlatformValues(defaultString(importGame.getPlatforms())))
                    .price(defaultString(importGame.getPrice()))
                    .url(defaultString(importGame.getUrl()))
                    .releaseDate(defaultString(importGame.getReleaseDate()))
                    .providerName(defaultString(importGame.getProviderName()))
                    .imageUrl(defaultString(importGame.getImageUrl()))
                    .build();
            game.setNormalizedTitleKey(CatalogGroupingUtils.buildNormalizedTitleKey(game));
            game.setCatalogGroupKey(CatalogGroupingUtils.buildGroupKey(game));
            final LinkedHashSet<String> externalIds = new LinkedHashSet<>();
            if (importGame.getExternalId() != null && !importGame.getExternalId().isBlank()) {
                externalIds.add(importGame.getExternalId());
            }
            return new IncomingAggregate(game, importGame.getExternalId(), externalIds);
        }
    }

    private static String defaultString(final String value) {
        return value == null ? "" : value;
    }
}
