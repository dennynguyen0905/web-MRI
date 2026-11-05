package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.model.Patient;
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
public class PatientDaoImpl extends AbstractHibernateDao<Patient, Long> implements PatientDao {

    public PatientDaoImpl(EntityManager entityManager) {
        super(entityManager, Patient.class);
    }

    @Override
    public Optional<Patient> findByPatientCode(String patientCode) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Patient> query = cb.createQuery(Patient.class);
        Root<Patient> root = query.from(Patient.class);
        query.select(root).where(cb.equal(root.get("patientCode"), patientCode));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    @Override
    public Optional<Patient> findBySocialInsuranceNo(String socialInsuranceNo) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Patient> query = cb.createQuery(Patient.class);
        Root<Patient> root = query.from(Patient.class);
        query.select(root).where(cb.equal(root.get("socialInsuranceNo"), socialInsuranceNo));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<Patient> query = cb.createQuery(Patient.class);
        Root<Patient> root = query.from(Patient.class);
        query.select(root)
                .where(predicates(cb, root, criteria))
                .orderBy(order(cb, root, criteria));
        List<Patient> items = entityManager.createQuery(query)
                .setFirstResult((criteria.page() - 1) * criteria.pageSize())
                .setMaxResults(criteria.pageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Patient> countRoot = countQuery.from(Patient.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, criteria));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new SearchResult(items, total);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Patient> root, SearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.patientCode() != null && !criteria.patientCode().isBlank()) {
            predicates.add(cb.equal(root.get("patientCode"), criteria.patientCode()));
        }
        if (criteria.fullName() != null && !criteria.fullName().isBlank()) {
            predicates.add(cb.like(root.get("fullName"), "%" + criteria.fullName() + "%"));
        }
        if (criteria.q() != null && !criteria.q().isBlank()) {
            String like = "%" + criteria.q() + "%";
            predicates.add(cb.or(
                    cb.like(root.get("patientCode"), like),
                    cb.like(root.get("fullName"), like),
                    cb.like(cb.coalesce(root.get("socialInsuranceNo"), ""), like)
            ));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private jakarta.persistence.criteria.Order order(CriteriaBuilder cb, Root<Patient> root, SearchCriteria criteria) {
        Path<Object> sortPath = root.get(criteria.sortField());
        return criteria.ascending() ? cb.asc(sortPath) : cb.desc(sortPath);
    }
}
