package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.StudyUpdateRequest;
import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.StatusCountsResponse;
import com.vrris.pacs.dto.response.StatusHistoryEntry;
import com.vrris.pacs.dto.response.StudyDetailResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.TransitionResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.service.StudyExportService;
import com.vrris.pacs.service.StudyService;
import com.vrris.pacs.service.StudyTransitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.vrris.pacs.rest.RestSupport.READ_ROLES;
import static com.vrris.pacs.rest.RestSupport.TRANSITION_ROLES;
import static com.vrris.pacs.rest.RestSupport.WRITE_ORDER_ROLES;
import static com.vrris.pacs.rest.RestSupport.attachment;

@RestController
@RequestMapping("/studies")
@Tag(name = "studies", description = "Quan ly ca chup")
public class StudyController {

    private final StudyService studyService;
    private final StudyTransitionService studyTransitionService;
    private final StudyExportService studyExportService;

    public StudyController(StudyService studyService, StudyTransitionService studyTransitionService,
                           StudyExportService studyExportService) {
        this.studyService = studyService;
        this.studyTransitionService = studyTransitionService;
        this.studyExportService = studyExportService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Worklist ca chup (IN-07)")
    @ApiResponse(responseCode = "200", description = "Mot trang worklist")
    @PreAuthorize(READ_ROLES)
    public StudyPageResponse listStudies(
            @Parameter(description = "PENDING | DONE") @RequestParam(required = false) String tab,
            @Parameter(description = "Loc theo mot hoac nhieu trang thai") @RequestParam(required = false) List<String> status,
            @Parameter(description = "Khoa/nhom; gom ca nhanh con") @RequestParam(name = "department_id", required = false) List<Long> departmentId,
            @Parameter(description = "Loc theo may") @RequestParam(name = "device_id", required = false) List<Long> deviceId,
            @Parameter(description = "Loc theo modality") @RequestParam(required = false) String modality,
            @Parameter(description = "Loc theo patient_class") @RequestParam(name = "patient_class", required = false) String patientClass,
            @Parameter(description = "SCHEDULED | ORDERED | COMPLETED") @RequestParam(name = "time_field", required = false) String timeField,
            @Parameter(description = "Tu ngay") @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Den ngay") @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Tim theo ma benh nhan") @RequestParam(name = "patient_code", required = false) String patientCode,
            @Parameter(description = "Tim theo ho ten benh nhan") @RequestParam(name = "full_name", required = false) String fullName,
            @Parameter(description = "Tim theo ma LSVV") @RequestParam(name = "visit_code", required = false) String visitCode,
            @Parameter(description = "Tim theo ma phieu chi dinh") @RequestParam(name = "order_code", required = false) String orderCode,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize,
            @Parameter(description = "<field>,<asc|desc>") @RequestParam(required = false) String sort
    ) {
        return studyService.search(tab, status, departmentId, deviceId, modality, patientClass, timeField,
                dateFrom, dateTo, patientCode, fullName, visitCode, orderCode, page, pageSize, sort);
    }

