package com.madre.excelweb.service;

import com.madre.excelweb.jpa.entity.GameComment;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.GameSourceEntry;
import com.madre.excelweb.jpa.entity.Wishlist;
import com.madre.excelweb.jpa.repositories.GameCommentRepository;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.GameSourceEntryRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.models.ListGameStatus;
import com.madre.excelweb.service.util.CatalogGroupingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogConsolidationService {
    private static final String CONSOLIDATION_BATCH_SIZE = "web.app.catalog.consolidation.batch-size";
    private static final int DEFAULT_CONSOLIDATION_BATCH_SIZE = 100;

    private final GameRepository gameRepository;
    private final WishlistRepository wishlistRepository;
    private final GameCommentRepository gameCommentRepository;
    private final GameSourceEntryRepository gameSourceEntryRepository;
    private final TransactionTemplate catalogConsolidationTransactionTemplate;
    private final Environment environment;

    @Transactional
    public void backfillCatalogGroupKeys() {
        final List<GameData> games = gameRepository.findAll();
        final List<GameData> gamesToUpdate = games.stream()
                .filter(Objects::nonNull)
                .filter(gameData -> {
                    final String nextPlatforms = CatalogGroupingUtils.mergePlatformValues(gameData.getPlatforms());
                    final String nextTitleKey = CatalogGroupingUtils.buildNormalizedTitleKey(gameData);
                    final String nextGroupKey = CatalogGroupingUtils.buildGroupKey(gameData);
                    if (Objects.equals(gameData.getPlatforms(), nextPlatforms)
                            && Objects.equals(gameData.getCatalogGroupKey(), nextGroupKey)
                            && Objects.equals(gameData.getNormalizedTitleKey(), nextTitleKey)) {
                        return false;
                    }
                    gameData.setPlatforms(nextPlatforms);
                    gameData.setNormalizedTitleKey(nextTitleKey);
                    gameData.setCatalogGroupKey(nextGroupKey);
                    return true;
                })
                .toList();

        if (gamesToUpdate.isEmpty()) {
            log.info("Catalog group-key backfill skipped because all games are already keyed.");
            return;
        }

        gameRepository.saveAll(gamesToUpdate);
        log.info("Backfilled catalog group keys for {} games.", gamesToUpdate.size());
    }

    public void consolidateCatalogVariants() {
        final ConsolidationProgress totalProgress = new ConsolidationProgress();
        totalProgress.merge(consolidateByDuplicateGroupKeys());
        totalProgress.merge(consolidateByNormalizedTitleKeys());

        if (totalProgress.mergedGroups == 0) {
            log.info("Catalog consolidation skipped because no duplicate catalog groups were found.");
            return;
        }

        log.info(
                "Catalog consolidation complete. mergedGroups={}, removedGames={}, migratedWishlistEntries={}, migratedComments={}",
                totalProgress.mergedGroups,
                totalProgress.removedGames,
                totalProgress.migratedWishlistEntries,
                totalProgress.migratedComments
        );
    }

    private ConsolidationProgress consolidateByDuplicateGroupKeys() {
        final List<String> duplicateGroupKeys = gameRepository.findDuplicateCatalogGroupKeys();
        if (duplicateGroupKeys.isEmpty()) {
            return new ConsolidationProgress();
        }

        return runBatchedConsolidation(
                duplicateGroupKeys,
                batchKeys -> consolidateBatchByCatalogGroupKey(batchKeys),
                "catalog-group-key"
        );
    }

    private ConsolidationProgress consolidateByNormalizedTitleKeys() {
        final List<String> duplicateTitleKeys = gameRepository.findDuplicateNormalizedTitleKeys();
        if (duplicateTitleKeys.isEmpty()) {
            return new ConsolidationProgress();
        }

        return runBatchedConsolidation(
                duplicateTitleKeys,
                batchKeys -> consolidateBatchByNormalizedTitleKey(batchKeys),
                "normalized-title-key"
        );
    }

    private ConsolidationProgress runBatchedConsolidation(final List<String> keys,
                                                          final Function<List<String>, ConsolidationProgress> batchFunction,
                                                          final String mode) {
        final ConsolidationProgress totalProgress = new ConsolidationProgress();
        final int batchSize = resolveBatchSize();

        for (int startIndex = 0; startIndex < keys.size(); startIndex += batchSize) {
            final int endIndex = Math.min(startIndex + batchSize, keys.size());
            final List<String> batchKeys = keys.subList(startIndex, endIndex);
            final ConsolidationProgress batchProgress = catalogConsolidationTransactionTemplate.execute(status ->
                    batchFunction.apply(batchKeys));
            totalProgress.merge(batchProgress);
            log.info(
                    "Catalog consolidation progress [{}]. mergedGroups={}, removedGames={}, migratedWishlistEntries={}, migratedComments={}, processedBatches={}/{}",
                    mode,
                    totalProgress.mergedGroups,
                    totalProgress.removedGames,
                    totalProgress.migratedWishlistEntries,
                    totalProgress.migratedComments,
                    (startIndex / batchSize) + 1,
                    (int) Math.ceil((double) keys.size() / batchSize)
            );
        }

        return totalProgress;
    }

    @Transactional
    protected ConsolidationProgress consolidateBatchByCatalogGroupKey(final List<String> batchKeys) {
        final List<GameData> games = gameRepository.findByCatalogGroupKeyInOrderByCatalogGroupKeyAscCreatedAscIdAsc(batchKeys);
        final Map<String, List<GameData>> groupedGames = games.stream()
                .collect(Collectors.groupingBy(this::applyAndReturnGroupKey));
        final ConsolidationProgress progress = new ConsolidationProgress();

        for (List<GameData> groupedVariantGames : groupedGames.values()) {
            mergeGroupedVariants(progress, groupedVariantGames);
        }

        return progress;
    }

    @Transactional
    protected ConsolidationProgress consolidateBatchByNormalizedTitleKey(final List<String> batchKeys) {
        final List<GameData> games = gameRepository.findByNormalizedTitleKeyInOrderByNormalizedTitleKeyAscCreatedAscIdAsc(batchKeys);
        final Map<String, List<GameData>> groupedGames = games.stream()
                .collect(Collectors.groupingBy(this::applyAndReturnNormalizedTitleKey));
        final ConsolidationProgress progress = new ConsolidationProgress();

        for (List<GameData> groupedVariantGames : groupedGames.values()) {
            final List<List<GameData>> mergeClusters = buildSafeMergeClusters(groupedVariantGames);
            for (List<GameData> mergeCluster : mergeClusters) {
                mergeGroupedVariants(progress, mergeCluster);
            }
        }

        return progress;
    }

    private int resolveBatchSize() {
        return Math.max(1, environment.getProperty(CONSOLIDATION_BATCH_SIZE, Integer.class, DEFAULT_CONSOLIDATION_BATCH_SIZE));
    }

    private String applyAndReturnGroupKey(final GameData gameData) {
        final String groupKey = CatalogGroupingUtils.buildGroupKey(gameData);
        gameData.setCatalogGroupKey(groupKey);
        return groupKey;
    }

    private String applyAndReturnNormalizedTitleKey(final GameData gameData) {
        final String normalizedTitleKey = CatalogGroupingUtils.buildNormalizedTitleKey(gameData);
        gameData.setNormalizedTitleKey(normalizedTitleKey);
        return normalizedTitleKey;
    }

    private List<List<GameData>> buildSafeMergeClusters(final List<GameData> groupedVariantGames) {
        final List<List<GameData>> clusters = new ArrayList<>();

        for (GameData candidateGame : groupedVariantGames) {
            boolean addedToExistingCluster = false;
            for (List<GameData> cluster : clusters) {
                if (cluster.stream().anyMatch(existingGame -> CatalogGroupingUtils.shouldMergeByTitleIdentity(existingGame, candidateGame))) {
                    cluster.add(candidateGame);
                    addedToExistingCluster = true;
                    break;
                }
            }

            if (!addedToExistingCluster) {
                final List<GameData> nextCluster = new ArrayList<>();
                nextCluster.add(candidateGame);
                clusters.add(nextCluster);
            }
        }

        return clusters;
    }

    private void mergeGroupedVariants(final ConsolidationProgress progress, final List<GameData> groupedVariantGames) {
        if (groupedVariantGames.size() < 2) {
            return;
        }

        groupedVariantGames.sort(Comparator
                .comparing(GameData::getCreated, Comparator.nullsLast(OffsetDateTime::compareTo))
                .thenComparing(GameData::getId, Comparator.nullsLast(Long::compareTo)));

        final GameData primaryGame = groupedVariantGames.get(0);
        final List<GameData> duplicateGames = groupedVariantGames.subList(1, groupedVariantGames.size());

        mergeGameMetadata(primaryGame, groupedVariantGames);
        progress.migratedWishlistEntries += mergeWishlistEntries(primaryGame, groupedVariantGames);
        progress.migratedComments += migrateComments(primaryGame, duplicateGames);
        migrateSourceEntries(primaryGame, groupedVariantGames);

        gameRepository.save(primaryGame);
        gameRepository.flush();
        gameRepository.deleteAllInBatch(duplicateGames);

        progress.mergedGroups++;
        progress.removedGames += duplicateGames.size();
    }

    private void mergeGameMetadata(final GameData primaryGame, final List<GameData> groupedVariantGames) {
        primaryGame.setName(CatalogGroupingUtils.pickPreferredTitle(groupedVariantGames.stream()
                .map(GameData::getName)
                .toArray(String[]::new)));
        primaryGame.setCatalogSources(CatalogGroupingUtils.mergeDelimitedValues(groupedVariantGames.stream()
                .map(GameData::getCatalogSources)
                .toList()));
        primaryGame.setPlatforms(CatalogGroupingUtils.mergePlatformValues(groupedVariantGames.stream()
                .map(GameData::getPlatforms)
                .toList()));
        primaryGame.setCategories(CatalogGroupingUtils.mergeDelimitedValues(groupedVariantGames.stream()
                .map(GameData::getCategories)
                .toList()));
        primaryGame.setDescription(CatalogGroupingUtils.pickPreferredText(groupedVariantGames.stream()
                .map(GameData::getDescription)
                .toArray(String[]::new)));
        primaryGame.setPrice(resolvePreferredPlayStationBackedValue(groupedVariantGames, GameData::getPrice));
        primaryGame.setUrl(resolvePreferredPlayStationBackedValue(groupedVariantGames, GameData::getUrl));
        primaryGame.setReleaseDate(resolvePreferredPlayStationBackedValue(groupedVariantGames, GameData::getReleaseDate));
        primaryGame.setProviderName(CatalogGroupingUtils.pickPreferredText(groupedVariantGames.stream()
                .map(GameData::getProviderName)
                .toArray(String[]::new)));
        primaryGame.setImageUrl(resolvePreferredImage(groupedVariantGames));
        primaryGame.setInteractions(groupedVariantGames.stream().map(GameData::getInteractions).filter(Objects::nonNull).mapToLong(Long::longValue).sum());
        primaryGame.setPosts(groupedVariantGames.stream().map(GameData::getPosts).filter(Objects::nonNull).mapToLong(Long::longValue).sum());
        primaryGame.setCreated(groupedVariantGames.stream()
                .map(GameData::getCreated)
                .filter(Objects::nonNull)
                .min(OffsetDateTime::compareTo)
                .orElse(primaryGame.getCreated()));
        primaryGame.setLastModified(OffsetDateTime.now(ZoneOffset.UTC));
        primaryGame.setNormalizedTitleKey(CatalogGroupingUtils.buildNormalizedTitleKey(primaryGame));
        primaryGame.setCatalogGroupKey(CatalogGroupingUtils.buildGroupKey(primaryGame));
    }

    private int mergeWishlistEntries(final GameData primaryGame, final List<GameData> groupedVariantGames) {
        final List<Long> gameIds = groupedVariantGames.stream()
                .map(GameData::getId)
                .filter(Objects::nonNull)
                .toList();
        final List<Wishlist> wishlistEntries = wishlistRepository.findByGameinfoIdIn(gameIds);
        if (wishlistEntries.isEmpty()) {
            return 0;
        }

        final Map<Long, List<Wishlist>> wishlistEntriesByUserId = wishlistEntries.stream()
                .filter(entry -> entry.getUserinfo() != null && entry.getUserinfo().getId() != null)
                .collect(Collectors.groupingBy(entry -> entry.getUserinfo().getId()));

        final List<Wishlist> wishlistEntriesToSave = new ArrayList<>();
        final List<Wishlist> wishlistEntriesToDelete = new ArrayList<>();

        for (List<Wishlist> userEntries : wishlistEntriesByUserId.values()) {
            final Wishlist entryToKeep = selectWishlistEntryToKeep(primaryGame, userEntries);
            entryToKeep.setGameinfo(primaryGame);
            entryToKeep.setStatus(resolveMergedStatus(userEntries));
            entryToKeep.setRating(resolveMergedRating(userEntries));
            entryToKeep.setSelectedPlatform(resolveMergedSelectedPlatform(userEntries));
            wishlistEntriesToSave.add(entryToKeep);

            userEntries.stream()
                    .filter(entry -> !Objects.equals(entry.getId(), entryToKeep.getId()))
                    .forEach(wishlistEntriesToDelete::add);
        }

        wishlistRepository.saveAll(wishlistEntriesToSave);
        wishlistRepository.flush();
        if (!wishlistEntriesToDelete.isEmpty()) {
            wishlistRepository.deleteAllInBatch(wishlistEntriesToDelete);
        }

        return wishlistEntriesToSave.size();
    }

    private Wishlist selectWishlistEntryToKeep(final GameData primaryGame, final List<Wishlist> wishlistEntries) {
        return wishlistEntries.stream()
                .sorted(Comparator
                        .comparing((Wishlist entry) -> !Objects.equals(entry.getGameinfo().getId(), primaryGame.getId()))
                        .thenComparing(this::statusPriority)
                        .thenComparing(entry -> entry.getRating() == null)
                        .thenComparing(Wishlist::getId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not determine a wishlist entry to keep."));
    }

    private int statusPriority(final Wishlist wishlistEntry) {
        final Map<ListGameStatus, Integer> priorities = new EnumMap<>(ListGameStatus.class);
        priorities.put(ListGameStatus.COMPLETED, 0);
        priorities.put(ListGameStatus.PLAYING, 1);
        priorities.put(ListGameStatus.REPLAYING, 2);
        priorities.put(ListGameStatus.ON_HOLD, 3);
        priorities.put(ListGameStatus.PLAN_TO_PLAY, 4);
        priorities.put(ListGameStatus.WANTED, 5);
        priorities.put(ListGameStatus.WATCHING, 6);
        priorities.put(ListGameStatus.DROPPED, 7);
        return priorities.getOrDefault(wishlistEntry.getStatus(), 99);
    }

    private ListGameStatus resolveMergedStatus(final List<Wishlist> wishlistEntries) {
        return wishlistEntries.stream()
                .min(Comparator.comparingInt(this::statusPriority))
                .map(Wishlist::getStatus)
                .orElse(ListGameStatus.PLAN_TO_PLAY);
    }

    private Double resolveMergedRating(final List<Wishlist> wishlistEntries) {
        return wishlistEntries.stream()
                .map(Wishlist::getRating)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);
    }

    private String resolveMergedSelectedPlatform(final List<Wishlist> wishlistEntries) {
        final Set<String> selectedPlatforms = new LinkedHashSet<>();
        wishlistEntries.stream()
                .map(Wishlist::getSelectedPlatform)
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> CatalogGroupingUtils.parseDelimitedValues(value).stream())
                .forEach(selectedPlatforms::add);

        if (selectedPlatforms.isEmpty()) {
            wishlistEntries.stream()
                    .map(Wishlist::getGameinfo)
                    .filter(Objects::nonNull)
                    .map(GameData::getPlatforms)
                    .flatMap(value -> CatalogGroupingUtils.parseDelimitedValues(value).stream())
                    .forEach(selectedPlatforms::add);
        }

        return CatalogGroupingUtils.mergePlatformValues(selectedPlatforms);
    }

    private int migrateComments(final GameData primaryGame, final Collection<GameData> duplicateGames) {
        final List<Long> duplicateGameIds = duplicateGames.stream()
                .map(GameData::getId)
                .filter(Objects::nonNull)
                .toList();
        if (duplicateGameIds.isEmpty()) {
            return 0;
        }

        final List<GameComment> commentsToMove = gameCommentRepository.findByGameIdIn(duplicateGameIds);
        commentsToMove.forEach(comment -> comment.setGame(primaryGame));
        if (!commentsToMove.isEmpty()) {
            gameCommentRepository.saveAll(commentsToMove);
            gameCommentRepository.flush();
        }

        return commentsToMove.size();
    }

    private void migrateSourceEntries(final GameData primaryGame, final Collection<GameData> groupedVariantGames) {
        final List<Long> gameIds = groupedVariantGames.stream()
                .map(GameData::getId)
                .filter(Objects::nonNull)
                .toList();
        if (gameIds.isEmpty()) {
            return;
        }

        final List<GameSourceEntry> sourceEntries = gameSourceEntryRepository.findByGameIdIn(gameIds);
        if (sourceEntries.isEmpty()) {
            return;
        }

        sourceEntries.forEach(sourceEntry -> sourceEntry.setGame(primaryGame));
        gameSourceEntryRepository.saveAll(sourceEntries);
        gameSourceEntryRepository.flush();
    }

    private String resolvePreferredImage(final Collection<GameData> groupedVariantGames) {
        final String playStationImage = groupedVariantGames.stream()
                .filter(gameData -> CatalogGroupingUtils.containsDelimitedValue(gameData.getCatalogSources(), "PLAYSTATION_STORE"))
                .map(GameData::getImageUrl)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");

        if (!playStationImage.isBlank()) {
            return playStationImage;
        }

        return groupedVariantGames.stream()
                .map(GameData::getImageUrl)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private String resolvePreferredPlayStationBackedValue(final Collection<GameData> groupedVariantGames,
                                                          final Function<GameData, String> valueExtractor) {
        final String playStationValue = groupedVariantGames.stream()
                .filter(gameData -> CatalogGroupingUtils.containsDelimitedValue(gameData.getCatalogSources(), "PLAYSTATION_STORE"))
                .map(valueExtractor)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");

        if (!playStationValue.isBlank()) {
            return playStationValue;
        }

        return groupedVariantGames.stream()
                .map(valueExtractor)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private static final class ConsolidationProgress {
        private int mergedGroups;
        private int removedGames;
        private int migratedWishlistEntries;
        private int migratedComments;

        private void merge(final ConsolidationProgress other) {
            if (other == null) {
                return;
            }
            mergedGroups += other.mergedGroups;
            removedGames += other.removedGames;
            migratedWishlistEntries += other.migratedWishlistEntries;
            migratedComments += other.migratedComments;
        }
    }
}
