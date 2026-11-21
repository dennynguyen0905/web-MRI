package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        AuditAction action,
        AuditEntityType entityType,
        Long entityId,
        Long patientId,
        String beforeValue,
        String afterValue,
        String ipAddress,
        LocalDateTime occurredAt
) {
}
