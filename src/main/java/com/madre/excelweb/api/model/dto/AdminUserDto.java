package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.AppRole;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.models.Role;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public record AdminUserDto(
        String id,
        String username,
        String email,
        String displayName,
        String favoritePlatform,
        List<String> roles,
        boolean banned,
        boolean permanentBan,
        String banReason,
        String bannedByUserId,
        OffsetDateTime banStartedAt,
        OffsetDateTime banExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime lastModified
) {
    public static AdminUserDto fromEntity(final AppUser user) {
        final Set<AppRole> userRoles = user.getRoles();
        return new AdminUserDto(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName(),
                user.getFavoritePlatform(),
                (userRoles == null ? Set.<AppRole>of() : userRoles).stream()
                        .map(AppRole::getName)
                        .map(Role::name)
                        .sorted()
                        .toList(),
                user.isCurrentlyBanned(),
                user.isBanPermanent(),
                user.getBanReason(),
                user.getBannedByUserId() == null ? null : user.getBannedByUserId().toString(),
                user.getBanStartedAt(),
                user.getBanExpiresAt(),
                user.getCreatedAt(),
                user.getLastModified()
        );
    }
}
