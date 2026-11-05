package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.Modality;

/**
 * Khop dung schema "Device" trong doc/api/openapi.yaml: id, code, name, modality, department_id, ae_title.
 * (station_name, is_active KHONG co trong contract -> khong tra ve).
 * Khong co factory method tu model.Device o day - mapper.DeviceMapper lam viec do (R-05: dto khong
 * import model).
 */
public record DeviceResponse(
        Long id,
        String code,
        String name,
        Modality modality,
        Long departmentId,
        String aeTitle
) {
}
