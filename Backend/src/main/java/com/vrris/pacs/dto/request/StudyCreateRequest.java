package com.vrris.pacs.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Khop schema "StudyCreate" trong doc/api/openapi.yaml: khong co accession_number (sinh tu dong,
 * rule.AccessionNumberGenerator) va khong co status (luon SCHEDULED khi tao moi).
 */
public record StudyCreateRequest(
        @NotNull Long serviceItemId,
        Long deviceId,
        @NotNull LocalDateTime scheduledAt
) {
}
