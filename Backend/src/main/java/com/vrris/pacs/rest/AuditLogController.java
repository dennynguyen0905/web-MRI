package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.AuditLogPageResponse;
import com.vrris.pacs.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static com.vrris.pacs.rest.RestSupport.ADMIN_ROLE;

@RestController
@RequestMapping("/audit-logs")
@Tag(name = "audit-logs", description = "Nhat ky truy vet he thong")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Xem nhat ky truy vet (IN-15)")
    @ApiResponse(responseCode = "200", description = "Mot trang nhat ky")
    @PreAuthorize(ADMIN_ROLE)
    public AuditLogPageResponse listAuditLogs(
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "actor_user_id", required = false) Long actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(name = "entity_type", required = false) String entityType,
            @RequestParam(name = "entity_id", required = false) Long entityId,
            @RequestParam(name = "occurred_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredFrom,
            @RequestParam(name = "occurred_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize
    ) {
        return auditLogService.search(patientId, actorUserId, action, entityType, entityId, occurredFrom, occurredTo, page, pageSize);
    }
}
