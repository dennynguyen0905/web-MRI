package com.vrris.pacs.dto.response;

import java.util.List;

public record AuditLogPageResponse(
        List<AuditLogResponse> items,
        long total,
        int page,
        int pageSize,
        int totalPages
) {
}
