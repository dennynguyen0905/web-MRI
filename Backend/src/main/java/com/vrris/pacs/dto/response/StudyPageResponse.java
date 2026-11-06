package com.vrris.pacs.dto.response;

import java.util.List;

public record StudyPageResponse(
        int page,
        int pageSize,
        long total,
        List<StudyResponse> items
) {
}
