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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * /studies/{studyId}/reports theo doc/api/openapi.yaml (IN-10) - operationId listStudyReports,
 * getCurrentReport, saveCurrentReportDraft. Xem Javadoc service.ReportService cho logic chi tiet
 * (vai tro, dieu kien, ma loi).
 */
@RestController
@Tag(name = "reports", description = "Bao cao ket qua ca chup")
public class ReportController {

    private static final String READ_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";
    private static final String WRITE_ROLES = "hasAuthority('ROLE_RADIOLOGIST')";

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(path = "/studies/{studyId}/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Moi version bao cao cua ca chup", description = "Version lon nhat truoc. Ban cu khong bi sua/ghi de.")
    @ApiResponse(responseCode = "200", description = "Danh sach bao cao")
    @PreAuthorize(READ_ROLES)
    public List<ReportResponse> listReports(@PathVariable Long studyId) {
        return reportService.listVersions(studyId);
    }

    @GetMapping(path = "/studies/{studyId}/reports/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bao cao hien hanh", description = "Ban co version lon nhat.")
    @ApiResponse(responseCode = "200", description = "Bao cao hien hanh")
    @ApiResponse(responseCode = "404", description = "Ca chup chua co bao cao")
    @PreAuthorize(READ_ROLES)
    public ReportResponse getCurrentReport(@PathVariable Long studyId) {
        return reportService.getCurrent(studyId);
    }

    @PutMapping(path = "/studies/{studyId}/reports/current",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Luu nhap bao cao hien hanh", description = "Chi khi ca dang READING va nguoi "
            + "goi la nguoi dang doc (reading_user_id); bao cao hien hanh phai dang DRAFT. Khong bat "
            + "buoc du truong - dieu kien technique/conclusion chi kiem khi chuyen READING -> READ.")
    @ApiResponse(responseCode = "200", description = "Da luu nhap")
    @ApiResponse(responseCode = "403", description = "Khong phai RADIOLOGIST dang giu ca")
    @ApiResponse(responseCode = "409", description = "Ca khong o READING hoac bao cao khong con la DRAFT")
    @PreAuthorize(WRITE_ROLES)
    public ReportResponse saveCurrentReportDraft(@PathVariable Long studyId,
                                                   @Valid @RequestBody ReportWriteRequest request) {
        return reportService.updateCurrent(studyId, request);
    }
}
