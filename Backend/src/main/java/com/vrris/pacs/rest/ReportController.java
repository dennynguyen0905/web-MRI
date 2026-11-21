package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.ReportWriteRequest;
import com.vrris.pacs.dto.response.ReportResponse;
import com.vrris.pacs.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.vrris.pacs.rest.RestSupport.RADIOLOGIST_ROLE;
import static com.vrris.pacs.rest.RestSupport.READ_ROLES;

@RestController
@RequestMapping("/studies/{studyId}/reports")
@Tag(name = "reports", description = "Bao cao ket qua ca chup")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Moi version bao cao cua ca chup")
    @ApiResponse(responseCode = "200", description = "Danh sach bao cao")
    @PreAuthorize(READ_ROLES)
    public List<ReportResponse> listReports(@PathVariable Long studyId) {
        return reportService.listVersions(studyId);
    }

    @GetMapping(path = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bao cao hien hanh")
    @ApiResponse(responseCode = "200", description = "Bao cao hien hanh")
    @ApiResponse(responseCode = "404", description = "Ca chup chua co bao cao")
    @PreAuthorize(READ_ROLES)
    public ReportResponse getCurrentReport(@PathVariable Long studyId) {
        return reportService.getCurrent(studyId);
    }

    @PutMapping(path = "/current", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Luu nhap bao cao hien hanh")
    @ApiResponse(responseCode = "200", description = "Da luu nhap")
    @ApiResponse(responseCode = "403", description = "Khong phai RADIOLOGIST dang giu ca")
    @ApiResponse(responseCode = "409", description = "Ca khong o READING hoac bao cao khong con la DRAFT/RETURNED")
    @PreAuthorize(RADIOLOGIST_ROLE)
    public ReportResponse saveCurrentReportDraft(@PathVariable Long studyId,
                                                   @Valid @RequestBody ReportWriteRequest request) {
        return reportService.updateCurrent(studyId, request);
    }
}
