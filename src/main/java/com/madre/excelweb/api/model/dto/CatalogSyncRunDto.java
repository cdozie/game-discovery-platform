package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.CatalogSyncRun;

import java.time.OffsetDateTime;

public record CatalogSyncRunDto(
        String id,
        String status,
        String catalogSource,
        String triggeredBy,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer requestedPageSize,
        Integer requestedMaxBatches,
        Integer requestedMaxRecords,
        Integer requestedStartOffset,
        Integer currentOffset,
        Integer completedBatches,
        OffsetDateTime lastCheckpointAt,
        Long processedCount,
        Long createdCount,
        Long updatedCount,
        Long unchangedCount,
        Long failedCount,
        String message
) {
    public static CatalogSyncRunDto fromEntity(final CatalogSyncRun run) {
        if (run == null) {
            return null;
        }

        return new CatalogSyncRunDto(
                run.getPublicId(),
                run.getStatus().name(),
                run.getCatalogSource().name(),
                run.getTriggeredBy(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getRequestedPageSize(),
                run.getRequestedMaxBatches(),
                run.getRequestedMaxRecords(),
                run.getRequestedStartOffset(),
                run.getCurrentOffset(),
                run.getCompletedBatches(),
                run.getLastCheckpointAt(),
                run.getProcessedCount(),
                run.getCreatedCount(),
                run.getUpdatedCount(),
                run.getUnchangedCount(),
                run.getFailedCount(),
                run.getMessage()
        );
    }
}
