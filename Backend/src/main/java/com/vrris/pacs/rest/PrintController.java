package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.PrintEventRequest;
import com.vrris.pacs.dto.response.PrintDocumentResponse;
import com.vrris.pacs.enu.PrintSection;
import com.vrris.pacs.service.PrintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * In phieu ket qua cua mot benh nhan (Task 5).
 *
 * CHUA CO trong doc/api/openapi.yaml v0.1 - hai endpoint duoi day la DE XUAT di kem task, can
 * bo sung vao contract va doc/06-rbac-matrix.md muc 3 truoc khi coi la chot. Vai tro duoc gan:
 * ca 5 vai tro, cung tap voi GET /studies/{id}/reports/current - ai doc duoc bao cao tren man hinh
 * thi in ra khong lo them du lieu nao; doi lai moi lan in deu ghi audit_log PRINT kem patient_id.
 */
@RestController
@Tag(name = "printing", description = "In phieu ket qua cua benh nhan")
public class PrintController {

    private static final String PRINT_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";

    private final PrintService printService;

    public PrintController(PrintService printService) {
        this.printService = printService;
    }

    @GetMapping(path = "/patients/{patientId}/print-documents", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Tai mot phan phieu ket qua dang PDF",
            description = "Moi lan goi tra MOT file cho MOT phan (moi phan la mot to rieng). "
                    + "Ghi audit_log action PRINT kem patient_id.")
    @ApiResponse(responseCode = "200", description = "File PDF")
    @PreAuthorize(PRINT_ROLES)
    public ResponseEntity<Resource> printDocument(
            @PathVariable Long patientId,
            @Parameter(description = "Ca chup can in") @RequestParam(name = "study_id") Long studyId,
            @Parameter(description = "Phan can in") @RequestParam PrintSection section
    ) {
        PrintDocumentResponse document = printService.render(patientId, studyId, section);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(document.content().length)
                .body(new ByteArrayResource(document.content()));
    }

    @PostMapping(path = "/patients/{patientId}/print-events", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Ghi nhan mot lan in bang hop thoai cua trinh duyet",
            description = "Khong tra file. Dung khi nguoi dung in/luu PDF bang trinh duyet - "
                    + "du lieu van roi khoi he thong nen van phai ghi audit_log.")
    @ApiResponse(responseCode = "204", description = "Da ghi nhan")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize(PRINT_ROLES)
    public void recordPrintEvent(@PathVariable Long patientId, @Valid @RequestBody PrintEventRequest request) {
        printService.recordPrintEvent(patientId, request.studyId(), request.sections());
    }
}
