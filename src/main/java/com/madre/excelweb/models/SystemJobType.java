package com.madre.excelweb.models;

public enum SystemJobType {
    CATALOG_SYNC,
    CATALOG_CONSOLIDATION,
    GAME_STATS_REBUILD;

    public static SystemJobType fromNullable(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return SystemJobType.valueOf(rawValue.trim().toUpperCase());
    }
}
