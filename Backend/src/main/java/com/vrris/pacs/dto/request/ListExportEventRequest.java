package com.vrris.pacs.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Ghi nhan su kien xuat hoac in danh sach benh nhan / ca chup tu trinh duyet (IN-25).
 */
public record ListExportEventRequest(
        @NotBlank(message = "target khong duoc de trong")
        @Pattern(regexp = "PATIENTS|STUDIES", message = "target phai la PATIENTS hoac STUDIES")
        String target,

        @NotBlank(message = "mode khong duoc de trong")
        @Pattern(regexp = "EXPORT|PRINT", message = "mode phai la EXPORT hoac PRINT")
        String mode,

        @JsonProperty("row_count")
        @Min(value = 0, message = "row_count khong duoc am")
        Integer rowCount,

        @JsonProperty("patient_id")
        Long patientId
) {
}

