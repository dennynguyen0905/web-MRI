package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.ImagingOrderCreateRequest;
import com.vrris.pacs.dto.request.ImagingOrderUpdateRequest;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.ClinicalSnapshotResponse;
import com.vrris.pacs.dto.response.ImagingOrderPageResponse;
import com.vrris.pacs.dto.response.ImagingOrderResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.service.ImagingOrderService;
import com.vrris.pacs.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.vrris.pacs.rest.RestSupport.READ_ROLES;
import static com.vrris.pacs.rest.RestSupport.WRITE_ORDER_ROLES;

@RestController
@RequestMapping("/imaging-orders")
@Tag(name = "imaging-orders", description = "Quan ly chi dinh")
public class ImagingOrderController {

    private final ImagingOrderService imagingOrderService;
    private final StudyService studyService;

    public ImagingOrderController(ImagingOrderService imagingOrderService, StudyService studyService) {
        this.imagingOrderService = imagingOrderService;
        this.studyService = studyService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach chi dinh")
    @ApiResponse(responseCode = "200", description = "Danh sach chi dinh")
    @PreAuthorize(READ_ROLES)
    public ImagingOrderPageResponse listImagingOrders(
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "order_code", required = false) String orderCode,
            @RequestParam(name = "visit_code", required = false) String visitCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String sort
    ) {
        return imagingOrderService.search(patientId, orderCode, visitCode, page, pageSize, sort);
    }

    @GetMapping(path = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chi tiet chi dinh")
    @ApiResponse(responseCode = "200", description = "Chi dinh kem cac ca chup")
    @PreAuthorize(READ_ROLES)
    public ImagingOrderResponse getImagingOrder(@PathVariable Long orderId) {
        return imagingOrderService.getById(orderId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tao chi dinh (tao tay)")
    @ApiResponse(responseCode = "201", description = "Da tao")
    @PreAuthorize(WRITE_ORDER_ROLES)
    public ImagingOrderResponse createImagingOrder(@Valid @RequestBody ImagingOrderCreateRequest request) {
        return imagingOrderService.create(request);
    }

    @PatchMapping(path = "/{orderId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sua chi dinh")
    @ApiResponse(responseCode = "200", description = "Da cap nhat")
    @PreAuthorize(WRITE_ORDER_ROLES)
    public ImagingOrderResponse updateImagingOrder(@PathVariable Long orderId, @Valid @RequestBody ImagingOrderUpdateRequest request) {
        return imagingOrderService.update(orderId, request);
    }

    @GetMapping(path = "/{orderId}/clinical-snapshot", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Thong tin lam sang cua chi dinh (JSON)")
    @ApiResponse(responseCode = "200", description = "Snapshot lam sang")
    @PreAuthorize(READ_ROLES)
    public ClinicalSnapshotResponse getClinicalSnapshot(@PathVariable Long orderId) {
        return imagingOrderService.getClinicalSnapshot(orderId);
    }

    @PostMapping(path = "/{orderId}/studies", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Them ca chup vao chi dinh")
    @ApiResponse(responseCode = "201", description = "Da tao ca chup")
    @PreAuthorize(WRITE_ORDER_ROLES)
    public StudyResponse addStudyToOrder(@PathVariable Long orderId, @Valid @RequestBody StudyCreateRequest request) {
        return studyService.addToOrder(orderId, request);
    }
}
