package com.madre.excelweb.api.model.dto;

public record AdminDashboardDto(
        long totalUsers,
        long activeBans,
        long totalGames,
        long pendingSubmissions,
        long approvedSubmissions,
        long rejectedSubmissions,
        String spotlightGameId,
        String spotlightGameName
) {
}
