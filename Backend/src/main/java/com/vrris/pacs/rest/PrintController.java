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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.vrris.pacs.rest.RestSupport.READ_ROLES;
import static com.vrris.pacs.rest.RestSupport.attachment;

@RestController
@RequestMapping("/patients/{patientId}")
@Tag(name = "printing", description = "In phieu ket qua cua benh nhan")
public class PrintController {

    private final PrintService printService;

    public PrintController(PrintService printService) {
        this.printService = printService;
    }

    @GetMapping(path = "/print-documents", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Tai mot phan phieu ket qua dang PDF")
    @ApiResponse(responseCode = "200", description = "File PDF")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<Resource> printDocument(
            @PathVariable Long patientId,
            @Parameter(description = "Ca chup can in") @RequestParam(name = "study_id") Long studyId,
            @Parameter(description = "Phan can in") @RequestParam PrintSection section
    ) {
        PrintDocumentResponse doc = printService.render(patientId, studyId, section);
        return attachment(doc.fileName(), MediaType.APPLICATION_PDF_VALUE, doc.content());
    }

    @PostMapping(path = "/print-events", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ghi nhan mot lan in bang hop thoai cua trinh duyet")
    @ApiResponse(responseCode = "204", description = "Da ghi nhan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(READ_ROLES)
    public void recordPrintEvent(@PathVariable Long patientId, @Valid @RequestBody PrintEventRequest request) {
        printService.recordPrintEvent(patientId, request.studyId(), request.sections());
    }
}
