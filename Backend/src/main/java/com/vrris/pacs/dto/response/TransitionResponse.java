package com.vrris.pacs.dto.response;

/**
 * Phan hoi cua POST /studies/{id}/transitions, khop TransitionResultDto trong
 * Frontend/src/shared/api/dto.ts va Frontend/src/features/studies/api/studiesApi.ts:
 * { study, history_entry, report }.
 */
public record TransitionResponse(
        StudyResponse study,
        StatusHistoryEntry historyEntry,
        ReportResponse report
) {
    public TransitionResponse(StudyResponse study, StatusHistoryEntry historyEntry) {
        this(study, historyEntry, null);
    }
}
