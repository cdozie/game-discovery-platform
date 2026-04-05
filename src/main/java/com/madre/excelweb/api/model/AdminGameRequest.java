package com.madre.excelweb.api.model;

import java.util.List;

public record AdminGameRequest(
        String name,
        String description,
        String categories,
        String platforms,
        String providerName,
        String imageUrl,
        String releaseDate,
        String url,
        String price,
        List<String> syncProtectedFields
) {
}
