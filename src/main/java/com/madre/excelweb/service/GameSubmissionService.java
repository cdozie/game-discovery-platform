package com.madre.excelweb.service;

import com.madre.excelweb.api.model.CreateGameSubmissionRequest;
import com.madre.excelweb.api.model.ReviewGameSubmissionRequest;
import com.madre.excelweb.api.model.dto.GameSubmissionDto;
import com.madre.excelweb.api.model.dto.OffsetPageDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.GameSubmission;
import com.madre.excelweb.jpa.repositories.GameSourceEntryRepository;
import com.madre.excelweb.jpa.repositories.GameSubmissionRepository;
import com.madre.excelweb.models.AdminAuditAction;
import com.madre.excelweb.models.AdminAuditEntityType;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.models.GameSubmissionStatus;
import com.madre.excelweb.models.MediaPurpose;
import com.madre.excelweb.service.util.CatalogGroupingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameSubmissionService {
    private static final int MAX_REMOTE_GAME_IMAGE_LENGTH = 4_096;
    private static final int MAX_INLINE_GAME_IMAGE_LENGTH = 2_500_000;
    private static final int MAX_INLINE_GIF_GAME_IMAGE_BYTES = 30 * 1024 * 1024;

    private final GameSubmissionRepository gameSubmissionRepository;
    private final CatalogUpsertService catalogUpsertService;
    private final GameSourceEntryRepository gameSourceEntryRepository;
    private final MediaAssetService mediaAssetService;
    private final AdminAuditService adminAuditService;
    private final PublicEntityLookupService publicEntityLookupService;

    @Transactional
    public GameSubmissionDto submitGame(final AppUser requestedByUser,
                                        final CreateGameSubmissionRequest request) {
        final GameSubmission submission = applySubmissionFields(GameSubmission.builder()
                .requestedByUser(requestedByUser)
                .status(GameSubmissionStatus.PENDING)
                .build(), request, requestedByUser == null ? null : requestedByUser.getId());
        return GameSubmissionDto.fromEntity(gameSubmissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<GameSubmissionDto> getMySubmissions(final AppUser requestedByUser) {
        return gameSubmissionRepository.findByRequestedByUserIdOrderByCreatedAtDesc(requestedByUser.getId()).stream()
                .map(GameSubmissionDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public OffsetPageDto<GameSubmissionDto> getSubmissions(final String rawStatus,
                                                           final Integer limit,
                                                           final Integer offset) {
        final int resolvedLimit = clamp(limit, 1, 40, 12);
        final int resolvedOffset = Math.max(0, offset == null ? 0 : offset);
        final GameSubmissionStatus status = rawStatus == null || rawStatus.isBlank()
                ? null
                : GameSubmissionStatus.fromNullable(rawStatus);
        final List<Long> submissionIds = status == null
                ? gameSubmissionRepository.findPageIdsOrderByCreatedAtDesc(resolvedLimit + 1, resolvedOffset)
                : gameSubmissionRepository.findPageIdsByStatusOrderByCreatedAtDesc(status.name(), resolvedLimit + 1, resolvedOffset);
        final boolean hasMore = submissionIds.size() > resolvedLimit;
        final List<Long> visibleSubmissionIds = submissionIds.stream().limit(resolvedLimit).toList();
        if (visibleSubmissionIds.isEmpty()) {
            return new OffsetPageDto<>(resolvedOffset, resolvedLimit, false, List.of());
        }

        final Map<Long, GameSubmission> submissionsById = gameSubmissionRepository.findAllByIdInWithDetails(visibleSubmissionIds).stream()
                .collect(Collectors.toMap(GameSubmission::getId, submission -> submission));
        final List<GameSubmissionDto> items = visibleSubmissionIds.stream()
                .map(submissionsById::get)
                .filter(Objects::nonNull)
                .map(GameSubmissionDto::fromEntity)
                .toList();
        return new OffsetPageDto<>(resolvedOffset, resolvedLimit, hasMore, items);
    }

    @Transactional
    public GameSubmissionDto reviewSubmission(final AppUser adminUser,
                                              final String submissionId,
                                              final ReviewGameSubmissionRequest request) {
        final GameSubmission submission = publicEntityLookupService.requireSubmission(submissionId);

        if (submission.getStatus() == GameSubmissionStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Withdrawn submissions cannot be reviewed.");
        }

        if (submission.getStatus() != GameSubmissionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending submissions can be reviewed."
            );
        }

        final GameSubmissionStatus nextStatus = GameSubmissionStatus.fromNullable(request.status());
        submission.setReviewedByUser(adminUser);
        submission.setReviewNotes(normalizeOptionalText(request.reviewNotes()));
        submission.setStatus(nextStatus);

        if (nextStatus == GameSubmissionStatus.APPROVED && submission.getCreatedGame() == null) {
            final String externalId = "SUBMISSION:" + submission.getPublicId();
            final CatalogImportGame importGame = CatalogImportGame.builder()
                    .externalId(externalId)
                    .name(submission.getName())
                    .description(submission.getDescription())
                    .categories(submission.getCategories())
                    .platforms(submission.getPlatforms())
                    .price("")
                    .url(submission.getUrl())
                    .releaseDate(submission.getReleaseDate())
                    .providerName(submission.getProviderName())
                    .imageUrl(submission.getImageUrl())
                    .build();
            catalogUpsertService.upsertBatch(List.of(importGame), CatalogSource.USER_SUBMITTED);
            final GameData createdGame = gameSourceEntryRepository.findByCatalogSourceAndExternalId(CatalogSource.USER_SUBMITTED, externalId)
                    .map(sourceEntry -> sourceEntry.getGame())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Approved submission could not be linked to a game."));
            submission.setCreatedGame(createdGame);
        }
        final GameSubmission savedSubmission = gameSubmissionRepository.save(submission);
        final Map<String, Object> reviewAuditDetails = new LinkedHashMap<>();
        reviewAuditDetails.put("status", nextStatus.name());
        reviewAuditDetails.put("reviewNotes", normalizeOptionalText(savedSubmission.getReviewNotes()));
        reviewAuditDetails.put(
                "createdGameId",
                savedSubmission.getCreatedGame() == null
                        ? ""
                        : normalizeOptionalText(savedSubmission.getCreatedGame().getPublicId())
        );
        adminAuditService.log(
                adminUser,
                AdminAuditAction.REVIEW_SUBMISSION,
                AdminAuditEntityType.SUBMISSION,
                savedSubmission.getPublicId(),
                "Reviewed submission '" + requireNonBlank(savedSubmission.getName(), "Submission name is required.") + "' as " + nextStatus.name() + ".",
                reviewAuditDetails
        );
        return GameSubmissionDto.fromEntity(savedSubmission);
    }

    @Transactional
    public GameSubmissionDto updateSubmissionAsAdmin(final AppUser adminUser,
                                                     final String submissionId,
                                                     final CreateGameSubmissionRequest request) {
        final GameSubmission submission = publicEntityLookupService.requireSubmission(submissionId);

        if (submission.getStatus() == GameSubmissionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved submissions cannot be changed through the review queue.");
        }
        if (submission.getStatus() == GameSubmissionStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Withdrawn submissions cannot be changed through the review queue.");
        }

        applySubmissionFields(submission, request, adminUser == null ? null : adminUser.getId());
        submission.setStatus(GameSubmissionStatus.PENDING);
        submission.setReviewedByUser(null);
        submission.setReviewNotes(null);
        final GameSubmission savedSubmission = gameSubmissionRepository.save(submission);
        final Map<String, Object> updateAuditDetails = new LinkedHashMap<>();
        updateAuditDetails.put("platforms", normalizeOptionalText(savedSubmission.getPlatforms()));
        updateAuditDetails.put("providerName", normalizeOptionalText(savedSubmission.getProviderName()));
        adminAuditService.log(
                adminUser,
                AdminAuditAction.UPDATE_SUBMISSION,
                AdminAuditEntityType.SUBMISSION,
                savedSubmission.getPublicId(),
                "Edited submission '" + requireNonBlank(savedSubmission.getName(), "Submission name is required.") + "' before review.",
                updateAuditDetails
        );
        return GameSubmissionDto.fromEntity(savedSubmission);
    }

    @Transactional
    public GameSubmissionDto updateMySubmission(final AppUser requestedByUser,
                                                final String submissionId,
                                                final CreateGameSubmissionRequest request) {
        final GameSubmission submission = requireOwnedSubmission(requestedByUser, submissionId);
        if (submission.getStatus() == GameSubmissionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved submissions cannot be changed.");
        }

        applySubmissionFields(submission, request, requestedByUser == null ? null : requestedByUser.getId());
        submission.setStatus(GameSubmissionStatus.PENDING);
        submission.setReviewedByUser(null);
        submission.setReviewNotes(null);
        return GameSubmissionDto.fromEntity(gameSubmissionRepository.save(submission));
    }

    @Transactional
    public GameSubmissionDto withdrawMySubmission(final AppUser requestedByUser,
                                                  final String submissionId) {
        final GameSubmission submission = requireOwnedSubmission(requestedByUser, submissionId);
        if (submission.getStatus() == GameSubmissionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved submissions cannot be withdrawn.");
        }
        if (submission.getStatus() == GameSubmissionStatus.WITHDRAWN) {
            return GameSubmissionDto.fromEntity(submission);
        }

        submission.setStatus(GameSubmissionStatus.WITHDRAWN);
        return GameSubmissionDto.fromEntity(gameSubmissionRepository.save(submission));
    }

    private GameSubmission applySubmissionFields(final GameSubmission submission,
                                                 final CreateGameSubmissionRequest request,
                                                 final Long createdByUserId) {
        submission.setName(requireNonBlank(request.name(), "Game name is required."));
        submission.setDescription(normalizeOptionalText(request.description()));
        submission.setCategories(normalizeDelimitedValue(request.categories()));
        submission.setPlatforms(requirePlatforms(request.platforms()));
        submission.setProviderName(normalizeOptionalText(request.providerName()));
        submission.setImageUrl(normalizeImageUrl(request.imageUrl(), createdByUserId));
        submission.setReleaseDate(normalizeOptionalText(request.releaseDate()));
        submission.setUrl(normalizeOptionalText(request.url()));
        return submission;
    }

    private GameSubmission requireOwnedSubmission(final AppUser requestedByUser,
                                                  final String submissionId) {
        if (requestedByUser == null || requestedByUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }

        final GameSubmission submission = publicEntityLookupService.requireSubmission(submissionId);

        if (submission.getRequestedByUser() == null
                || submission.getRequestedByUser().getId() == null
                || !submission.getRequestedByUser().getId().equals(requestedByUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own submissions.");
        }

        return submission;
    }

    private String requirePlatforms(final String platforms) {
        final String normalizedPlatforms = CatalogGroupingUtils.mergePlatformValues(normalizeOptionalText(platforms));
        if (normalizedPlatforms.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one platform is required.");
        }
        return normalizedPlatforms;
    }

    private String normalizeDelimitedValue(final String rawValue) {
        return CatalogGroupingUtils.mergeDelimitedValues(normalizeOptionalText(rawValue));
    }

    private String requireNonBlank(final String rawValue, final String message) {
        final String normalized = normalizeOptionalText(rawValue);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeOptionalText(final String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    private String normalizeImageUrl(final String rawValue, final Long createdByUserId) {
        final String normalized = mediaAssetService.normalizeImageReference(
                rawValue,
                "Game image",
                MediaPurpose.GAME_SUBMISSION,
                createdByUserId,
                MAX_REMOTE_GAME_IMAGE_LENGTH,
                MAX_INLINE_GAME_IMAGE_LENGTH,
                MAX_INLINE_GIF_GAME_IMAGE_BYTES
        );
        return normalized == null ? "" : normalized;
    }

    private int clamp(final Integer value, final int min, final int max, final int fallback) {
        final int resolvedValue = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolvedValue));
    }
}
