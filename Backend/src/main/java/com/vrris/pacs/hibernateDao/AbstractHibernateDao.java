package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.BaseDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * CRUD co ban dung EntityManager (JPA Criteria API, Hibernate la JPA provider phia duoi) - R-05
 * quyet dinh 2: DAO tu viet, khong dung interface Spring Data JPA (JpaRepository/CrudRepository).
 * Truy van dong (bo loc worklist, cay khoa...) viet them o tung XxxDaoImpl bang Criteria API.
 */
public abstract class AbstractHibernateDao<T, ID> implements BaseDao<T, ID> {

    /** Ky tu escape dung chung cho moi menh de LIKE tim kiem tu do (xem {@link #containsPattern}). */
    protected static final char LIKE_ESCAPE = '\\';

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

    /**
     * Id sinh boi IDENTITY (xem model/*.java): id == null -> entity moi -> persist. Id khac null ->
     * entity da co trong DB (managed hoac detached) -> merge. Truoc day dua vao
     * entityManager.contains(): entity detached co id bi persist nham va nem EntityExistsException.
     * Tra ve ban managed - noi goi phai dung gia tri tra ve, khong dung tham so truyen vao.
     */
    @Override
    public T save(T entity) {
        Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        if (id == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
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

    /**
     * Chuoi "%value%" da escape % _ \ do nguoi dung nhap - nhap "_" hay "%" phai tim dung ky tu do,
     * khong bien thanh wildcard khop moi dong. Dung kem {@link #LIKE_ESCAPE} trong cb.like(...).
     */
    protected static String containsPattern(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('%');
        for (char c : value.toCharArray()) {
            if (c == '%' || c == '_' || c == LIKE_ESCAPE) {
                sb.append(LIKE_ESCAPE);
            }
            sb.append(c);
        }
        return sb.append('%').toString();
    }

    /** LIKE '%value%' (da escape), khong phan biet hoa thuong. */
    protected Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> path, String value) {
        return cb.like(cb.lower(path), containsPattern(value.toLowerCase(Locale.ROOT)), LIKE_ESCAPE);
    }

    /**
     * Sap xep theo truong nguoi dung chon roi theo id cung chieu - cot phu duy nhat de phan trang on
     * dinh (khong lap/mat dong giua cac trang khi nhieu dong trung gia tri sap xep).
     */
    protected Order[] orderWithIdTieBreaker(CriteriaBuilder cb, Path<?> sortPath, Path<?> idPath, boolean ascending) {
        return ascending
                ? new Order[]{cb.asc(sortPath), cb.asc(idPath)}
                : new Order[]{cb.desc(sortPath), cb.desc(idPath)};
    }
}
