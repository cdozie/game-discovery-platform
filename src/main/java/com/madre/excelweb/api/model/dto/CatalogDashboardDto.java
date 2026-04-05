package com.madre.excelweb.api.model.dto;

import java.util.List;

public record CatalogDashboardDto(
        long totalGames,
        long totalUsers,
        long totalWishlistEntries,
        boolean syncInProgress,
        CatalogSyncRunDto lastSync,
        List<GameDto> latestGames,
        List<GameDto> popularGames
) {
}
