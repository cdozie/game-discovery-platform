package com.madre.excelweb.api.model.dto;

import java.util.List;

public record GameSearchPageDto(
        String query,
        int offset,
        int limit,
        boolean hasMore,
        List<GameDto> items
) {
}
