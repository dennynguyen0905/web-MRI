package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dto.response.DepartmentResponse;
import com.vrris.pacs.mapper.DepartmentMapper;
import com.vrris.pacs.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Day la du lieu seed/danh muc, khong phai du lieu benh nhan -> khong ghi audit_log
 * (doc/02-erd.md, doc/15-kien-truc-backend.md muc 10).
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentDao departmentDao;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentDao departmentDao, DepartmentMapper departmentMapper) {
        this.departmentDao = departmentDao;
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentMapper.toResponseList(departmentDao.findActiveOrderedByTree());
    }
}
