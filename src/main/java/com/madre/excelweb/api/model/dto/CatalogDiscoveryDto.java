package com.madre.excelweb.api.model.dto;

import java.util.List;

public record CatalogDiscoveryDto(
        String preferredPlatform,
        GameDto spotlightGame,
        List<GameDto> featuredGames,
        List<GameDto> newAndNoteworthy,
        List<GameDto> communityFavorites,
        List<GameDto> surpriseGames
) {
}