    @GetMapping(path = "/status-counts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Dem so ca theo nhom trang thai (IN-09)")
    @ApiResponse(responseCode = "200", description = "So dem theo trang thai")
    @PreAuthorize(READ_ROLES)
    public StatusCountsResponse getStatusCounts(
            @Parameter(description = "Khoa/nhom; gom ca nhanh con") @RequestParam(name = "department_id", required = false) List<Long> departmentId,
            @Parameter(description = "Loc theo may") @RequestParam(name = "device_id", required = false) List<Long> deviceId,
            @Parameter(description = "Loc theo modality") @RequestParam(required = false) String modality,
            @Parameter(description = "Loc theo patient_class") @RequestParam(name = "patient_class", required = false) String patientClass,
            @Parameter(description = "SCHEDULED | ORDERED | COMPLETED") @RequestParam(name = "time_field", required = false) String timeField,
            @Parameter(description = "Tu ngay") @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Den ngay") @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Tim theo ma benh nhan") @RequestParam(name = "patient_code", required = false) String patientCode,
            @Parameter(description = "Tim theo ho ten benh nhan") @RequestParam(name = "full_name", required = false) String fullName,
            @Parameter(description = "Tim theo ma LSVV") @RequestParam(name = "visit_code", required = false) String visitCode,
            @Parameter(description = "Tim theo ma phieu chi dinh") @RequestParam(name = "order_code", required = false) String orderCode
    ) {
        return studyService.getStatusCounts(departmentId, deviceId, modality, patientClass, timeField,
                dateFrom, dateTo, patientCode, fullName, visitCode, orderCode);
    }

    @GetMapping(path = "/export", produces = "text/csv;charset=UTF-8")
    @Operation(summary = "Xuat danh sach ca chup dang CSV (UTF-8 kem BOM)")
    @ApiResponse(responseCode = "200", description = "File CSV (UTF-8 kem BOM)")
    @ApiResponse(responseCode = "422", description = "Bo loc khop qua nhieu dong (EXPORT_TOO_MANY_ROWS)")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<Resource> exportStudies(
            @RequestParam(name = "order_id", required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "device_id", required = false) Long deviceId,
            @RequestParam(required = false) String q,
            @RequestParam(name = "patient_code", required = false) String patientCode,
            @RequestParam(name = "full_name", required = false) String fullName,
            @RequestParam(required = false) String sort
    ) {
        ExportDocumentResponse doc = studyExportService.exportCsv(orderId, status, deviceId, q, patientCode, fullName, sort);
        return attachment(doc.fileName(), "text/csv;charset=UTF-8", doc.content());
    }

    @GetMapping(path = "/{studyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet mot ca chup (IN-08)")
    @ApiResponse(responseCode = "200", description = "Chi tiet ca chup")
    @PreAuthorize(READ_ROLES)
    public StudyDetailResponse getStudy(@PathVariable Long studyId) {
        return studyService.getDetailById(studyId);
    }

    @GetMapping(path = "/{studyId}/viewer-link", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lay link mo OHIF Viewer cho ca chup")
    @ApiResponse(responseCode = "200", description = "Link mo OHIF Viewer")
    @ApiResponse(responseCode = "422", description = "Ca chup chua co anh trong PACS")
    @PreAuthorize(READ_ROLES)
    public ViewerLinkResponse getViewerLink(@PathVariable Long studyId) {
        return studyService.getViewerLink(studyId);
    }

    @PatchMapping(path = "/{studyId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sua thong tin ca chup")
    @ApiResponse(responseCode = "200", description = "Da cap nhat")
    @ApiResponse(responseCode = "400", description = "study_instance_uid khong dung dinh dang DICOM UID")
    @ApiResponse(responseCode = "403", description = "CLINICIAN dat study_instance_uid hoac sai vai tro")
    @ApiResponse(responseCode = "409", description = "Ca da APPROVED/CANCELLED hoac study_instance_uid trung ca khac")
    @PreAuthorize(WRITE_ORDER_ROLES)
    public StudyResponse updateStudy(@PathVariable Long studyId, @Valid @RequestBody StudyUpdateRequest request) {
        return studyService.update(studyId, request);
    }

    @PostMapping(path = "/{studyId}/transitions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chuyen trang thai ca chup (T2..T10)")
    @ApiResponse(responseCode = "200", description = "Da chuyen trang thai")
    @PreAuthorize(TRANSITION_ROLES)
    public TransitionResponse transitionStudy(@PathVariable Long studyId, @Valid @RequestBody TransitionRequest request) {
        return studyTransitionService.transition(studyId, request);
    }

    @GetMapping(path = "/{studyId}/status-history", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lich su chuyen trang thai ca chup")
    @ApiResponse(responseCode = "200", description = "Danh sach lich su chuyen trang thai")
    @PreAuthorize(READ_ROLES)
    public List<StatusHistoryEntry> listStatusHistory(@PathVariable Long studyId) {
        return studyTransitionService.listStatusHistory(studyId);
    }
}
