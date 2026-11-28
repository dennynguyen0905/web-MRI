package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.DepartmentType;
/**
 * Khop dung schema "Department" trong doc/api/openapi.yaml (khong co is_active).
 * JSON dung snake_case qua spring.jackson.property-naming-strategy=SNAKE_CASE.
 * Khong co factory method tu model.Department o day - mapper.DepartmentMapper lam viec do
 * (R-05: dto khong import model).
 */
public record DepartmentResponse(
        Long id,
        String code,
        String name,
        Long parentId,
        DepartmentType type,
        Integer sortOrder
) {
}
