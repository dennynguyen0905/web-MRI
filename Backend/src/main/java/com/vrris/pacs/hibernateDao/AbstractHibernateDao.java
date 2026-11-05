package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.BaseDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

/**
 * CRUD co ban dung EntityManager (JPA Criteria API, Hibernate la JPA provider phia duoi) - R-05
 * quyet dinh 2: DAO tu viet, khong dung interface Spring Data JPA (JpaRepository/CrudRepository).
 * Truy van dong (bo loc worklist, cay khoa...) viet them o tung XxxDaoImpl bang Criteria API.
 */
public abstract class AbstractHibernateDao<T, ID> implements BaseDao<T, ID> {

    protected final EntityManager entityManager;
    private final Class<T> entityClass;

    protected AbstractHibernateDao(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        query.select(root);
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public T save(T entity) {
        // Id sinh boi IDENTITY (xem model/*.java): entity moi (chua persist) luon la transient,
        // entityManager.contains(...) tra false -> persist; entity da lay ra tu DB roi sua ->
        // contains tra true -> merge. Du voi nhu cau CRUD don gian cua skeleton nay.
        if (entityManager.contains(entity)) {
            return entityManager.merge(entity);
        }
        entityManager.persist(entity);
        return entity;
    }

    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(entityManager::remove);
    }

    protected CriteriaBuilder criteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    protected Class<T> entityClass() {
        return entityClass;
    }
}
