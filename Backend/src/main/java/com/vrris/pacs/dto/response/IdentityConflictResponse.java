package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.ConflictSource;

import java.time.LocalDateTime;

/**
 * Khop schema "IdentityConflict" trong doc/api/openapi.yaml. "resolvedBy" KHONG duoc MapStruct sinh
 * (mapper.IdentityConflictMapper bo qua field nay) - service.impl.IdentityConflictServiceImpl ghep
 * vao sau bang AppUserDao.findById(resolved_by_user_id), giong cach PatientResponse ghep "age".
 */
public record IdentityConflictResponse(
        Long id,
        Long patientId,
        Long orderId,
        ConflictSource source,
        String fieldName,
        String risValue,
        String hisValue,
        LocalDateTime detectedAt,
        ConflictResolution resolution,
        UserRef resolvedBy,
        LocalDateTime resolvedAt
) {
}
