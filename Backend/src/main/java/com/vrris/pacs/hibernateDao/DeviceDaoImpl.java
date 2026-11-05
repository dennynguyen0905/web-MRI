package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.model.Device;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DeviceDaoImpl extends AbstractHibernateDao<Device, Long> implements DeviceDao {

    public DeviceDaoImpl(EntityManager entityManager) {
        super(entityManager, Device.class);
    }

    @Override
    public List<Device> search(Long departmentId, String modality) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Device> query = cb.createQuery(Device.class);
        Root<Device> root = query.from(Device.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(root.get("isActive")));
        if (departmentId != null) {
            predicates.add(cb.equal(root.get("departmentId"), departmentId));
        }
        if (modality != null) {
            predicates.add(cb.equal(root.get("modality"), modality));
        }

        query.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(root.get("name")));
        return entityManager.createQuery(query).getResultList();
    }
}
