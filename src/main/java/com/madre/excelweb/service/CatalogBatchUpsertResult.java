package com.madre.excelweb.service;

public record CatalogBatchUpsertResult(
        long processedCount,
        long createdCount,
        long updatedCount,
        long unchangedCount
) {
    public static CatalogBatchUpsertResult empty() {
        return new CatalogBatchUpsertResult(0L, 0L, 0L, 0L);
    }

    public CatalogBatchUpsertResult plus(final CatalogBatchUpsertResult other) {
        return new CatalogBatchUpsertResult(
                processedCount + other.processedCount,
                createdCount + other.createdCount,
                updatedCount + other.updatedCount,
                unchangedCount + other.unchangedCount
        );
    }
}
