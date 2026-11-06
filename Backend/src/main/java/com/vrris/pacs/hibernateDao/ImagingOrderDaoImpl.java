package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.model.ImagingOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ImagingOrderDaoImpl extends AbstractHibernateDao<ImagingOrder, Long> implements ImagingOrderDao {

    public ImagingOrderDaoImpl(EntityManager entityManager) {
        super(entityManager, ImagingOrder.class);
    }

    @Override
    public Optional<ImagingOrder> findByOrderCode(String orderCode) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<ImagingOrder> query = cb.createQuery(ImagingOrder.class);
        Root<ImagingOrder> root = query.from(ImagingOrder.class);
        query.select(root).where(cb.equal(root.get("orderCode"), orderCode));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    @Override
    public long countByOrderCodeStartingWith(String prefix) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ImagingOrder> root = query.from(ImagingOrder.class);
        query.select(cb.count(root)).where(cb.like(root.get("orderCode"), prefix + "%"));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<ImagingOrder> query = cb.createQuery(ImagingOrder.class);
        Root<ImagingOrder> root = query.from(ImagingOrder.class);
        query.select(root)
                .where(predicates(cb, root, criteria))
                .orderBy(order(cb, root, criteria));
        List<ImagingOrder> items = entityManager.createQuery(query)
                .setFirstResult((criteria.page() - 1) * criteria.pageSize())
                .setMaxResults(criteria.pageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ImagingOrder> countRoot = countQuery.from(ImagingOrder.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, criteria));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new SearchResult(items, total);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<ImagingOrder> root, SearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.patientId() != null) {
            predicates.add(cb.equal(root.get("patientId"), criteria.patientId()));
        }
        if (criteria.orderCode() != null && !criteria.orderCode().isBlank()) {
            predicates.add(cb.equal(root.get("orderCode"), criteria.orderCode()));
        }
        if (criteria.visitCode() != null && !criteria.visitCode().isBlank()) {
            predicates.add(cb.equal(root.get("visitCode"), criteria.visitCode()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private jakarta.persistence.criteria.Order order(CriteriaBuilder cb, Root<ImagingOrder> root, SearchCriteria criteria) {
        Path<Object> sortPath = root.get(criteria.sortField());
        return criteria.ascending() ? cb.asc(sortPath) : cb.desc(sortPath);
    }
}
