package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.SystemJobStatus;
import com.madre.excelweb.models.SystemJobType;
import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "system_jobs", indexes = {
        @Index(name = "system_jobs_created_at_idx", columnList = "created_at"),
        @Index(name = "system_jobs_status_idx", columnList = "status"),
        @Index(name = "system_jobs_job_type_idx", columnList = "job_type"),
        @Index(name = "system_jobs_public_id_idx", columnList = "public_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemJob implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 64)
    private SystemJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SystemJobStatus status;

    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "catalog_source", length = 64)
    private String catalogSource;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    @Column(name = "progress_message", columnDefinition = "TEXT")
    private String progressMessage;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "related_sync_run_id")
    private Long relatedSyncRunId;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    @Column(name = "last_heartbeat_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastHeartbeatAt;

    @PrePersist
    public void prePersist() {
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdUtils.newPublicId();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (status == null) {
            status = SystemJobStatus.QUEUED;
        }
    }
}
