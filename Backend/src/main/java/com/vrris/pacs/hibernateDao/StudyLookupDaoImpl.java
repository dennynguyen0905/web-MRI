package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.StudyLookupDao;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.Study;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class StudyLookupDaoImpl implements StudyLookupDao {

    private final EntityManager entityManager;

    public StudyLookupDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public <T> List<T> findAllByIds(Class<T> entityType, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);
        query.select(root).where(root.get("id").in(ids));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<Report> findCurrentReportsByStudyIds(Collection<Long> studyIds) {
        if (studyIds == null || studyIds.isEmpty()) {
            return List.of();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Report> query = cb.createQuery(Report.class);
        Root<Report> root = query.from(Report.class);

        Subquery<Integer> maxVersion = query.subquery(Integer.class);
        Root<Report> inner = maxVersion.from(Report.class);
        maxVersion.select(cb.max(inner.get("version")))
                .where(cb.equal(inner.get("studyId"), root.get("studyId")));

        query.select(root).where(
                root.get("studyId").in(studyIds),
                cb.equal(root.get("version"), maxVersion));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public boolean existsOtherStudyWithInstanceUid(String studyInstanceUid, Long excludeStudyId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Study> root = query.from(Study.class);
        query.select(cb.count(root)).where(
                cb.equal(root.get("studyInstanceUid"), studyInstanceUid),
                excludeStudyId == null ? cb.conjunction() : cb.notEqual(root.get("id"), excludeStudyId));
        return entityManager.createQuery(query).getSingleResult() > 0;
    }
}
