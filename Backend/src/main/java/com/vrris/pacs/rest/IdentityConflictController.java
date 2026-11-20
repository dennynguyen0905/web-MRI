package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.ConflictResolutionRequest;
import com.vrris.pacs.dto.response.IdentityConflictPageResponse;
import com.vrris.pacs.dto.response.IdentityConflictResponse;
import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.ConflictSource;
import com.vrris.pacs.service.IdentityConflictService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static com.vrris.pacs.rest.RestSupport.TECH_OR_ADMIN_ROLES;

@RestController
@RequestMapping("/identity-conflicts")
@Tag(name = "identity-conflicts", description = "Doi chieu danh tinh HIS <-> RIS")
public class IdentityConflictController {

    private final IdentityConflictService identityConflictService;

    public IdentityConflictController(IdentityConflictService identityConflictService) {
        this.identityConflictService = identityConflictService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach lech du lieu HIS <-> RIS")
    @ApiResponse(responseCode = "200", description = "Danh sach conflict")
    @PreAuthorize(TECH_OR_ADMIN_ROLES)
    public IdentityConflictPageResponse listIdentityConflicts(
            @RequestParam(required = false) ConflictResolution resolution,
            @RequestParam(required = false) ConflictSource source,
            @RequestParam(name = "field_name", required = false) String fieldName,
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "detected_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedFrom,
            @RequestParam(name = "detected_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String sort
    ) {
        return identityConflictService.search(resolution, source, fieldName, patientId, detectedFrom, detectedTo,
                page, pageSize, sort);
    }

    @GetMapping(path = "/{conflictId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet mot lech du lieu")
    @ApiResponse(responseCode = "200", description = "Conflict")
    @PreAuthorize(TECH_OR_ADMIN_ROLES)
    public IdentityConflictResponse getIdentityConflict(@PathVariable Long conflictId) {
        return identityConflictService.getById(conflictId);
    }

    @PostMapping(path = "/{conflictId}/resolution", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Xu ly mot lech du lieu")
    @ApiResponse(responseCode = "200", description = "Da xu ly")
    @PreAuthorize(TECH_OR_ADMIN_ROLES)
    public IdentityConflictResponse resolveIdentityConflict(@PathVariable Long conflictId,
                                                              @Valid @RequestBody ConflictResolutionRequest request) {
        return identityConflictService.resolve(conflictId, request);
    }
}
