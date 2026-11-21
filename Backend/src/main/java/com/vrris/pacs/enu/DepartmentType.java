package com.vrris.pacs.enu;

/** Khop cot "department.type" (database/migrations/V1__ref_data.sql: 'KHOA|NHOM'). */
public enum DepartmentType {
    KHOA,  // 0 - khoa
    NHOM   // 1 - nhom/phong thuoc mot khoa (department.parent_id tro len khoa)
}
