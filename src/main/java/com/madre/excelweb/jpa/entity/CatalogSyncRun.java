package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.CatalogSyncStatus;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "catalog_sync_runs", indexes = {
        @Index(name = "catalog_sync_runs_started_at_idx", columnList = "started_at"),
        @Index(name = "catalog_sync_runs_status_idx", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSyncRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CatalogSyncStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_source", nullable = false)
    private CatalogSource catalogSource;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "started_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    @Column(name = "requested_page_size")
    private Integer requestedPageSize;

    @Column(name = "requested_max_batches")
    private Integer requestedMaxBatches;

    @Column(name = "requested_max_records")
    private Integer requestedMaxRecords;

    @Column(name = "requested_start_offset", nullable = false)
    private Integer requestedStartOffset;

    @Column(name = "current_offset", nullable = false)
    private Integer currentOffset;

    @Column(name = "completed_batches", nullable = false)
    private Integer completedBatches;

    @Column(name = "last_checkpoint_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastCheckpointAt;

    @Column(name = "processed_count", nullable = false)
    private Long processedCount;

    @Column(name = "created_count", nullable = false)
    private Long createdCount;

    @Column(name = "updated_count", nullable = false)
    private Long updatedCount;

    @Column(name = "unchanged_count", nullable = false)
    private Long unchangedCount;

    @Column(name = "failed_count", nullable = false)
    private Long failedCount;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @PrePersist
    public void prePersist() {
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdUtils.newPublicId();
        }
    }
}
