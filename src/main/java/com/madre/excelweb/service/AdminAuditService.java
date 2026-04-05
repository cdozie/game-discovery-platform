package com.madre.excelweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madre.excelweb.api.model.dto.AdminAuditLogDto;
import com.madre.excelweb.jpa.entity.AdminAuditLog;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.repositories.AdminAuditLogRepository;
import com.madre.excelweb.models.AdminAuditAction;
import com.madre.excelweb.models.AdminAuditEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public void log(final AppUser actor,
                    final AdminAuditAction action,
                    final AdminAuditEntityType entityType,
                    final String entityId,
                    final String summary,
                    final Map<String, ?> details) {
        final String serializedDetails;
        try {
            serializedDetails = objectMapper.writeValueAsString(sanitizeDetails(details));
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Could not serialize admin audit details.");
        }

        adminAuditLogRepository.save(AdminAuditLog.builder()
                .actorUserId(actor == null ? null : actor.getId())
                .actorUsername(actor == null ? null : actor.getUsername())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(summary)
                .detailsJson(serializedDetails)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }

    private Map<String, Object> sanitizeDetails(final Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }

        final Map<String, Object> sanitized = new LinkedHashMap<>();
        details.forEach((key, value) -> sanitized.put(key, value == null ? "" : value));
        return sanitized;
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogDto> getRecentLogs(final Integer limit) {
        final int resolvedLimit = Math.max(1, Math.min(limit == null ? 80 : limit, 200));
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, resolvedLimit)).stream()
                .map(AdminAuditLogDto::fromEntity)
                .toList();
    }
}
