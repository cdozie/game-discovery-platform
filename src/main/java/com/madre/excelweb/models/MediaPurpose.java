package com.madre.excelweb.models;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public enum MediaPurpose {
    AVATAR,
    GAME_SUBMISSION,
    CATALOG_IMAGE;

    public static MediaPurpose fromNullable(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return CATALOG_IMAGE;
        }

        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported media purpose."));
    }
}
