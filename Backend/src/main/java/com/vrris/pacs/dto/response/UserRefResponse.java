package com.vrris.pacs.dto.response;

/**
 * Tham chieu nguoi dung rut gon (UserRef trong doc/api/openapi.yaml va Frontend/src/shared/api/dto.ts).
 */
public record UserRefResponse(
        Long id,
        String username,
        String fullName
) {
}

