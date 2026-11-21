package com.vrris.pacs.dto.response;

import java.time.LocalDateTime;

/**
 * Mot ban ghi lich su trang thai ca chup (GET /studies/{id}/status-history va POST /studies/{id}/transitions).
 * Kèm ca changedBy (UserRefResponse -> JSON changed_by) khop Frontend/src/shared/api/dto.ts
 * (StatusHistoryEntryDto.changed_by) va changedByUserId/changedByUsername de tuong thich nguoc.
 */
public record StatusHistoryEntry(
        Long id,
        Long studyId,
        String fromStatus,
        String toStatus,
        String transitionCode,
        Long changedByUserId,
        String changedByUsername,
        UserRefResponse changedBy,
        LocalDateTime changedAt,
        String reason,
        String note
) {
    public StatusHistoryEntry(
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
        this(
                id,
                studyId,
                fromStatus,
                toStatus,
                null,
                changedByUserId,
                changedByUsername,
                changedByUserId == null ? null : new UserRefResponse(changedByUserId, changedByUsername, changedByUsername),
                changedAt,
                reason,
                note
        );
    }
}
