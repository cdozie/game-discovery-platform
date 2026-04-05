package com.madre.excelweb.service;

import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.CatalogSyncRun;
import com.madre.excelweb.jpa.entity.GameComment;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.GameSubmission;
import com.madre.excelweb.jpa.repositories.CatalogSyncRunRepository;
import com.madre.excelweb.jpa.repositories.GameCommentRepository;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.GameSubmissionRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PublicEntityLookupService {
    private final GameRepository gameRepository;
    private final GameCommentRepository gameCommentRepository;
    private final GameSubmissionRepository gameSubmissionRepository;
    private final UserRepository userRepository;
    private final CatalogSyncRunRepository catalogSyncRunRepository;

    public Optional<GameData> findGame(final String idOrPublicId) {
        return findEntity(idOrPublicId,
                () -> gameRepository.findByPublicId(normalizeId(idOrPublicId)),
                () -> parseLegacyNumericId(idOrPublicId).flatMap(gameRepository::findGameById));
    }

    public GameData requireGame(final String idOrPublicId) {
        return findGame(idOrPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));
    }

    public Optional<GameComment> findComment(final String idOrPublicId) {
        return findEntity(idOrPublicId,
                () -> gameCommentRepository.findByPublicIdWithGame(normalizeId(idOrPublicId)),
                () -> parseLegacyNumericId(idOrPublicId).flatMap(gameCommentRepository::findByIdWithGame));
    }

    public GameComment requireComment(final String idOrPublicId) {
        return findComment(idOrPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found."));
    }

    public Optional<GameSubmission> findSubmission(final String idOrPublicId) {
        return findEntity(idOrPublicId,
                () -> gameSubmissionRepository.findByPublicId(normalizeId(idOrPublicId)),
                () -> parseLegacyNumericId(idOrPublicId).flatMap(gameSubmissionRepository::findById));
    }

    public GameSubmission requireSubmission(final String idOrPublicId) {
        return findSubmission(idOrPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found."));
    }

    public Optional<AppUser> findUser(final String idOrPublicId) {
        return findEntity(idOrPublicId,
                () -> userRepository.getUserByPublicId(normalizeId(idOrPublicId)),
                () -> parseLegacyNumericId(idOrPublicId).flatMap(userRepository::getUserById));
    }

    public AppUser requireUser(final String idOrPublicId) {
        return findUser(idOrPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    public Optional<CatalogSyncRun> findCatalogSyncRun(final String idOrPublicId) {
        return findEntity(idOrPublicId,
                () -> catalogSyncRunRepository.findByPublicId(normalizeId(idOrPublicId)),
                () -> parseLegacyNumericId(idOrPublicId).flatMap(catalogSyncRunRepository::findById));
    }

    public CatalogSyncRun requireCatalogSyncRun(final String idOrPublicId) {
        return findCatalogSyncRun(idOrPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog sync run not found."));
    }

    private <T> Optional<T> findEntity(final String idOrPublicId,
                                       final Supplier<Optional<T>> publicIdLookup,
                                       final Supplier<Optional<T>> legacyIdLookup) {
        final String normalizedId = normalizeId(idOrPublicId);
        if (normalizedId == null) {
            return Optional.empty();
        }

        final Optional<T> publicIdMatch = publicIdLookup.get();
        if (publicIdMatch.isPresent()) {
            return publicIdMatch;
        }

        return legacyIdLookup.get();
    }

    private Optional<Long> parseLegacyNumericId(final String idOrPublicId) {
        final String normalizedId = normalizeId(idOrPublicId);
        if (normalizedId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(normalizedId));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String normalizeId(final String idOrPublicId) {
        if (idOrPublicId == null || idOrPublicId.isBlank()) {
            return null;
        }
        return idOrPublicId.trim();
    }
}
