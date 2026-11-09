package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(path = "/studies/{studyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet mot ca chup (ban rut gon)", description = "Chua join patient/order/service_item/device/current_report nhu StudyDetail day du.")
    @ApiResponse(responseCode = "200", description = "Ca chup")
    @PreAuthorize(READ_ROLES)
    public StudyResponse getStudy(@PathVariable Long studyId) {
        return studyService.getById(studyId);
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
