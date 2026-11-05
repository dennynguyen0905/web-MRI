package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.DepartmentResponse;
import com.vrris.pacs.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /departments -> cay khoa -> nhom (IN-12: "Cay may: khoa -> nhom -> may").
 * openapi.yaml ghi x-allowed-roles cho endpoint nay la ca 5 vai tro (khong public) -> @PreAuthorize
 * yeu cau dang nhap VA co it nhat 1 trong 5 vai tro (IN-13/IN-14).
 */
@RestController
@Tag(name = "reference-data", description = "Khoa, may, danh muc dich vu")
public class DepartmentController {

    private static final String ALLOWED_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping(path = "/departments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cay khoa -> nhom", description = "Danh sach khoa va nhom de dung cay loc khoa -> nhom -> may.")
    @ApiResponse(responseCode = "200", description = "Danh sach khoa va nhom")
    @PreAuthorize(ALLOWED_ROLES)
    public List<DepartmentResponse> listDepartments() {
        return departmentService.listDepartments();
    }
}
