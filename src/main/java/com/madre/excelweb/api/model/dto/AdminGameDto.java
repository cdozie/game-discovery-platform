package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.service.util.AdminOverrideUtils;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminGameDto(
        String id,
        String catalogSources,
        String name,
        String description,
        String categories,
        String platforms,
        String price,
        String url,
        String releaseDate,
        String providerName,
        String imageUrl,
        String syncProtectedByUserId,
        OffsetDateTime syncProtectedAt,
        List<String> syncProtectedFields,
        String wishlistTotal,
        String comments,
        String averageRating,
        OffsetDateTime created,
        OffsetDateTime lastModified
) {
    public static AdminGameDto fromEntity(final GameData gameData) {
        return new AdminGameDto(
                gameData.getPublicId(),
                gameData.getCatalogSources(),
                gameData.getName(),
                gameData.getDescription(),
                gameData.getCategories(),
                gameData.getPlatforms(),
                gameData.getPrice(),
                gameData.getUrl(),
                gameData.getReleaseDate(),
                gameData.getProviderName(),
                gameData.getImageUrl(),
                gameData.getAdminLastModifiedByUserId() == null ? null : gameData.getAdminLastModifiedByUserId().toString(),
                gameData.getAdminLastModifiedAt(),
                AdminOverrideUtils.parseLockedFields(gameData.getAdminLockedFields()).stream()
                        .map(Enum::name)
                        .toList(),
                String.valueOf(gameData.getWishlistTotal()),
                String.valueOf(gameData.getComments()),
                gameData.getAverageRating() == null ? null : String.valueOf(gameData.getAverageRating()),
                gameData.getCreated(),
                gameData.getLastModified()
        );
    }
}
