package com.vrris.pacs.dto.response;

/**
 * Khop schema "UserRef" trong doc/api/openapi.yaml - tham chieu gon toi mot app_user
 * (vi du IdentityConflict.resolved_by). Dung chung cho moi response can nhung khong doi
 * toan bo AppUser ra ngoai REST.
 */
public record UserRef(
        Long id,
        String username,
        String fullName
) {
}
