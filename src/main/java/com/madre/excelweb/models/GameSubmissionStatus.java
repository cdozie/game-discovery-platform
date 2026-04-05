package com.madre.excelweb.models;

import java.util.Arrays;

public enum GameSubmissionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    public static GameSubmissionStatus fromNullable(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PENDING;
        }

        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported submission status: " + rawValue));
    }
}
