package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.DailySequenceDao;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/**
 * MySQL: INSERT ... ON DUPLICATE KEY UPDATE seq_value = LAST_INSERT_ID(seq_value + 1) khoa dong
 * cua khoa (row lock InnoDB) den het transaction, nen hai transaction dong thoi nhan hai so khac
 * nhau. LAST_INSERT_ID() doc lai gia tri vua dat tren CUNG connection.
 */
@Repository
public class DailySequenceDaoImpl implements DailySequenceDao {

    private static final String UPSERT_SQL =
            "INSERT INTO daily_sequence (seq_key, seq_value) VALUES (?1, LAST_INSERT_ID(1)) "
                    + "ON DUPLICATE KEY UPDATE seq_value = LAST_INSERT_ID(seq_value + 1)";

    private final EntityManager entityManager;

    public DailySequenceDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int next(String sequenceKey) {
        entityManager.createNativeQuery(UPSERT_SQL)
                .setParameter(1, sequenceKey)
                .executeUpdate();
        Number value = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        return value.intValue();
    }
}
