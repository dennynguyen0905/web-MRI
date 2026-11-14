package com.vrris.pacs.dto.response;

import java.time.LocalDateTime;

public record StatusHistoryEntry(
        Long id,
        Long studyId,
        String fromStatus,
        String toStatus,
        Long changedByUserId,
        String changedByUsername,
        LocalDateTime changedAt,
        String reason,
        String note
) {
}
