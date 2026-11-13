package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.model.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogDaoImpl extends AbstractHibernateDao<AuditLog, Long> implements AuditLogDao {

    public AuditLogDaoImpl(EntityManager entityManager) {
        super(entityManager, AuditLog.class);
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<AuditLog> query = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        query.select(root)
                .where(predicates(cb, root, criteria))
                .orderBy(cb.desc(root.get("occurredAt")), cb.desc(root.get("id")));

        List<AuditLog> items = entityManager.createQuery(query)
                .setFirstResult((criteria.page() - 1) * criteria.pageSize())
                .setMaxResults(criteria.pageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditLog> countRoot = countQuery.from(AuditLog.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, criteria));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new SearchResult(items, total);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<AuditLog> root, SearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.patientId() != null) {
            predicates.add(cb.equal(root.get("patientId"), criteria.patientId()));
        }
        if (criteria.actorUserId() != null) {
            predicates.add(cb.equal(root.get("actorUserId"), criteria.actorUserId()));
        }
        if (criteria.action() != null && !criteria.action().isBlank()) {
            predicates.add(cb.equal(root.get("action"), criteria.action()));
        }
        if (criteria.entityType() != null && !criteria.entityType().isBlank()) {
            predicates.add(cb.equal(root.get("entityType"), criteria.entityType()));
        }
        if (criteria.entityId() != null) {
            predicates.add(cb.equal(root.get("entityId"), criteria.entityId()));
        }
        if (criteria.occurredFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.occurredFrom()));
        }
        if (criteria.occurredTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.occurredTo()));
        }
        return predicates.toArray(new Predicate[0]);
    }
}
