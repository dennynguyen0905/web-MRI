package com.vrris.pacs.dto.response;

import java.util.List;

public record ImagingOrderPageResponse(
        int page,
        int pageSize,
        long total,
        List<ImagingOrderResponse> items
) {
}
