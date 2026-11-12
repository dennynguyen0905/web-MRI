package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.ConflictResolutionRequest;
import com.vrris.pacs.dto.response.IdentityConflictPageResponse;
import com.vrris.pacs.dto.response.IdentityConflictResponse;
import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.ConflictSource;
import com.vrris.pacs.service.IdentityConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * /identity-conflicts theo doc/api/openapi.yaml (operationId listIdentityConflicts/
 * getIdentityConflict/resolveIdentityConflict) - IN-05. x-allowed-roles: ca 3 endpoint chi ADMIN
 * va TECHNICIAN (doc/06-rbac-matrix.md muc 5, RBAC-11/RC-18) - RADIOLOGIST va cac vai tro khac
 * nhan 403, giong cach rest.ImagingOrderController dang lam voi WRITE_ROLES.
 */
@RestController
@Tag(name = "identity-conflicts", description = "Doi chieu danh tinh HIS <-> RIS")
public class IdentityConflictController {

    private static final String ROLES = "hasAnyAuthority('ROLE_ADMIN','ROLE_TECHNICIAN')";

    private final IdentityConflictService identityConflictService;

    public IdentityConflictController(IdentityConflictService identityConflictService) {
        this.identityConflictService = identityConflictService;
    }

    @GetMapping(path = "/identity-conflicts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach lech du lieu HIS <-> RIS", description = "Man 'Danh sach lech du lieu' (04-his-ris-reconciliation.md muc 8).")
    @ApiResponse(responseCode = "200", description = "Danh sach conflict")
    @PreAuthorize(ROLES)
    public IdentityConflictPageResponse listIdentityConflicts(
            @Parameter(description = "Loc theo trang thai xu ly") @RequestParam(required = false) ConflictResolution resolution,
            @Parameter(description = "Loc theo nguon") @RequestParam(required = false) ConflictSource source,
            @Parameter(description = "Loc theo ten truong") @RequestParam(name = "field_name", required = false) String fieldName,
            @Parameter(description = "Loc theo benh nhan") @RequestParam(name = "patient_id", required = false) Long patientId,
            @Parameter(description = "Tu thoi diem phat hien") @RequestParam(name = "detected_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedFrom,
            @Parameter(description = "Den thoi diem phat hien") @RequestParam(name = "detected_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedTo,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize,
            @Parameter(description = "<field>,<asc|desc>, vi du detected_at,desc") @RequestParam(required = false) String sort
    ) {
        return identityConflictService.search(resolution, source, fieldName, patientId, detectedFrom, detectedTo,
                page, pageSize, sort);
    }

    @GetMapping(path = "/identity-conflicts/{conflictId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet mot lech du lieu")
    @ApiResponse(responseCode = "200", description = "Conflict")
    @PreAuthorize(ROLES)
    public IdentityConflictResponse getIdentityConflict(@PathVariable Long conflictId) {
        return identityConflictService.getById(conflictId);
    }

    @PostMapping(path = "/identity-conflicts/{conflictId}/resolution",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Xu ly mot lech du lieu", description = "KEEP_RIS / TAKE_HIS / MANUAL_EDIT (04-his-ris-reconciliation.md muc 2).")
    @ApiResponse(responseCode = "200", description = "Da xu ly")
    @PreAuthorize(ROLES)
    public IdentityConflictResponse resolveIdentityConflict(@PathVariable Long conflictId,
                                                              @Valid @RequestBody ConflictResolutionRequest request) {
        return identityConflictService.resolve(conflictId, request);
    }
}
