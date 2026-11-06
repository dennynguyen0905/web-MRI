package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.PatientClass;

/**
 * Khop schema "ImagingOrderUpdate" trong doc/api/openapi.yaml: cap nhat tung phan, khong doi duoc
 * patient_id/order_code/studies o day. mapper.ImagingOrderMapper.updateEntityFromRequest bo qua
 * field null (giong cach dto.request.PatientUpdateRequest / mapper.PatientMapper dang lam).
 */
public record ImagingOrderUpdateRequest(
        String visitCode,
        Long orderingDepartmentId,
        String orderingDoctorName,
        PatientClass patientClass,
        String room,
        String bed,
        String clinicalDiagnosis
) {
}
