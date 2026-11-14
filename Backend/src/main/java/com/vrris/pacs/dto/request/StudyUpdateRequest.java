package com.vrris.pacs.dto.request;

import java.time.LocalDateTime;

public record StudyUpdateRequest(
        Long serviceItemId,
        Long deviceId,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String studyInstanceUid
) {
}
