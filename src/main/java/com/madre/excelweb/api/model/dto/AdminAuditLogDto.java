package com.madre.excelweb.api.model.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madre.excelweb.jpa.entity.AdminAuditLog;

import java.time.OffsetDateTime;
import java.util.Map;

public record AdminAuditLogDto(
        String id,
        String actorUserId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String summary,
        Map<String, Object> details,
        OffsetDateTime createdAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public static AdminAuditLogDto fromEntity(final AdminAuditLog auditLog) {
        return new AdminAuditLogDto(
                auditLog.getPublicId(),
                auditLog.getActorUserId() == null ? null : auditLog.getActorUserId().toString(),
                auditLog.getActorUsername(),
                auditLog.getAction() == null ? null : auditLog.getAction().name(),
                auditLog.getEntityType() == null ? null : auditLog.getEntityType().name(),
                auditLog.getEntityId(),
                auditLog.getSummary(),
                parseDetails(auditLog.getDetailsJson()),
                auditLog.getCreatedAt()
        );
    }

    private static Map<String, Object> parseDetails(final String rawDetails) {
        if (rawDetails == null || rawDetails.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(rawDetails, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of("raw", rawDetails);
        }
    }
}
