package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.AppUser;

import java.time.OffsetDateTime;
import java.util.List;

public record UserProfileDto(
        String id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        String favoritePlatform,
        List<String> roles,
        boolean admin,
        OffsetDateTime createdAt,
        OffsetDateTime lastModified
) {
    public static UserProfileDto fromEntity(final AppUser user) {
        final List<String> roles = (user.getRoles() == null ? java.util.Set.<com.madre.excelweb.jpa.entity.AppRole>of() : user.getRoles()).stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
        return new UserProfileDto(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getFavoritePlatform(),
                roles,
                roles.contains("ADMIN"),
                user.getCreatedAt(),
                user.getLastModified()
        );
    }
}
