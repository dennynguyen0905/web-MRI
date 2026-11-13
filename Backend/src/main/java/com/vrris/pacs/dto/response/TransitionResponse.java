package com.vrris.pacs.dto.response;

public record TransitionResponse(
        StudyResponse study,
        StatusHistoryEntry historyEntry
) {
}
