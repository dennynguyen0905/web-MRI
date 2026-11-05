package com.vrris.pacs.dto.response;

import java.util.List;

/**
 * Khop schema "PatientPage" trong doc/api/openapi.yaml (allOf PageMeta + items).
 */
public record PatientPageResponse(
        int page,
        int pageSize,
        long total,
        List<PatientResponse> items
) {
}
