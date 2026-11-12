package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.IdentityConflictDao;
import com.vrris.pacs.model.IdentityConflict;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class IdentityConflictDaoImpl extends AbstractHibernateDao<IdentityConflict, Long> implements IdentityConflictDao {

    public IdentityConflictDaoImpl(EntityManager entityManager) {
        super(entityManager, IdentityConflict.class);
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<IdentityConflict> query = cb.createQuery(IdentityConflict.class);
        Root<IdentityConflict> root = query.from(IdentityConflict.class);
        query.select(root)
                .where(predicates(cb, root, criteria))
                .orderBy(order(cb, root, criteria));
        List<IdentityConflict> items = entityManager.createQuery(query)
                .setFirstResult((criteria.page() - 1) * criteria.pageSize())
                .setMaxResults(criteria.pageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<IdentityConflict> countRoot = countQuery.from(IdentityConflict.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, criteria));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new SearchResult(items, total);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<IdentityConflict> root, SearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.resolution() != null) {
            predicates.add(cb.equal(root.get("resolution"), criteria.resolution()));
        }
        if (criteria.source() != null) {
            predicates.add(cb.equal(root.get("source"), criteria.source()));
        }
        if (criteria.fieldName() != null && !criteria.fieldName().isBlank()) {
            predicates.add(cb.equal(root.get("fieldName"), criteria.fieldName()));
        }
        if (criteria.patientId() != null) {
            predicates.add(cb.equal(root.get("patientId"), criteria.patientId()));
        }
        if (criteria.detectedFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("detectedAt"), criteria.detectedFrom()));
        }
        if (criteria.detectedTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("detectedAt"), criteria.detectedTo()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private jakarta.persistence.criteria.Order order(CriteriaBuilder cb, Root<IdentityConflict> root, SearchCriteria criteria) {
        Path<Object> sortPath = root.get(criteria.sortField());
        return criteria.ascending() ? cb.asc(sortPath) : cb.desc(sortPath);
    }
}
