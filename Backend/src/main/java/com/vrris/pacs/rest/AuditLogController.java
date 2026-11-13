package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.AuditLogPageResponse;
import com.vrris.pacs.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Tag(name = "audit-logs", description = "Nhat ky truy vet he thong")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping(path = "/audit-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Xem nhat ky truy vet (IN-15)", description = "Append-only, chi doc; chi ADMIN. Truy vet theo benh nhan qua patient_id.")
    @ApiResponse(responseCode = "200", description = "Mot trang nhat ky")
    @ApiResponse(responseCode = "401", description = "Chua dang nhap")
    @ApiResponse(responseCode = "403", description = "Khong phai ADMIN")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AuditLogPageResponse listAuditLogs(
            @Parameter(description = "Loc theo benh nhan") @RequestParam(name = "patient_id", required = false) Long patientId,
            @Parameter(description = "Loc theo nguoi thuc hien") @RequestParam(name = "actor_user_id", required = false) Long actorUserId,
            @Parameter(description = "Loc theo hanh vi") @RequestParam(required = false) String action,
            @Parameter(description = "Loc theo loai thuc the") @RequestParam(name = "entity_type", required = false) String entityType,
            @Parameter(description = "Loc theo id thuc the") @RequestParam(name = "entity_id", required = false) Long entityId,
            @Parameter(description = "Tu thoi diem") @RequestParam(name = "occurred_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredFrom,
            @Parameter(description = "Den thoi diem") @RequestParam(name = "occurred_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredTo,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize
    ) {
        return auditLogService.search(patientId, actorUserId, action, entityType, entityId, occurredFrom, occurredTo, page, pageSize);
    }
}
