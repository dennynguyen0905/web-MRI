package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.model.Report;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ReportDaoImpl extends AbstractHibernateDao<Report, Long> implements ReportDao {

    public ReportDaoImpl(EntityManager entityManager) {
        super(entityManager, Report.class);
    }

    @Override
    public Optional<Report> findCurrentByStudyId(Long studyId) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Report> query = cb.createQuery(Report.class);
        Root<Report> root = query.from(Report.class);
        query.select(root)
                .where(cb.equal(root.get("studyId"), studyId))
                .orderBy(cb.desc(root.get("version")));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    @Override
    public List<Report> findAllByStudyId(Long studyId) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Report> query = cb.createQuery(Report.class);
        Root<Report> root = query.from(Report.class);
        query.select(root)
                .where(cb.equal(root.get("studyId"), studyId))
                .orderBy(cb.desc(root.get("version")));
        return entityManager.createQuery(query).getResultList();
    }
}
