package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.model.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AppUserDaoImpl extends AbstractHibernateDao<AppUser, Long> implements AppUserDao {

    public AppUserDaoImpl(EntityManager entityManager) {
        super(entityManager, AppUser.class);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<AppUser> query = cb.createQuery(AppUser.class);
        Root<AppUser> root = query.from(AppUser.class);
        query.select(root).where(cb.equal(root.get("username"), username));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }
}
