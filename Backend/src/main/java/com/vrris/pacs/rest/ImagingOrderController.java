package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.ImagingOrderCreateRequest;
import com.vrris.pacs.dto.request.ImagingOrderUpdateRequest;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.ImagingOrderPageResponse;
import com.vrris.pacs.dto.response.ImagingOrderResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.service.ImagingOrderService;
import com.vrris.pacs.service.StudyService;
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
 * /imaging-orders theo doc/api/openapi.yaml (operationId listImagingOrders/getImagingOrder/
 * createImagingOrder/updateImagingOrder/addStudyToOrder) - IN-02. x-allowed-roles: doc (ca 5 vai
 * tro), ghi (ADMIN/CLINICIAN/TECHNICIAN) - giong cach rest.PatientController.
 *
 * addStudyToOrder goi StudyService (khong qua ImagingOrderService) vi day la hanh vi tren entity
 * Study (them mot study vao mot order da co) - rest duoc phep phu thuoc nhieu interface service
 * (L1 chi cam dao/hibernateDao/model, khong cam nhieu service).
 */
@RestController
@Tag(name = "imaging-orders", description = "Quan ly chi dinh")
public class ImagingOrderController {

    private static final String READ_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";
    private static final String WRITE_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN')";

    private final ImagingOrderService imagingOrderService;
    private final StudyService studyService;

    public ImagingOrderController(ImagingOrderService imagingOrderService, StudyService studyService) {
        this.imagingOrderService = imagingOrderService;
        this.studyService = studyService;
    }

    @GetMapping(path = "/imaging-orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach chi dinh", description = "Loc theo patient_id/order_code/visit_code (dung cho bang 'phieu cung benh nhan' IN-08).")
    @ApiResponse(responseCode = "200", description = "Danh sach chi dinh")
    @PreAuthorize(READ_ROLES)
    public ImagingOrderPageResponse listImagingOrders(
            @Parameter(description = "Loc theo benh nhan") @RequestParam(name = "patient_id", required = false) Long patientId,
            @Parameter(description = "Loc theo ma phieu") @RequestParam(name = "order_code", required = false) String orderCode,
            @Parameter(description = "Loc theo ma LSVV") @RequestParam(name = "visit_code", required = false) String visitCode,
            @Parameter(description = "So trang, bat dau tu 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "So dong moi trang") @RequestParam(name = "page_size", required = false) Integer pageSize,
            @Parameter(description = "<field>,<asc|desc>, vi du ordered_at,desc") @RequestParam(required = false) String sort
    ) {
        return imagingOrderService.search(patientId, orderCode, visitCode, page, pageSize, sort);
    }

    @GetMapping(path = "/imaging-orders/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet chi dinh", description = "Kem danh sach cac ca chup.")
    @ApiResponse(responseCode = "200", description = "Chi dinh kem cac ca chup")
    @PreAuthorize(READ_ROLES)
    public ImagingOrderResponse getImagingOrder(@PathVariable Long orderId) {
        return imagingOrderService.getById(orderId);
    }

    @PostMapping(path = "/imaging-orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tao chi dinh (tao tay)", description = "source = MANUAL, kem it nhat mot study SCHEDULED, accession_number sinh tu dong.")
    @ApiResponse(responseCode = "201", description = "Da tao")
    @PreAuthorize(WRITE_ROLES)
    public ImagingOrderResponse createImagingOrder(@Valid @RequestBody ImagingOrderCreateRequest request) {
        return imagingOrderService.create(request);
    }

    @PatchMapping(path = "/imaging-orders/{orderId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sua chi dinh", description = "Cap nhat tung phan. Khong doi duoc patient_id/order_code.")
    @ApiResponse(responseCode = "200", description = "Da cap nhat")
    @PreAuthorize(WRITE_ROLES)
    public ImagingOrderResponse updateImagingOrder(@PathVariable Long orderId, @Valid @RequestBody ImagingOrderUpdateRequest request) {
        return imagingOrderService.update(orderId, request);
    }

    @PostMapping(path = "/imaging-orders/{orderId}/studies", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Them ca chup vao chi dinh", description = "Mot chi dinh co the co nhieu ca chup (IN-02). Ca moi o trang thai SCHEDULED.")
    @ApiResponse(responseCode = "201", description = "Da tao ca chup")
    @PreAuthorize(WRITE_ROLES)
    public StudyResponse addStudyToOrder(@PathVariable Long orderId, @Valid @RequestBody StudyCreateRequest request) {
        return studyService.addToOrder(orderId, request);
    }
}
