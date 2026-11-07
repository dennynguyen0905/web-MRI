package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.ClinicalSnapshotDao;
import com.vrris.pacs.model.ClinicalSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ClinicalSnapshotDaoImpl extends AbstractHibernateDao<ClinicalSnapshot, Long>
        implements ClinicalSnapshotDao {

    public ClinicalSnapshotDaoImpl(EntityManager entityManager) {
        super(entityManager, ClinicalSnapshot.class);
    }

    @Override
    public Optional<ClinicalSnapshot> findByOrderId(Long orderId) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<ClinicalSnapshot> query = cb.createQuery(ClinicalSnapshot.class);
        Root<ClinicalSnapshot> root = query.from(ClinicalSnapshot.class);
        query.select(root).where(cb.equal(root.get("orderId"), orderId));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }
}
