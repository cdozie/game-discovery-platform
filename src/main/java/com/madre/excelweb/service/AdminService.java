package com.madre.excelweb.service;

import com.madre.excelweb.api.model.AdminBanUserRequest;
import com.madre.excelweb.api.model.AdminGameRequest;
import com.madre.excelweb.api.model.dto.AdminDashboardDto;
import com.madre.excelweb.api.model.dto.AdminGameDto;
import com.madre.excelweb.api.model.dto.AdminUserDto;
import com.madre.excelweb.jpa.entity.AdminCatalogSettings;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.AdminCatalogSettingsRepository;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.GameSourceEntryRepository;
import com.madre.excelweb.jpa.repositories.GameSubmissionRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.models.AdminAuditAction;
import com.madre.excelweb.models.AdminAuditEntityType;
import com.madre.excelweb.models.AdminGameField;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.models.GameSubmissionStatus;
import com.madre.excelweb.models.MediaPurpose;
import com.madre.excelweb.service.util.AdminOverrideUtils;
import com.madre.excelweb.service.util.CatalogGroupingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final int MAX_REMOTE_GAME_IMAGE_LENGTH = 4_096;
    private static final int MAX_INLINE_GAME_IMAGE_LENGTH = 2_500_000;
    private static final int MAX_INLINE_GIF_GAME_IMAGE_BYTES = 30 * 1024 * 1024;

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GameSubmissionRepository gameSubmissionRepository;
    private final AdminCatalogSettingsRepository adminCatalogSettingsRepository;
    private final CatalogUpsertService catalogUpsertService;
    private final GameSourceEntryRepository gameSourceEntryRepository;
    private final MediaAssetService mediaAssetService;
    private final AdminAuditService adminAuditService;
    private final PublicEntityLookupService publicEntityLookupService;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        final long activeBans = userRepository.findAll().stream()
                .filter(AppUser::isCurrentlyBanned)
                .count();
        final AdminCatalogSettings settings = adminCatalogSettingsRepository.findById(AdminCatalogSettings.SINGLETON_ID)
                .orElse(null);
        return new AdminDashboardDto(
                userRepository.count(),
                activeBans,
                gameRepository.count(),
                gameSubmissionRepository.countByStatus(GameSubmissionStatus.PENDING),
                gameSubmissionRepository.countByStatus(GameSubmissionStatus.APPROVED),
                gameSubmissionRepository.countByStatus(GameSubmissionStatus.REJECTED),
                settings == null || settings.getSpotlightGame() == null
                        ? null
                        : settings.getSpotlightGame().getPublicId(),
                settings == null || settings.getSpotlightGame() == null
                        ? null
                        : settings.getSpotlightGame().getName()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> getUsers(final String query, final boolean onlyBanned) {
        final String normalizedQuery = normalizeQuery(query);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(user -> !onlyBanned || user.isCurrentlyBanned())
                .filter(user -> normalizedQuery.isBlank() || matchesUserQuery(user, normalizedQuery))
                .map(AdminUserDto::fromEntity)
                .toList();
    }

    @Transactional
    public AdminUserDto banUser(final AppUser adminUser, final String userId, final AdminBanUserRequest request) {
        final AppUser targetUser = publicEntityLookupService.requireUser(userId);
        if (Objects.equals(adminUser.getId(), targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins cannot ban themselves.");
        }

        final boolean permanent = Boolean.TRUE.equals(request.permanent());
        final Integer durationDays = request.durationDays();
        if (!permanent && (durationDays == null || durationDays < 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ban length must be at least 1 day unless the ban is permanent.");
        }

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        targetUser.setBanStartedAt(now);
        targetUser.setBanPermanent(permanent);
        targetUser.setBanExpiresAt(permanent ? null : now.plusDays(durationDays));
        targetUser.setBanReason(normalizeText(request.reason()));
        targetUser.setBannedByUserId(adminUser.getId());
        final AppUser savedUser = userRepository.save(targetUser);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.BAN_USER,
                AdminAuditEntityType.USER,
                savedUser.getPublicId(),
                "Banned user '" + savedUser.getUsername() + "'.",
                java.util.Map.of(
                        "permanent", permanent,
                        "durationDays", permanent ? "" : String.valueOf(durationDays),
                        "reason", savedUser.getBanReason() == null ? "" : savedUser.getBanReason()
                )
        );
        return AdminUserDto.fromEntity(savedUser);
    }

    @Transactional
    public AdminUserDto unbanUser(final AppUser adminUser, final String userId) {
        final AppUser targetUser = publicEntityLookupService.requireUser(userId);
        targetUser.setBanStartedAt(null);
        targetUser.setBanExpiresAt(null);
        targetUser.setBanPermanent(false);
        targetUser.setBanReason(null);
        targetUser.setBannedByUserId(null);
        final AppUser savedUser = userRepository.save(targetUser);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.UNBAN_USER,
                AdminAuditEntityType.USER,
                savedUser.getPublicId(),
                "Removed ban from user '" + savedUser.getUsername() + "'.",
                java.util.Map.of()
        );
        return AdminUserDto.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public List<AdminGameDto> getGames(final String query, final Integer limit) {
        final int resolvedLimit = Math.max(1, Math.min(limit == null ? 24 : limit, 50));
        if (query == null || query.isBlank()) {
            return gameRepository.findAllByOrderByCreatedDesc(PageRequest.of(0, resolvedLimit)).stream()
                    .map(AdminGameDto::fromEntity)
                    .toList();
        }

        final String normalizedQuery = query.trim().replaceAll("\\s+", " ");
        final String likeQuery = normalizedQuery
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return gameRepository.searchByQuery(normalizedQuery, likeQuery, null, null, null, false, null, false, false, "RELEVANCE", resolvedLimit).stream()
                .map(AdminGameDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminGameDto getGame(final String gameId) {
        return AdminGameDto.fromEntity(publicEntityLookupService.requireGame(gameId));
    }

    @Transactional
    public AdminGameDto createGame(final AppUser adminUser, final AdminGameRequest request) {
        final String externalId = "ADMIN:" + UUID.randomUUID();
        final CatalogImportGame importGame = CatalogImportGame.builder()
                .externalId(externalId)
                .name(requireName(request.name()))
                .description(normalizeText(request.description()))
                .categories(normalizeDelimited(request.categories()))
                .platforms(requirePlatforms(request.platforms()))
                .price(normalizeText(request.price()))
                .url(normalizeText(request.url()))
                .releaseDate(normalizeText(request.releaseDate()))
                .providerName(normalizeText(request.providerName()))
                .imageUrl(normalizeImageUrl(request.imageUrl(), adminUser == null ? null : adminUser.getId()))
                .build();
        catalogUpsertService.upsertBatch(List.of(importGame), CatalogSource.USER_SUBMITTED);
        final GameData createdGame = gameSourceEntryRepository.findByCatalogSourceAndExternalId(CatalogSource.USER_SUBMITTED, externalId)
                .map(sourceEntry -> sourceEntry.getGame())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Created game could not be loaded."));

        final Set<AdminGameField> protectedFields = request.syncProtectedFields() == null || request.syncProtectedFields().isEmpty()
                ? Set.of(AdminGameField.values())
                : parseFields(request.syncProtectedFields());
        createdGame.setAdminLockedFields(AdminOverrideUtils.serializeLockedFields(protectedFields));
        createdGame.setAdminLastModifiedByUserId(adminUser.getId());
        createdGame.setAdminLastModifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        final GameData savedGame = gameRepository.save(createdGame);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.CREATE_GAME,
                AdminAuditEntityType.GAME,
                savedGame.getPublicId(),
                "Created catalog game '" + savedGame.getName() + "'.",
                java.util.Map.of(
                        "catalogSources", savedGame.getCatalogSources(),
                        "platforms", savedGame.getPlatforms(),
                        "providerName", savedGame.getProviderName() == null ? "" : savedGame.getProviderName()
                )
        );
        return AdminGameDto.fromEntity(savedGame);
    }

    @Transactional
    public AdminGameDto updateGame(final AppUser adminUser, final String gameId, final AdminGameRequest request) {
        final GameData game = publicEntityLookupService.requireGame(gameId);

        final String nextName = request.name() == null ? game.getName() : requireName(request.name());
        final String nextDescription = request.description() == null ? game.getDescription() : normalizeText(request.description());
        final String nextCategories = request.categories() == null ? game.getCategories() : normalizeDelimited(request.categories());
        final String nextPlatforms = request.platforms() == null ? game.getPlatforms() : requirePlatforms(request.platforms());
        final String nextPrice = request.price() == null ? game.getPrice() : normalizeText(request.price());
        final String nextUrl = request.url() == null ? game.getUrl() : normalizeText(request.url());
        final String nextReleaseDate = request.releaseDate() == null ? game.getReleaseDate() : normalizeText(request.releaseDate());
        final String nextProviderName = request.providerName() == null ? game.getProviderName() : normalizeText(request.providerName());
        final String nextImageUrl = request.imageUrl() == null ? game.getImageUrl() : normalizeImageUrl(request.imageUrl(), adminUser == null ? null : adminUser.getId());

        final Set<AdminGameField> changedFields = new LinkedHashSet<>();
        flagChange(changedFields, AdminGameField.NAME, game.getName(), nextName);
        flagChange(changedFields, AdminGameField.DESCRIPTION, game.getDescription(), nextDescription);
        flagChange(changedFields, AdminGameField.CATEGORIES, game.getCategories(), nextCategories);
        flagChange(changedFields, AdminGameField.PLATFORMS, game.getPlatforms(), nextPlatforms);
        flagChange(changedFields, AdminGameField.PRICE, game.getPrice(), nextPrice);
        flagChange(changedFields, AdminGameField.URL, game.getUrl(), nextUrl);
        flagChange(changedFields, AdminGameField.RELEASE_DATE, game.getReleaseDate(), nextReleaseDate);
        flagChange(changedFields, AdminGameField.PROVIDER_NAME, game.getProviderName(), nextProviderName);
        flagChange(changedFields, AdminGameField.IMAGE_URL, game.getImageUrl(), nextImageUrl);

        game.setName(nextName);
        game.setDescription(nextDescription);
        game.setCategories(nextCategories);
        game.setPlatforms(nextPlatforms);
        game.setPrice(nextPrice);
        game.setUrl(nextUrl);
        game.setReleaseDate(nextReleaseDate);
        game.setProviderName(nextProviderName);
        game.setImageUrl(nextImageUrl);
        game.setNormalizedTitleKey(CatalogGroupingUtils.buildNormalizedTitleKey(game));
        game.setCatalogGroupKey(CatalogGroupingUtils.buildGroupKey(game));

        final Set<AdminGameField> protectedFields = request.syncProtectedFields() == null
                ? new LinkedHashSet<>(AdminOverrideUtils.parseLockedFields(game.getAdminLockedFields()))
                : parseFields(request.syncProtectedFields());
        protectedFields.addAll(changedFields);
        game.setAdminLockedFields(AdminOverrideUtils.serializeLockedFields(protectedFields));
        game.setAdminLastModifiedByUserId(adminUser.getId());
        game.setAdminLastModifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        final GameData savedGame = gameRepository.save(game);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.UPDATE_GAME,
                AdminAuditEntityType.GAME,
                savedGame.getPublicId(),
                "Updated catalog game '" + savedGame.getName() + "'.",
                java.util.Map.of(
                        "changedFields", changedFields.stream().map(Enum::name).toList(),
                        "syncProtectedFields", protectedFields.stream().map(Enum::name).toList()
                )
        );
        return AdminGameDto.fromEntity(savedGame);
    }

    @Transactional
    public void deleteGame(final AppUser adminUser, final String gameId) {
        final GameData game = publicEntityLookupService.requireGame(gameId);
        final String gameName = game.getName();
        gameRepository.delete(game);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.DELETE_GAME,
                AdminAuditEntityType.GAME,
                game.getPublicId(),
                "Deleted catalog game '" + gameName + "'.",
                java.util.Map.of("name", gameName == null ? "" : gameName)
        );
    }

    @Transactional
    public AdminDashboardDto setSpotlightGame(final AppUser adminUser, final String gameId) {
        final GameData game = publicEntityLookupService.requireGame(gameId);
        final AdminCatalogSettings settings = getOrCreateCatalogSettings();
        settings.setSpotlightGame(game);
        adminCatalogSettingsRepository.save(settings);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.SET_SPOTLIGHT,
                AdminAuditEntityType.CATALOG,
                game.getPublicId(),
                "Set '" + game.getName() + "' as the discovery spotlight.",
                java.util.Map.of("gameName", game.getName() == null ? "" : game.getName())
        );
        return getDashboard();
    }

    @Transactional
    public AdminDashboardDto clearSpotlightGame(final AppUser adminUser) {
        final AdminCatalogSettings settings = getOrCreateCatalogSettings();
        final String previousSpotlightId = settings.getSpotlightGame() == null
                ? null
                : settings.getSpotlightGame().getPublicId();
        final String previousSpotlightName = settings.getSpotlightGame() == null ? null : settings.getSpotlightGame().getName();
        settings.setSpotlightGame(null);
        adminCatalogSettingsRepository.save(settings);
        adminAuditService.log(
                adminUser,
                AdminAuditAction.CLEAR_SPOTLIGHT,
                AdminAuditEntityType.CATALOG,
                previousSpotlightId,
                "Cleared the discovery spotlight game.",
                java.util.Map.of("previousGameName", previousSpotlightName == null ? "" : previousSpotlightName)
        );
        return getDashboard();
    }

    @Transactional(readOnly = true)
    public List<com.madre.excelweb.api.model.dto.AdminAuditLogDto> getAuditLogs(final Integer limit) {
        return adminAuditService.getRecentLogs(limit);
    }

    private AdminCatalogSettings getOrCreateCatalogSettings() {
        return adminCatalogSettingsRepository.findById(AdminCatalogSettings.SINGLETON_ID)
                .orElseGet(() -> adminCatalogSettingsRepository.save(AdminCatalogSettings.builder()
                        .id(AdminCatalogSettings.SINGLETON_ID)
                        .build()));
    }

    private boolean matchesUserQuery(final AppUser user, final String normalizedQuery) {
        final String haystack = String.join(" ",
                normalizeText(user.getUsername()),
                normalizeText(user.getEmail()),
                normalizeText(user.getDisplayName()),
                normalizeText(user.getFavoritePlatform()),
                normalizeText(user.getBanReason())
        ).toLowerCase();
        return haystack.contains(normalizedQuery);
    }

    private String normalizeQuery(final String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }

    private String requireName(final String rawValue) {
        final String value = normalizeText(rawValue);
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game name is required.");
        }
        return value;
    }

    private String requirePlatforms(final String rawValue) {
        final String value = CatalogGroupingUtils.mergePlatformValues(normalizeText(rawValue));
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one platform is required.");
        }
        return value;
    }

    private String normalizeDelimited(final String rawValue) {
        return CatalogGroupingUtils.mergeDelimitedValues(normalizeText(rawValue));
    }

    private String normalizeText(final String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    private String normalizeImageUrl(final String rawValue, final Long createdByUserId) {
        final String normalized = mediaAssetService.normalizeImageReference(
                rawValue,
                "Game image",
                MediaPurpose.CATALOG_IMAGE,
                createdByUserId,
                MAX_REMOTE_GAME_IMAGE_LENGTH,
                MAX_INLINE_GAME_IMAGE_LENGTH,
                MAX_INLINE_GIF_GAME_IMAGE_BYTES
        );
        return normalized == null ? "" : normalized;
    }

    private void flagChange(final Set<AdminGameField> changedFields,
                            final AdminGameField field,
                            final String currentValue,
                            final String nextValue) {
        if (!Objects.equals(normalizeText(currentValue), normalizeText(nextValue))) {
            changedFields.add(field);
        }
    }

    private Set<AdminGameField> parseFields(final List<String> rawFields) {
        return (rawFields == null ? List.<String>of() : rawFields).stream()
                .map(AdminGameField::fromNullable)
                .flatMap(java.util.Optional::stream)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
