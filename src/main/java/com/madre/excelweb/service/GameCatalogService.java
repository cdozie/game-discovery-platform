package com.madre.excelweb.service;

import com.madre.excelweb.api.model.dto.CatalogDashboardDto;
import com.madre.excelweb.api.model.dto.CatalogDiscoveryDto;
import com.madre.excelweb.api.model.dto.CatalogSyncRunDto;
import com.madre.excelweb.api.model.dto.GameDataInteractionsDto;
import com.madre.excelweb.api.model.dto.GameDto;
import com.madre.excelweb.api.model.dto.GameSearchPageDto;
import com.madre.excelweb.jpa.entity.AdminCatalogSettings;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.AdminCatalogSettingsRepository;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.rest.ControllerUtils;
import com.madre.excelweb.service.util.CatalogGroupingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GameCatalogService {
    private final AdminCatalogSettingsRepository adminCatalogSettingsRepository;
    private final GameRepository gameRepository;
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ControllerUtils controllerUtils;
    private final CatalogSyncCoordinator catalogSyncCoordinator;
    private final PublicEntityLookupService publicEntityLookupService;

    public CatalogDashboardDto getDashboard(final AppUser appUser, final Integer latestLimit, final Long popularLimit) {
        final int resolvedLatestLimit = clampInt(latestLimit, 1, 24, 8);
        final long resolvedPopularLimit = clampLong(popularLimit, 1, 48, 12);

        return new CatalogDashboardDto(
                gameRepository.count(),
                userRepository.count(),
                wishlistRepository.count(),
                catalogSyncCoordinator.isAnySyncInProgress(),
                catalogSyncCoordinator.getLatestRun().map(CatalogSyncRunDto::fromEntity).orElse(null),
                getLatestGames(appUser, resolvedLatestLimit),
                getPopularDashboardGames(appUser, resolvedPopularLimit)
        );
    }

    public CatalogDiscoveryDto getDiscovery(final AppUser appUser) {
        final List<GameDto> featuredGames = toGameDtos(appUser, gameRepository.findAllByOrderByInteractionsDesc(8L, null, null));
        final List<GameDto> newAndNoteworthy = getLatestGames(appUser, 8);
        final List<GameDto> communityFavorites = toGameDtos(appUser, gameRepository.findAllByOrderByWishlistTotalDesc(8));
        final List<GameDto> surpriseGames = toGameDtos(appUser, gameRepository.findRandomGames(8));
        final String preferredPlatform = appUser == null ? null : appUser.getFavoritePlatform();
        final GameDto spotlightGame = getAdminSpotlight(appUser)
                .orElseGet(() -> pickSpotlight(preferredPlatform, featuredGames, communityFavorites, newAndNoteworthy, surpriseGames));

        return new CatalogDiscoveryDto(
                preferredPlatform,
                spotlightGame,
                featuredGames,
                newAndNoteworthy,
                communityFavorites,
                surpriseGames
        );
    }

    public List<GameDto> getLatestGames(final AppUser appUser, final Integer limit) {
        final int resolvedLimit = clampInt(limit, 1, 40, 12);
        return gameRepository.findAllByOrderByCreatedDesc(PageRequest.of(0, resolvedLimit)).stream()
                .map(gameData -> controllerUtils.gameDataToGameDto(appUser, gameData))
                .toList();
    }

    public List<GameDataInteractionsDto> getPopularGames(final AppUser appUser,
                                                         final Long limit,
                                                         final Long lastId,
                                                         final Long lastInteractions) {
        final long resolvedLimit = clampLong(limit, 1, 100, 24);
        return gameRepository.findAllByOrderByInteractionsDesc(resolvedLimit, lastId, lastInteractions).stream()
                .map(game -> GameDataInteractionsDto.builder()
                        .id(game.getPublicId())
                        .name(game.getName())
                        .catalogSources(game.getCatalogSources())
                        .url(game.getUrl())
                        .price(game.getPrice())
                        .releaseDate(game.getReleaseDate())
                        .description(game.getDescription())
                        .categories(game.getCategories())
                        .platforms(game.getPlatforms())
                        .provider(game.getProviderName())
                        .imageUrl(game.getImageUrl())
                        .interactions(String.valueOf(game.getInteractions()))
                        .inWishlist(controllerUtils.gameInWishlist(appUser, game))
                        .build())
                .toList();
    }

    public List<GameDto> getPopularDashboardGames(final AppUser appUser, final Long limit) {
        final long resolvedLimit = clampLong(limit, 1, 48, 12);
        return gameRepository.findAllByOrderByInteractionsDesc(resolvedLimit, null, null).stream()
                .map(gameData -> controllerUtils.gameDataToGameDto(appUser, gameData))
                .toList();
    }

    public Optional<GameDto> getGame(final AppUser appUser, final String gameId) {
        return publicEntityLookupService.findGame(gameId)
                .map(gameData -> controllerUtils.gameDataToGameDto(appUser, gameData));
    }

    public Optional<GameData> getGameEntity(final String gameId) {
        return publicEntityLookupService.findGame(gameId);
    }

    public List<GameDto> searchGames(final AppUser appUser,
                                     final String query,
                                     final Integer limit,
                                     final String platform,
                                     final String category) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        final int resolvedLimit = clampInt(limit, 1, 50, 20);
        final String normalizedQuery = normalizeSearchQuery(query);
        final String likeQuery = escapeLikePattern(normalizedQuery);
        final String normalizedPlatform = normalizeOptionalFilter(platform);
        final String normalizedCategory = normalizeOptionalFilter(category);

        return gameRepository.searchByQuery(
                        normalizedQuery,
                        likeQuery,
                        normalizedPlatform,
                        normalizedCategory,
                        null,
                        false,
                        null,
                        false,
                        false,
                        "RELEVANCE",
                        resolvedLimit
                ).stream()
                .map(gameData -> controllerUtils.gameDataToGameDto(appUser, gameData))
                .toList();
    }

    public GameSearchPageDto searchGamesPage(final AppUser appUser,
                                             final String query,
                                             final Integer limit,
                                             final Integer offset,
                                             final String platform,
                                             final String category,
                                             final String publisher,
                                             final Boolean publisherExact,
                                             final String catalogSource,
                                             final Boolean hasDescription,
                                             final Boolean hasImage,
                                             final String sort) {
        final boolean hasTextQuery = query != null && !query.isBlank();
        final String normalizedPlatform = normalizeOptionalFilter(platform);
        final String normalizedCategory = normalizeOptionalFilter(category);
        final String normalizedPublisher = normalizeOptionalFilter(publisher);
        final boolean requireExactPublisher = Boolean.TRUE.equals(publisherExact) && normalizedPublisher != null;
        final String normalizedCatalogSource = normalizeCatalogSource(catalogSource);
        final boolean requireDescription = Boolean.TRUE.equals(hasDescription);
        final boolean requireImage = Boolean.TRUE.equals(hasImage);
        final boolean hasAnyFilters = Stream.of(normalizedPlatform, normalizedCategory, normalizedPublisher, normalizedCatalogSource)
                .anyMatch(value -> value != null && !value.isBlank())
                || requireDescription
                || requireImage;
        if (!hasTextQuery && !hasAnyFilters) {
            return new GameSearchPageDto("", 0, clampInt(limit, 1, 48, 24), false, List.of());
        }

        final int resolvedLimit = clampInt(limit, 1, 48, 24);
        final int resolvedOffset = Math.max(0, offset == null ? 0 : offset);
        final String normalizedQuery = hasTextQuery ? normalizeSearchQuery(query) : "";
        final String likeQuery = escapeLikePattern(normalizedQuery);
        final String normalizedSort = normalizeSort(sort, hasTextQuery);
        final List<GameData> fetchedGames = gameRepository.searchByQuery(
                normalizedQuery,
                likeQuery,
                normalizedPlatform,
                normalizedCategory,
                normalizedPublisher,
                requireExactPublisher,
                normalizedCatalogSource,
                requireDescription,
                requireImage,
                normalizedSort,
                resolvedLimit + 1,
                resolvedOffset
        );
        final List<GameDto> items = fetchedGames.stream()
                .limit(resolvedLimit)
                .map(gameData -> controllerUtils.gameDataToGameDto(appUser, gameData))
                .toList();
        final boolean hasMore = fetchedGames.size() > resolvedLimit;

        return new GameSearchPageDto(normalizedQuery, resolvedOffset, resolvedLimit, hasMore, items);
    }

    public Optional<String> getRandomGameId() {
        return gameRepository.findRandomGameId()
                .flatMap(gameRepository::findGameById)
                .map(GameData::getPublicId);
    }

    private List<GameDto> toGameDtos(final AppUser appUser, final List<GameData> games) {
        return games.stream()
                .map(game -> controllerUtils.gameDataToGameDto(appUser, game))
                .toList();
    }

    private Optional<GameDto> getAdminSpotlight(final AppUser appUser) {
        return adminCatalogSettingsRepository.findById(AdminCatalogSettings.SINGLETON_ID)
                .map(AdminCatalogSettings::getSpotlightGame)
                .flatMap(game -> game == null || game.getId() == null ? Optional.<GameData>empty() : gameRepository.findGameById(game.getId()))
                .map(game -> controllerUtils.gameDataToGameDto(appUser, game));
    }

    private GameDto pickSpotlight(final String preferredPlatform,
                                  final List<GameDto> featuredGames,
                                  final List<GameDto> communityFavorites,
                                  final List<GameDto> newAndNoteworthy,
                                  final List<GameDto> surpriseGames) {
        if (preferredPlatform != null && !preferredPlatform.isBlank()) {
            final Optional<GameDto> matchingPreferredPlatform = Stream.of(featuredGames, communityFavorites, newAndNoteworthy, surpriseGames)
                    .flatMap(List::stream)
                    .filter(game -> matchesPreferredPlatform(game, preferredPlatform))
                    .findFirst();
            if (matchingPreferredPlatform.isPresent()) {
                return matchingPreferredPlatform.get();
            }
        }

        return Stream.of(communityFavorites, featuredGames, newAndNoteworthy, surpriseGames)
                .flatMap(List::stream)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesPreferredPlatform(final GameDto game, final String preferredPlatform) {
        if (game == null || game.getPlatforms() == null || preferredPlatform == null || preferredPlatform.isBlank()) {
            return false;
        }

        final String normalizedPreference = CatalogGroupingUtils.canonicalizePlatformValue(preferredPlatform);
        return CatalogGroupingUtils.parseDelimitedValues(game.getPlatforms()).stream()
                .anyMatch(platform -> platform.equalsIgnoreCase(normalizedPreference));
    }

    private String normalizeSearchQuery(final String query) {
        return query.trim().replaceAll("\\s+", " ");
    }

    private String escapeLikePattern(final String query) {
        return query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private String normalizeOptionalFilter(final String filterValue) {
        if (filterValue == null || filterValue.isBlank()) {
            return null;
        }
        return filterValue.trim();
    }

    private String normalizeCatalogSource(final String catalogSource) {
        if (catalogSource == null || catalogSource.isBlank()) {
            return null;
        }

        return switch (catalogSource.trim().toUpperCase()) {
            case "PLAYSTATION_STORE", "IGDB_PC_XBOX", "USER_SUBMITTED" -> catalogSource.trim().toUpperCase();
            default -> null;
        };
    }

    private String normalizeSort(final String sort, final boolean hasTextQuery) {
        if (sort == null || sort.isBlank()) {
            return hasTextQuery ? "RELEVANCE" : "NEWEST";
        }

        return switch (sort.trim().toUpperCase()) {
            case "RELEVANCE" -> hasTextQuery ? "RELEVANCE" : "NEWEST";
            case "NEWEST", "RECENTLY_UPDATED", "POPULAR", "WISHLIST", "ALPHABETICAL" -> sort.trim().toUpperCase();
            default -> hasTextQuery ? "RELEVANCE" : "NEWEST";
        };
    }

    private int clampInt(final Integer value, final int min, final int max, final int fallback) {
        final int resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private long clampLong(final Long value, final long min, final long max, final long fallback) {
        final long resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }
}
