package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.StudyStatusHistoryDao;
import com.vrris.pacs.model.StudyStatusHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudyStatusHistoryDaoImpl extends AbstractHibernateDao<StudyStatusHistory, Long> implements StudyStatusHistoryDao {

    public StudyStatusHistoryDaoImpl(EntityManager entityManager) {
        super(entityManager, StudyStatusHistory.class);
    }

    @Override
    public List<StudyStatusHistory> findByStudyIdOrderByChangedAtAsc(Long studyId) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<StudyStatusHistory> query = cb.createQuery(StudyStatusHistory.class);
        Root<StudyStatusHistory> root = query.from(StudyStatusHistory.class);
        query.select(root)
                .where(cb.equal(root.get("studyId"), studyId))
                .orderBy(cb.asc(root.get("changedAt")), cb.asc(root.get("id")));
        return entityManager.createQuery(query).getResultList();
    }
}
