package com.vrris.pacs.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        String action,
        String entityType,
        Long entityId,
        Long patientId,
        String beforeValue,
        String afterValue,
        String ipAddress,
        LocalDateTime occurredAt
) {
}
