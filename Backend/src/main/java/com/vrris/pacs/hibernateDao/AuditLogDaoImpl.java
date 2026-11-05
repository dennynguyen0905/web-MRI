package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.model.AuditLog;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/** audit_log la append-only (xem comment trong V5__audit_and_conflict.sql) - chi dung save(). */
@Repository
public class AuditLogDaoImpl extends AbstractHibernateDao<AuditLog, Long> implements AuditLogDao {

    public AuditLogDaoImpl(EntityManager entityManager) {
        super(entityManager, AuditLog.class);
    }
}
