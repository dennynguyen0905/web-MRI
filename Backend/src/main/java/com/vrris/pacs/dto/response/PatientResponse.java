package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Khop schema "Patient" trong doc/api/openapi.yaml. "age"/"ageApproximate" KHONG duoc MapStruct
 * sinh (mapper.PatientMapper bo qua hai field nay) - service.impl.PatientServiceImpl tinh bang
 * rule.AgeCalculator roi dung lai record nay voi gia tri da tinh (R-05: logic nghiep vu nam o
 * service/rule, khong nam o mapper).
 */
public record PatientResponse(
        Long id,
        String patientCode,
        String hisPatientId,
        String fullName,
        LocalDate dateOfBirth,
        DobPrecision dobPrecision,
        Integer age,
        Boolean ageApproximate,
        Gender gender,
        String socialInsuranceNo,
        String phone,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
