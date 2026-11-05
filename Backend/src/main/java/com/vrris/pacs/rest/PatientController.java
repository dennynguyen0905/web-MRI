package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.response.PatientPageResponse;
import com.vrris.pacs.dto.response.PatientResponse;
import com.vrris.pacs.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 4 endpoint /patients theo doc/api/openapi.yaml (operationId listPatients/getPatient/
 * createPatient/updatePatient): danh sach + tim theo ma/ten, xem, tao (chan trung patient_code -
 * IN-01), sua tung phan. x-allowed-roles cua openapi.yaml: doc (ca 5 vai tro), ghi (ADMIN/
 * CLINICIAN/TECHNICIAN).
 */
@RestController
@Tag(name = "patients", description = "Quan ly benh nhan")
public class PatientController {

    private static final String READ_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";
    private static final String WRITE_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN')";

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping(path = "/patients", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tim benh nhan", description = "Tim theo ma, ho ten hoac BHXH (tham so q), hoac loc rieng patient_code/full_name.")
    @ApiResponse(responseCode = "200", description = "Danh sach benh nhan")
    @PreAuthorize(READ_ROLES)
    public PatientPageResponse listPatients(
            @Parameter(description = "Tim tu do tren ma benh nhan, ho ten, BHXH") @RequestParam(required = false) String q,
            @Parameter(description = "Loc theo ma benh nhan") @RequestParam(name = "patient_code", required = false) String patientCode,
            @Parameter(description = "Loc theo ho ten") @RequestParam(name = "full_name", required = false) String fullName,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize,
            @Parameter(description = "<field>,<asc|desc>, vi du full_name,asc") @RequestParam(required = false) String sort
    ) {
        return patientService.search(q, patientCode, fullName, page, pageSize, sort);
    }

    @GetMapping(path = "/patients/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet benh nhan")
    @ApiResponse(responseCode = "200", description = "Benh nhan")
    @PreAuthorize(READ_ROLES)
    public PatientResponse getPatient(@PathVariable Long patientId) {
        return patientService.getById(patientId);
    }

    @PostMapping(path = "/patients", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tao benh nhan", description = "Chan trung patient_code (IN-01). Khong nhan age.")
    @ApiResponse(responseCode = "201", description = "Da tao")
    @PreAuthorize(WRITE_ROLES)
    public PatientResponse createPatient(@Valid @RequestBody PatientCreateRequest request) {
        return patientService.create(request);
    }

    @PatchMapping(path = "/patients/{patientId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sua thong tin benh nhan", description = "Cap nhat tung phan. Khong doi duoc patient_code.")
    @ApiResponse(responseCode = "200", description = "Da cap nhat")
    @PreAuthorize(WRITE_ROLES)
    public PatientResponse updatePatient(@PathVariable Long patientId, @Valid @RequestBody PatientUpdateRequest request) {
        return patientService.update(patientId, request);
    }
}
