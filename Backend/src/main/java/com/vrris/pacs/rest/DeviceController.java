package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.DeviceResponse;
import com.vrris.pacs.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.vrris.pacs.rest.RestSupport.READ_ROLES;

@RestController
@RequestMapping("/devices")
@Tag(name = "reference-data", description = "Khoa, may, danh muc dich vu")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh sach may chup")
    @ApiResponse(responseCode = "200", description = "Danh sach may")
    @PreAuthorize(READ_ROLES)
    public List<DeviceResponse> listDevices(
            @RequestParam(name = "department_id", required = false) Long departmentId,
            @RequestParam(required = false) String modality
    ) {
        return deviceService.listDevices(departmentId, modality);
    }
}
