package com.madre.excelweb.api.model;

public record CatalogSyncRequest(String catalogSource,
                                 Integer pageSize,
                                 Integer maxBatches,
                                 Integer maxRecords,
                                 Integer startOffset) {
}
