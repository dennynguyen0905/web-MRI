package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Khop schema "PatientCreate" trong doc/api/openapi.yaml: khong co "age", khong co "patient_code"
 * trung (kiem o service.impl.PatientServiceImpl -> 409 PATIENT_DUPLICATE_CODE, khong phai @Valid).
 */
public record PatientCreateRequest(
        @NotBlank String patientCode,
        @NotBlank String fullName,
        LocalDate dateOfBirth,
        @NotNull DobPrecision dobPrecision,
        @NotNull Gender gender,
        String socialInsuranceNo,
        String phone,
        String address
) {
}
