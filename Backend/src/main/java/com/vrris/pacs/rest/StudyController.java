package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * /studies theo doc/api/openapi.yaml - BAN RUT GON cua operationId listStudies/getStudy.
 *
 * listStudies day du la worklist IN-07 (2 tab PENDING/DONE, bo loc khoa/nhom, modality, patient_class,
 * patient_code, full_name, khoang thoi gian theo time_field, dem theo nhom trang thai IN-09) va
 * getStudy day du la StudyDetail (join patient/order/service_item/device/current_report) - CHUA lam
 * o day, ngoai pham vi "entity + DAO + mot luong rest->service->dao->model" cua lan nay (xem
 * Javadoc dao.StudyDao.SearchCriteria va dto.response.StudyResponse).
 *
 * Tao study: xem rest.ImagingOrderController (createImagingOrder, addStudyToOrder) - Study khong
 * co endpoint tao rieng ngoai hai cho do, dung theo doc/api/openapi.yaml (khong co POST /studies).
 */
@RestController
@Tag(name = "studies", description = "Quan ly ca chup (ban rut gon)")
public class StudyController {

    private static final String READ_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @GetMapping(path = "/studies", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach ca chup (ban rut gon)", description = "Loc theo order_id/status/device_id. Day du hon la worklist IN-07, chua lam trong pham vi nay.")
    @ApiResponse(responseCode = "200", description = "Mot trang danh sach ca chup")
    @PreAuthorize(READ_ROLES)
    public StudyPageResponse listStudies(
            @Parameter(description = "Loc theo chi dinh") @RequestParam(name = "order_id", required = false) Long orderId,
            @Parameter(description = "Loc theo trang thai") @RequestParam(required = false) String status,
            @Parameter(description = "Loc theo may") @RequestParam(name = "device_id", required = false) Long deviceId,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize,
            @Parameter(description = "<field>,<asc|desc>, vi du scheduled_at,asc") @RequestParam(required = false) String sort
    ) {
        return studyService.search(orderId, status, deviceId, page, pageSize, sort);
    }

    @GetMapping(path = "/studies/export", produces = "text/csv;charset=UTF-8")
    @Operation(
            summary = "Xuat danh sach ca chup / benh nhan tren worklist dang CSV (UTF-8 kem BOM)",
            description = "De xuat IN-25 (doc/01-scope.md muc 3.1 & muc 9, doc/06-rbac-matrix.md muc 3). "
                    + "Ghi audit_log action EXPORT (patient_id co neu 1 benh nhan, NULL neu nhieu - doc/09 muc 2.2).")
    @ApiResponse(responseCode = "200", description = "File CSV (UTF-8 kem BOM)")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<Resource> exportStudies(
            @Parameter(description = "Loc theo chi dinh") @RequestParam(name = "order_id", required = false) Long orderId,
            @Parameter(description = "Loc theo trang thai") @RequestParam(required = false) String status,
            @Parameter(description = "Loc theo may") @RequestParam(name = "device_id", required = false) Long deviceId,
            @Parameter(description = "Tim tu do tren ma benh nhan, ho ten, ma phieu, ma ho so") @RequestParam(required = false) String q,
            @Parameter(description = "Loc theo ma benh nhan") @RequestParam(name = "patient_code", required = false) String patientCode,
            @Parameter(description = "Loc theo ho ten") @RequestParam(name = "full_name", required = false) String fullName,
            @Parameter(description = "<field>,<asc|desc>, vi du scheduled_at,asc") @RequestParam(required = false) String sort
    ) {
        ExportDocumentResponse document = studyService.exportCsv(
                orderId, status, deviceId, q, patientCode, fullName, sort);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(document.content().length)
                .body(new ByteArrayResource(document.content()));
    }

    @GetMapping(path = "/studies/{studyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet mot ca chup (ban rut gon)", description = "Chua join patient/order/service_item/device/current_report nhu StudyDetail day du.")
    @ApiResponse(responseCode = "200", description = "Ca chup")
    @PreAuthorize(READ_ROLES)
    public StudyResponse getStudy(@PathVariable Long studyId) {
        return studyService.getById(studyId);
    }
}
