package com.madre.excelweb.models;

import java.util.Arrays;
import java.util.Optional;

public enum AdminGameField {
    NAME,
    DESCRIPTION,
    CATEGORIES,
    PLATFORMS,
    PRICE,
    URL,
    RELEASE_DATE,
    PROVIDER_NAME,
    IMAGE_URL;

    public static Optional<AdminGameField> fromNullable(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst();
    }
}
