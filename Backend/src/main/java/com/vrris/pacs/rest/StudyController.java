package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.StatusCountsResponse;
import com.vrris.pacs.dto.response.StudyDetailResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * /studies theo doc/api/openapi.yaml - operationId listStudies (IN-07, worklist) day du ve PHAN
 * LOC/TRANG/SAP XEP; getStudy (IN-08) da ghep thanh StudyDetail day du.
 *
 * listStudies: 2 tab PENDING/DONE (03-state-machine.md bang 2.1), loc theo status[]/department_id[]
 * (gom ca nhanh con)/device_id[]/modality/patient_class/khoang thoi gian theo time_field/patient_code/
 * full_name/visit_code/order_code, phan trang + sap xep - xem Javadoc service.StudyService.search.
 * Response van tra StudyResponse (khong ghep patient/order/service_item/device/current_report thanh
 * WorklistRow nhu openapi "WorklistPage" mo ta) - CHUA lam, ngoai pham vi IN-07 lan sua nay (chi
 * sua phan loc/tim o StudyController.listStudies, xem phieu viec IN-07).
 *
 * getStatusCounts (IN-09, GET /studies/status-counts): dem so ca theo 7 nhom trang thai cua
 * enu.StudyStatus, dung CUNG bo loc voi listStudies TRU status[]/tab (khop mo ta openapi.yaml
 * operationId getStudyStatusCounts) - xem Javadoc service.StudyService.getStatusCounts.
 *
 * getStudy (IN-08, GET /studies/{id}): tra StudyDetail day du (join patient/order/service_item/
 * device/current_report), khong con la ban rut gon - xem Javadoc service.StudyService.getDetailById.
 *
 * Tao study: xem rest.ImagingOrderController (createImagingOrder, addStudyToOrder) - Study khong
 * co endpoint tao rieng ngoai hai cho do, dung theo doc/api/openapi.yaml (khong co POST /studies).
 */
@RestController
@Tag(name = "studies", description = "Quan ly ca chup")
public class StudyController {

    private static final String READ_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @GetMapping(path = "/studies", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Worklist ca chup (IN-07)", description = "2 tab PENDING/DONE; loc theo status/department/device/modality/patient_class/khoang thoi gian/benh nhan; phan trang + sap xep. Response van la StudyResponse (ban rut gon), chua ghep thanh WorklistRow.")
    @ApiResponse(responseCode = "200", description = "Mot trang worklist")
    @PreAuthorize(READ_ROLES)
    public StudyPageResponse listStudies(
            @Parameter(description = "PENDING (chi SCHEDULED) hoac DONE (cac trang thai con lai)") @RequestParam(required = false) String tab,
            @Parameter(description = "Loc theo mot hoac nhieu trang thai") @RequestParam(required = false) List<String> status,
            @Parameter(description = "Khoa/nhom; gom ca nhanh con") @RequestParam(name = "department_id", required = false) List<Long> departmentId,
            @Parameter(description = "Loc theo may") @RequestParam(name = "device_id", required = false) List<Long> deviceId,
            @Parameter(description = "Loc theo modality") @RequestParam(required = false) String modality,
            @Parameter(description = "Loc theo patient_class") @RequestParam(name = "patient_class", required = false) String patientClass,
            @Parameter(description = "SCHEDULED | ORDERED | COMPLETED - cot dung voi date_from/date_to, mac dinh SCHEDULED") @RequestParam(name = "time_field", required = false) String timeField,
            @Parameter(description = "Tu ngay, gom ca ngay nay") @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Den ngay, gom ca ngay nay") @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Tim theo ma benh nhan (chua dung ca tri)") @RequestParam(name = "patient_code", required = false) String patientCode,
            @Parameter(description = "Tim theo ho ten benh nhan (chua dung ca tri)") @RequestParam(name = "full_name", required = false) String fullName,
            @Parameter(description = "Tim theo ma LSVV") @RequestParam(name = "visit_code", required = false) String visitCode,
            @Parameter(description = "Tim theo ma phieu chi dinh") @RequestParam(name = "order_code", required = false) String orderCode,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize,
            @Parameter(description = "<field>,<asc|desc>, vi du scheduled_at,asc") @RequestParam(required = false) String sort
    ) {
        return studyService.search(tab, status, departmentId, deviceId, modality, patientClass, timeField,
                dateFrom, dateTo, patientCode, fullName, visitCode, orderCode, page, pageSize, sort);
    }

    @GetMapping(path = "/studies/status-counts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Dem so ca theo nhom trang thai (IN-09)", description = "Ap dung cung bo loc voi listStudies, tru status/tab. Tinh tu DB tai thoi diem goi, khong phai hang so.")
    @ApiResponse(responseCode = "200", description = "So dem theo trang thai")
    @PreAuthorize(READ_ROLES)
    public StatusCountsResponse getStatusCounts(
            @Parameter(description = "Khoa/nhom; gom ca nhanh con") @RequestParam(name = "department_id", required = false) List<Long> departmentId,
            @Parameter(description = "Loc theo may") @RequestParam(name = "device_id", required = false) List<Long> deviceId,
            @Parameter(description = "Loc theo modality") @RequestParam(required = false) String modality,
            @Parameter(description = "Loc theo patient_class") @RequestParam(name = "patient_class", required = false) String patientClass,
            @Parameter(description = "SCHEDULED | ORDERED | COMPLETED - cot dung voi date_from/date_to, mac dinh SCHEDULED") @RequestParam(name = "time_field", required = false) String timeField,
            @Parameter(description = "Tu ngay, gom ca ngay nay") @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Den ngay, gom ca ngay nay") @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Tim theo ma benh nhan (chua dung ca tri)") @RequestParam(name = "patient_code", required = false) String patientCode,
            @Parameter(description = "Tim theo ho ten benh nhan (chua dung ca tri)") @RequestParam(name = "full_name", required = false) String fullName,
            @Parameter(description = "Tim theo ma LSVV") @RequestParam(name = "visit_code", required = false) String visitCode,
            @Parameter(description = "Tim theo ma phieu chi dinh") @RequestParam(name = "order_code", required = false) String orderCode
    ) {
        return studyService.getStatusCounts(departmentId, deviceId, modality, patientClass, timeField,
                dateFrom, dateTo, patientCode, fullName, visitCode, orderCode);
    }

    @GetMapping(path = "/studies/{studyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet mot ca chup (IN-08)", description = "Gom benh nhan, chi dinh, dich vu, may, bao cao hien hanh (current_report). Tra study_instance_uid de client mo OHIF bang deep-link; khong tra anh.")
    @ApiResponse(responseCode = "200", description = "Chi tiet ca chup")
    @PreAuthorize(READ_ROLES)
    public StudyDetailResponse getStudy(@PathVariable Long studyId) {
        return studyService.getDetailById(studyId);
    }

    @GetMapping(path = "/studies/{studyId}/viewer-link", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lay link mo OHIF Viewer cho ca chup", description = "Kiem tra study_instance_uid, ghi audit_log VIEW_IMAGE, ghep base URL OHIF.")
    @ApiResponse(responseCode = "200", description = "Link mo OHIF Viewer")
    @ApiResponse(responseCode = "422", description = "Ca chup chua co anh trong PACS (study_instance_uid is null)")
    @PreAuthorize(READ_ROLES)
    public ViewerLinkResponse getViewerLink(@PathVariable Long studyId) {
        return studyService.getViewerLink(studyId);
    }
}
