package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.model.Department;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Cay khoa -> nhom: cha (parent_id NULL) truoc, roi theo sort_order, id.
 * Tren MySQL, ORDER BY ... ASC dua NULL len truoc nen khoa goc (KHOA) luon o truoc nhom con (NHOM).
 */
@Repository
public class DepartmentDaoImpl extends AbstractHibernateDao<Department, Long> implements DepartmentDao {

    public DepartmentDaoImpl(EntityManager entityManager) {
        super(entityManager, Department.class);
    }

    @Override
    public List<Department> findActiveOrderedByTree() {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Department> query = cb.createQuery(Department.class);
        Root<Department> root = query.from(Department.class);
        query.select(root)
                .where(cb.isTrue(root.get("isActive")))
                .orderBy(
                        cb.asc(root.get("parentId")),
                        cb.asc(root.get("sortOrder")),
                        cb.asc(root.get("id"))
                );
        return entityManager.createQuery(query).getResultList();
    }
}
