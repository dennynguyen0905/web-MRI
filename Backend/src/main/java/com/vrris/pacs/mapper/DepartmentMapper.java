package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.DepartmentResponse;
import com.vrris.pacs.model.Department;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponse toResponse(Department department);

    List<DepartmentResponse> toResponseList(List<Department> departments);
}
