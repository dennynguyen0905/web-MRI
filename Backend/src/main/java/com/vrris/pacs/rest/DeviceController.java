package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.DeviceResponse;
import com.vrris.pacs.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /devices -> danh sach may chup, loc theo department_id / modality.
 * modality nhan String tho, khong phai enu.Modality - gia tri khong hop le duoc service doi thanh
 * ApiException (VALIDATION_ERROR, 400) de rest khong phu thuoc enu (R-05: rest chi phu thuoc
 * service, dto, exception).
 */
@RestController
@Tag(name = "reference-data", description = "Khoa, may, danh muc dich vu")
public class DeviceController {

    private static final String ALLOWED_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping(path = "/devices", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach may chup")
    @ApiResponse(responseCode = "200", description = "Danh sach may")
    @PreAuthorize(ALLOWED_ROLES)
    public List<DeviceResponse> listDevices(
            @Parameter(description = "Loc theo khoa") @RequestParam(name = "department_id", required = false) Long departmentId,
            @Parameter(description = "Loc theo modality") @RequestParam(required = false) String modality
    ) {
        return deviceService.listDevices(departmentId, modality);
    }
}
