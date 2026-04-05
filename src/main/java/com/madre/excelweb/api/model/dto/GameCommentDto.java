package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.GameComment;
import com.madre.excelweb.models.Role;

import java.time.OffsetDateTime;
import java.util.List;

public record GameCommentDto(
        String id,
        String parentCommentId,
        String content,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarUrl,
        boolean authorAdmin,
        OffsetDateTime createdAt,
        OffsetDateTime lastModified,
        boolean edited,
        boolean deleted,
        boolean ownedByCurrentUser,
        List<GameCommentDto> replies
) {
    public static GameCommentDto fromEntity(final GameComment comment) {
        return fromEntity(comment, List.of(), null);
    }

    public static GameCommentDto fromEntity(final GameComment comment, final List<GameCommentDto> replies, final Long currentUserId) {
        final String authorDisplayName = comment.getAuthor().getDisplayName() == null || comment.getAuthor().getDisplayName().isBlank()
                ? comment.getAuthor().getUsername()
                : comment.getAuthor().getDisplayName();
        final boolean authorAdmin = comment.getAuthor().getRoles() != null
                && comment.getAuthor().getRoles().stream()
                .anyMatch(role -> role.getName() == Role.ADMIN);
        return new GameCommentDto(
                comment.getPublicId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getPublicId(),
                comment.isDeleted() ? "" : comment.getContent(),
                comment.getAuthor().getUsername(),
                authorDisplayName,
                comment.getAuthor().getAvatarUrl(),
                authorAdmin,
                comment.getCreatedAt(),
                comment.getLastModified(),
                comment.getLastModified() != null && comment.getCreatedAt() != null
                        && comment.getLastModified().isAfter(comment.getCreatedAt()),
                comment.isDeleted(),
                currentUserId != null && comment.getAuthor().getId() != null && comment.getAuthor().getId().equals(currentUserId),
                replies == null ? List.of() : List.copyOf(replies)
        );
    }
}
