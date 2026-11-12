package com.vrris.pacs.dto.request;

/**
 * Khop schema "ReportWrite" trong doc/api/openapi.yaml (5 truong, IN-10). Luu nhap khong bat buoc
 * du truong - dieu kien technique/conclusion khong rong chi kiem khi chuyen READING -> READ (T5,
 * rule.StudyStateMachine, ngoai pham vi IN-10), nen khong co @NotBlank o day.
 */
public record ReportWriteRequest(
        String technique,
        String findings,
        String diagnosis,
        String conclusion,
        String recommendation
) {
}
