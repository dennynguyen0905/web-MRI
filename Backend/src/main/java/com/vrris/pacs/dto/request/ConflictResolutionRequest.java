package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.ConflictResolution;
import jakarta.validation.constraints.NotNull;

/**
 * Khop schema "ConflictResolutionRequest" trong doc/api/openapi.yaml (POST /identity-conflicts/{id}/resolution).
 * manual_value bat buoc khi resolution = MANUAL_EDIT - kiem o service (nghiep vu, khong phai @Valid
 * dinh dang), giong cach dto.request.PatientCreateRequest.socialInsuranceNo dang lam voi nghi trung BHXH.
 */
public record ConflictResolutionRequest(
        @NotNull ConflictResolution resolution,
        String manualValue,
        String note
) {
}
