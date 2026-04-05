package com.madre.excelweb.models;

import java.util.Arrays;

public enum CatalogSource {
    PLAYSTATION_STORE,
    IGDB_PC_XBOX,
    USER_SUBMITTED;

    public static CatalogSource fromNullable(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PLAYSTATION_STORE;
        }

        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported catalog source: " + rawValue));
    }
}
