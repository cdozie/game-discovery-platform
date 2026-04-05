package com.madre.excelweb.api.model;

public record UpdateUserProfileRequest(
        String displayName,
        String bio,
        String avatarUrl,
        String favoritePlatform
) {
}
