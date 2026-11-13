package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.RoleDao;
import com.vrris.pacs.model.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoleDaoImpl extends AbstractHibernateDao<Role, Long> implements RoleDao {

    public RoleDaoImpl(EntityManager entityManager) {
        super(entityManager, Role.class);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Role> query = cb.createQuery(Role.class);
        Root<Role> root = query.from(Role.class);
        query.select(root).where(cb.equal(root.get("code"), code));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }
}
