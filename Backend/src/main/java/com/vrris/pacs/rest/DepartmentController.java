package com.vrris.pacs.rest;

import com.vrris.pacs.dto.response.DepartmentResponse;
import com.vrris.pacs.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.vrris.pacs.rest.RestSupport.READ_ROLES;

@RestController
@RequestMapping("/departments")
@Tag(name = "reference-data", description = "Khoa, may, danh muc dich vu")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cay khoa -> nhom")
    @ApiResponse(responseCode = "200", description = "Danh sach khoa va nhom")
    @PreAuthorize(READ_ROLES)
    public List<DepartmentResponse> listDepartments() {
        return departmentService.listDepartments();
    }
}
