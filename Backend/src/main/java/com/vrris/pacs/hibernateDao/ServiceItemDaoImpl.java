package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.model.ServiceItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ServiceItemDaoImpl extends AbstractHibernateDao<ServiceItem, Long> implements ServiceItemDao {

    public ServiceItemDaoImpl(EntityManager entityManager) {
        super(entityManager, ServiceItem.class);
    }

    @Override
    public List<ServiceItem> search(String modality) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<ServiceItem> query = cb.createQuery(ServiceItem.class);
        Root<ServiceItem> root = query.from(ServiceItem.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(root.get("isActive")));
        if (modality != null) {
            predicates.add(cb.equal(root.get("modality"), modality));
        }

        query.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(root.get("name")));
        return entityManager.createQuery(query).getResultList();
    }
}
