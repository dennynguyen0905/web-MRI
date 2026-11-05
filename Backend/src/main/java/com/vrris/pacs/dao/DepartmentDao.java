package com.vrris.pacs.dao;

import com.vrris.pacs.model.Department;

import java.util.List;

public interface DepartmentDao extends BaseDao<Department, Long> {

    /**
     * Cay khoa -> nhom: cha (parent_id NULL) truoc, roi theo sort_order, id.
     * Tren MySQL, ORDER BY ... ASC dua NULL len truoc nen khoa goc (KHOA) luon o truoc nhom con (NHOM).
     */
    List<Department> findActiveOrderedByTree();
}
