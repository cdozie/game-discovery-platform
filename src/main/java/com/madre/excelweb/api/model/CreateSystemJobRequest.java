package com.madre.excelweb.api.model;

public record CreateSystemJobRequest(
        String jobType,
        String catalogSource,
        Integer pageSize,
        Integer maxBatches,
        Integer maxRecords,
        Integer startOffset
) {
}
