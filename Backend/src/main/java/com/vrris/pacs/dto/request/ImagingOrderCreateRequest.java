package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.PatientClass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Khop schema "ImagingOrderCreate" trong doc/api/openapi.yaml: tao imaging_order (source = MANUAL,
 * IN-02) kem it nhat mot study (service.impl.ImagingOrderServiceImpl.create goi StudyService.addToOrder
 * cho tung phan tu cua "studies"). Khong co order_code (sinh tu dong, rule.OrderCodeGenerator).
 */
public record ImagingOrderCreateRequest(
        @NotNull Long patientId,
        String visitCode,
        @NotNull Long orderingDepartmentId,
        @NotBlank String orderingDoctorName,
        @NotNull PatientClass patientClass,
        String room,
        String bed,
        String clinicalDiagnosis,
        LocalDateTime orderedAt,
        @NotEmpty @Valid List<StudyCreateRequest> studies
) {
}
