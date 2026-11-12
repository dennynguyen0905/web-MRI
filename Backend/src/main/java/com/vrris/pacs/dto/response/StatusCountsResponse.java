package com.vrris.pacs.dto.response;

import java.util.Map;

/** GET /studies/status-counts (IN-09) - khop schema StatusCounts cua doc/api/openapi.yaml. */
public record StatusCountsResponse(
        long total,
        Map<String, Long> byStatus
) {
}
