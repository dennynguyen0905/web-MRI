package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;

import java.time.LocalDate;

/**
 * Khop schema "PatientUpdate" trong doc/api/openapi.yaml: cap nhat tung phan, khong co "patient_code"
 * (khong doi duoc) va khong co "age". mapper.PatientMapper.updateEntityFromRequest bo qua field null
 * (NullValuePropertyMappingStrategy.IGNORE) - field khong gui trong JSON = giu nguyen gia tri cu.
 *
 * Don gian hoa co chu dich: field gui gia tri null tuong minh (vi du muon xoa date_of_birth) cung bi
 * coi la "khong doi", vi record thuan khong phan biet duoc "thieu field" voi "field = null" khi bind
 * JSON. Chua can JsonNullable<T> cho pham vi 4 endpoint nay.
 */
public record PatientUpdateRequest(
        String fullName,
        LocalDate dateOfBirth,
        DobPrecision dobPrecision,
        Gender gender,
        String socialInsuranceNo,
        String phone,
        String address
) {
}
