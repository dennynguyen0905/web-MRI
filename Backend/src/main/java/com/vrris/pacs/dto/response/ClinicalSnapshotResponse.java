package com.vrris.pacs.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * Khop schema "ClinicalSnapshot" trong doc/api/openapi.yaml (operationId getClinicalSnapshot,
 * IN-11). "payload" tra ve dang object JSON (khong phai String) - service.impl.ImagingOrderServiceImpl
 * parse cot clinical_snapshot.payload (luu String trong model.ClinicalSnapshot) bang ObjectMapper
 * truoc khi dung record nay, giong cach PrintServiceImpl.snapshotNode doc payload.
 */
public record ClinicalSnapshotResponse(
        Long orderId,
        String source,
        LocalDateTime fetchedAt,
        JsonNode payload
) {
}
