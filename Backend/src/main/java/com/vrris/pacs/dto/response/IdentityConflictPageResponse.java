package com.vrris.pacs.dto.response;

import java.util.List;

public record IdentityConflictPageResponse(
        int page,
        int pageSize,
        long total,
        List<IdentityConflictResponse> items
) {
}
