package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.model.Study;
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
public class StudyDaoImpl extends AbstractHibernateDao<Study, Long> implements StudyDao {

    public StudyDaoImpl(EntityManager entityManager) {
        super(entityManager, Study.class);
    }

    @Override
    public Optional<Study> findByAccessionNumber(String accessionNumber) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Study> query = cb.createQuery(Study.class);
        Root<Study> root = query.from(Study.class);
        query.select(root).where(cb.equal(root.get("accessionNumber"), accessionNumber));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    @Override
    public List<Study> findByOrderId(Long orderId) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Study> query = cb.createQuery(Study.class);
        Root<Study> root = query.from(Study.class);
        query.select(root)
                .where(cb.equal(root.get("orderId"), orderId))
                .orderBy(cb.asc(root.get("scheduledAt")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public long countByAccessionNumberStartingWith(String prefix) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Study> root = query.from(Study.class);
        query.select(cb.count(root)).where(cb.like(root.get("accessionNumber"), prefix + "%"));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<Study> query = cb.createQuery(Study.class);
        Root<Study> root = query.from(Study.class);
        query.select(root)
                .where(predicates(cb, root, criteria))
                .orderBy(order(cb, root, criteria));
        List<Study> items = entityManager.createQuery(query)
                .setFirstResult((criteria.page() - 1) * criteria.pageSize())
                .setMaxResults(criteria.pageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Study> countRoot = countQuery.from(Study.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, criteria));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new SearchResult(items, total);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Study> root, SearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.orderId() != null) {
            predicates.add(cb.equal(root.get("orderId"), criteria.orderId()));
        }
        if (criteria.status() != null && !criteria.status().isBlank()) {
            predicates.add(cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.deviceId() != null) {
            predicates.add(cb.equal(root.get("deviceId"), criteria.deviceId()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private jakarta.persistence.criteria.Order order(CriteriaBuilder cb, Root<Study> root, SearchCriteria criteria) {
        Path<Object> sortPath = root.get(criteria.sortField());
        return criteria.ascending() ? cb.asc(sortPath) : cb.desc(sortPath);
    }
}
