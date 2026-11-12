package com.vrris.pacs.rule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Mo rong danh sach department_id duoc loc theo cay khoa -> nhom thanh "gom ca nhanh con"
 * (GET /studies?department_id=..., doc/api/openapi.yaml). Nhan vao cap (id, parent_id) thuan -
 * khong phu thuoc model.Department (ArchUnit L5: rule/ chi duoc phu thuoc enu/ va JDK) - service
 * tu chuyen doi List<Department> sang Node truoc khi goi.
 */
public final class DepartmentTreeExpander {

    private DepartmentTreeExpander() {
    }

    public record Node(Long id, Long parentId) {
    }

    /** Diem dung: lap cho den khi khong them duoc id nao (cay co the sau hon 2 cap). */
    public static Set<Long> expand(Collection<Node> allNodes, Collection<Long> requestedIds) {
        Set<Long> result = new HashSet<>(requestedIds);
        boolean added = true;
        while (added) {
            added = false;
            for (Node node : allNodes) {
                if (node.parentId() != null && result.contains(node.parentId()) && !result.contains(node.id())) {
                    result.add(node.id());
                    added = true;
                }
            }
        }
        return result;
    }
}
