package com.madre.excelweb.api.model;

public record CreateGameSubmissionRequest(
        String name,
        String description,
        String categories,
        String platforms,
        String providerName,
        String imageUrl,
        String releaseDate,
        String url
) {
}
