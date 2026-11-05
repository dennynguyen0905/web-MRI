package com.vrris.pacs.dao;

import java.util.List;
import java.util.Optional;

/**
 * CRUD co ban dung chung cho moi DAO tu viet (R-05 quyet dinh 2 - khong dung Spring Data JPA).
 * Trien khai duy nhat o hibernateDao.AbstractHibernateDao bang EntityManager + Criteria API.
 */
public interface BaseDao<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll();

    T save(T entity);

    void deleteById(ID id);
}
