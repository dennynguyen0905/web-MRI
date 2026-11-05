package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.Modality;

/**
 * Khop dung schema "ServiceItem" trong doc/api/openapi.yaml: id, code, name, modality, body_part.
 * (is_active KHONG co trong contract -> khong tra ve).
 * Khong co factory method tu model.ServiceItem o day - mapper.ServiceItemMapper lam viec do
 * (R-05: dto khong import model).
 */
public record ServiceItemResponse(
        Long id,
        String code,
        String name,
        Modality modality,
        String bodyPart
) {
}
