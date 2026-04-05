package com.madre.excelweb.api.model.dto;

import java.util.List;

public record OffsetPageDto<T>(
        int offset,
        int limit,
        boolean hasMore,
        List<T> items
) {
}
