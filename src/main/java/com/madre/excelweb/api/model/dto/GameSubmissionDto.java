package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.GameSubmission;
import com.madre.excelweb.models.GameSubmissionStatus;

import java.time.OffsetDateTime;

public record GameSubmissionDto(
        String id,
        String status,
        String name,
        String description,
        String categories,
        String platforms,
        String providerName,
        String imageUrl,
        String releaseDate,
        String url,
        String reviewNotes,
        String requestedByUsername,
        String requestedByDisplayName,
        String reviewedByUsername,
        String createdGameId,
        String createdGameName,
        boolean canEdit,
        boolean canWithdraw,
        OffsetDateTime createdAt,
        OffsetDateTime lastModified
) {
    private static boolean canEdit(final GameSubmission submission) {
        return submission.getStatus() != GameSubmissionStatus.APPROVED;
    }

    private static boolean canWithdraw(final GameSubmission submission) {
        return submission.getStatus() != GameSubmissionStatus.APPROVED
                && submission.getStatus() != GameSubmissionStatus.WITHDRAWN;
    }

    public static GameSubmissionDto fromEntity(final GameSubmission submission) {
        return new GameSubmissionDto(
                submission.getPublicId(),
                submission.getStatus() == null ? null : submission.getStatus().name(),
                submission.getName(),
                submission.getDescription(),
                submission.getCategories(),
                submission.getPlatforms(),
                submission.getProviderName(),
                submission.getImageUrl(),
                submission.getReleaseDate(),
                submission.getUrl(),
                submission.getReviewNotes(),
                submission.getRequestedByUser() == null ? null : submission.getRequestedByUser().getUsername(),
                submission.getRequestedByUser() == null ? null :
                        (submission.getRequestedByUser().getDisplayName() == null || submission.getRequestedByUser().getDisplayName().isBlank()
                                ? submission.getRequestedByUser().getUsername()
                                : submission.getRequestedByUser().getDisplayName()),
                submission.getReviewedByUser() == null ? null : submission.getReviewedByUser().getUsername(),
                submission.getCreatedGame() == null ? null : submission.getCreatedGame().getPublicId(),
                submission.getCreatedGame() == null ? null : submission.getCreatedGame().getName(),
                canEdit(submission),
                canWithdraw(submission),
                submission.getCreatedAt(),
                submission.getLastModified()
        );
    }
}
