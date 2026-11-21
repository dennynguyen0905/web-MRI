package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.ListExportEventRequest;
import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.PatientPageResponse;
import com.vrris.pacs.dto.response.PatientResponse;
import com.vrris.pacs.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.vrris.pacs.rest.RestSupport.READ_ROLES;
import static com.vrris.pacs.rest.RestSupport.WRITE_ORDER_ROLES;
import static com.vrris.pacs.rest.RestSupport.attachment;

@RestController
@RequestMapping("/patients")
@Tag(name = "patients", description = "Quan ly benh nhan")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tim benh nhan")
    @ApiResponse(responseCode = "200", description = "Danh sach benh nhan")
    @PreAuthorize(READ_ROLES)
    public PatientPageResponse listPatients(
            @RequestParam(required = false) String q,
            @RequestParam(name = "patient_code", required = false) String patientCode,
            @RequestParam(name = "full_name", required = false) String fullName,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String sort
    ) {
        return patientService.search(q, patientCode, fullName, page, pageSize, sort);
    }

    @GetMapping(path = "/export", produces = "text/csv;charset=UTF-8")
    @Operation(summary = "Xuat danh sach benh nhan dang CSV (UTF-8 kem BOM)")
    @ApiResponse(responseCode = "200", description = "File CSV (UTF-8 kem BOM)")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<Resource> exportPatients(
            @RequestParam(required = false) String q,
            @RequestParam(name = "patient_code", required = false) String patientCode,
            @RequestParam(name = "full_name", required = false) String fullName,
            @RequestParam(required = false) String sort
    ) {
        ExportDocumentResponse doc = patientService.exportCsv(q, patientCode, fullName, sort);
        return attachment(doc.fileName(), "text/csv;charset=UTF-8", doc.content());
    }

    @PostMapping(path = "/export-events", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ghi nhan mot lan xuat hoac in danh sach tu trinh duyet")
    @ApiResponse(responseCode = "204", description = "Da ghi nhan")
    @PreAuthorize(READ_ROLES)
    public void recordExportEvent(@Valid @RequestBody ListExportEventRequest request) {
        patientService.recordExportEvent(request);
    }

    @GetMapping(path = "/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet benh nhan")
    @ApiResponse(responseCode = "200", description = "Benh nhan")
    @PreAuthorize(READ_ROLES)
    public PatientResponse getPatient(@PathVariable Long patientId) {
        return patientService.getById(patientId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tao benh nhan")
    @ApiResponse(responseCode = "201", description = "Da tao")
    @PreAuthorize(WRITE_ORDER_ROLES)
    public PatientResponse createPatient(@Valid @RequestBody PatientCreateRequest request) {
        return patientService.create(request);
    }

    @PatchMapping(path = "/{patientId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sua thong tin benh nhan")
    @ApiResponse(responseCode = "200", description = "Da cap nhat")
    @PreAuthorize(WRITE_ORDER_ROLES)
    public PatientResponse updatePatient(@PathVariable Long patientId, @Valid @RequestBody PatientUpdateRequest request) {
        return patientService.update(patientId, request);
    }
}
