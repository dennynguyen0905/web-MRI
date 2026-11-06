package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.SyncStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Khop schema "ImagingOrder" trong doc/api/openapi.yaml. "studies" KHONG duoc MapStruct sinh
 * (mapper.ImagingOrderMapper bo qua field nay) - service.impl.ImagingOrderServiceImpl ghep vao sau
 * bang StudyService.listByOrderId, giong cach PatientResponse ghep "age" (R-05: logic nghiep vu nam
 * o service, khong nam o mapper).
 */
public record ImagingOrderResponse(
        Long id,
        String orderCode,
        Long patientId,
        String visitCode,
        OrderSource source,
        String hisOrderId,
        SyncStatus syncStatus,
        Long orderingDepartmentId,
        String orderingDoctorName,
        PatientClass patientClass,
        String room,
        String bed,
        String clinicalDiagnosis,
        LocalDateTime orderedAt,
        List<StudyResponse> studies,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
