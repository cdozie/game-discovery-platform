package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.SystemJob;

import java.time.OffsetDateTime;

public record SystemJobDto(
        String id,
        String type,
        String status,
        String requestedBy,
        String catalogSource,
        String relatedSyncRunId,
        String progressMessage,
        String resultSummary,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime lastHeartbeatAt
) {
    public static SystemJobDto fromEntity(final SystemJob job, final CatalogSyncRunDto relatedSyncRun) {
        return new SystemJobDto(
                job.getPublicId(),
                job.getJobType() == null ? null : job.getJobType().name(),
                job.getStatus() == null ? null : job.getStatus().name(),
                job.getRequestedBy(),
                job.getCatalogSource(),
                relatedSyncRun == null ? null : relatedSyncRun.id(),
                job.getProgressMessage(),
                job.getResultSummary(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getLastHeartbeatAt()
        );
    }
}
