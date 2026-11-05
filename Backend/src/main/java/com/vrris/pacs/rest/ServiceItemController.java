package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.ServiceItemResponse;
import com.vrris.pacs.service.ServiceItemService;
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

/** GET /service-items -> danh muc dich vu CDHA, loc theo modality. */
@RestController
@Tag(name = "reference-data", description = "Khoa, may, danh muc dich vu")
public class ServiceItemController {

    private static final String ALLOWED_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping(path = "/service-items", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Danh muc dich vu CDHA")
    @ApiResponse(responseCode = "200", description = "Danh sach dich vu")
    @PreAuthorize(ALLOWED_ROLES)
    public List<ServiceItemResponse> listServiceItems(
            @Parameter(description = "Loc theo modality") @RequestParam(required = false) String modality
    ) {
        return serviceItemService.listServiceItems(modality);
    }
}
