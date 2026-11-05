package com.vrris.pacs.service;

import com.vrris.pacs.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> listDepartments();
}
