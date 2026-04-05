package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.AdminAuditAction;
import com.madre.excelweb.models.AdminAuditEntityType;
import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "admin_audit_logs_created_idx", columnList = "created_at"),
        @Index(name = "admin_audit_logs_actor_created_idx", columnList = "actor_user_id, created_at"),
        @Index(name = "admin_audit_logs_entity_created_idx", columnList = "entity_type, entity_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username")
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64)
    private AdminAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 64)
    private AdminAuditEntityType entityType;

    @Column(name = "entity_id", length = 128)
    private String entityId;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "details_json", nullable = false, columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdUtils.newPublicId();
        }
        if (detailsJson == null || detailsJson.isBlank()) {
            detailsJson = "{}";
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
