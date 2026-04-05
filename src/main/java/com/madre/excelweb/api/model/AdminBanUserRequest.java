package com.madre.excelweb.api.model;

public record AdminBanUserRequest(
        Integer durationDays,
        Boolean permanent,
        String reason
) {
}
