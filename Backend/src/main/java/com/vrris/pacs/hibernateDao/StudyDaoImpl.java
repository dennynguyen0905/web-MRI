package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class StudyDaoImpl extends AbstractHibernateDao<Study, Long> implements StudyDao {

    public StudyDaoImpl(EntityManager entityManager) {
        super(entityManager, Study.class);
    }

    @Override
    public Optional<Study> findByAccessionNumber(String accessionNumber) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Study> query = cb.createQuery(Study.class);
        Root<Study> root = query.from(Study.class);
        query.select(root).where(cb.equal(root.get("accessionNumber"), accessionNumber));
        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    @Override
    public List<Study> findByOrderId(Long orderId) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Study> query = cb.createQuery(Study.class);
        Root<Study> root = query.from(Study.class);
        query.select(root)
                .where(cb.equal(root.get("orderId"), orderId))
                .orderBy(cb.asc(root.get("scheduledAt")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public long countByAccessionNumberStartingWith(String prefix) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Study> root = query.from(Study.class);
        query.select(cb.count(root)).where(cb.like(root.get("accessionNumber"), prefix + "%"));
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Study khong co @ManyToOne toi ImagingOrder/Patient/ServiceItem (xem Javadoc model.Study) -
     * join bang nhieu Root + dieu kien bang (cross join + predicate = INNER JOIN dung, vi
     * study.order_id/service_item_id va imaging_order.patient_id la NOT NULL nen moi Study luon
     * khop dung 1 dong ImagingOrder/Patient/ServiceItem, khong lam phinh/mat ket qua).
     */
    @Override
    public SearchResult search(SearchCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<Study> query = cb.createQuery(Study.class);
        Root<Study> studyRoot = query.from(Study.class);
        Root<ImagingOrder> orderRoot = query.from(ImagingOrder.class);
        Root<Patient> patientRoot = query.from(Patient.class);
        Root<ServiceItem> serviceItemRoot = query.from(ServiceItem.class);
        query.select(studyRoot)
                .where(predicates(cb, query, studyRoot, orderRoot, patientRoot, serviceItemRoot, criteria))
                .orderBy(order(cb, studyRoot, orderRoot, patientRoot, criteria));
        List<Study> items = entityManager.createQuery(query)
                .setFirstResult((criteria.page() - 1) * criteria.pageSize())
                .setMaxResults(criteria.pageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Study> countStudyRoot = countQuery.from(Study.class);
        Root<ImagingOrder> countOrderRoot = countQuery.from(ImagingOrder.class);
        Root<Patient> countPatientRoot = countQuery.from(Patient.class);
        Root<ServiceItem> countServiceItemRoot = countQuery.from(ServiceItem.class);
        countQuery.select(cb.countDistinct(countStudyRoot))
                .where(predicates(cb, countQuery, countStudyRoot, countOrderRoot, countPatientRoot, countServiceItemRoot, criteria));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new SearchResult(items, total);
    }

    /** IN-09: group by status, cung dieu kien loc voi search() tru statuses/page/sort (xem Javadoc dao.StudyDao). */
    @Override
    public Map<String, Long> countByStatus(StatusCountCriteria criteria) {
        CriteriaBuilder cb = criteriaBuilder();

        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Study> studyRoot = query.from(Study.class);
        Root<ImagingOrder> orderRoot = query.from(ImagingOrder.class);
        Root<Patient> patientRoot = query.from(Patient.class);
        Root<ServiceItem> serviceItemRoot = query.from(ServiceItem.class);
        query.multiselect(studyRoot.get("status"), cb.count(studyRoot))
                .where(predicates(cb, query, studyRoot, orderRoot, patientRoot, serviceItemRoot, criteria))
                .groupBy(studyRoot.get("status"));

        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : entityManager.createQuery(query).getResultList()) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    private Predicate[] predicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Study> studyRoot,
                                    Root<ImagingOrder> orderRoot, Root<Patient> patientRoot,
                                    Root<ServiceItem> serviceItemRoot, SearchCriteria criteria) {
        List<Predicate> predicates = commonPredicates(cb, query, studyRoot, orderRoot, patientRoot, serviceItemRoot,
                criteria.departmentIds(), criteria.deviceIds(), criteria.modality(), criteria.patientClass(),
                criteria.patientCode(), criteria.fullName(), criteria.visitCode(), criteria.orderCode(),
                criteria.timeField(), criteria.dateFrom(), criteria.dateTo());
        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            predicates.add(studyRoot.get("status").in(criteria.statuses()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    /** IN-09: cung dieu kien loc voi predicates(SearchCriteria) nhung KHONG co dieu kien theo status. */
    private Predicate[] predicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Study> studyRoot,
                                    Root<ImagingOrder> orderRoot, Root<Patient> patientRoot,
                                    Root<ServiceItem> serviceItemRoot, StatusCountCriteria criteria) {
        List<Predicate> predicates = commonPredicates(cb, query, studyRoot, orderRoot, patientRoot, serviceItemRoot,
                criteria.departmentIds(), criteria.deviceIds(), criteria.modality(), criteria.patientClass(),
                criteria.patientCode(), criteria.fullName(), criteria.visitCode(), criteria.orderCode(),
                criteria.timeField(), criteria.dateFrom(), criteria.dateTo());
        return predicates.toArray(new Predicate[0]);
    }

    private List<Predicate> commonPredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Study> studyRoot,
                                               Root<ImagingOrder> orderRoot, Root<Patient> patientRoot,
                                               Root<ServiceItem> serviceItemRoot, Set<Long> departmentIds,
                                               List<Long> deviceIds, String modality, String patientClass,
                                               String patientCode, String fullName, String visitCode,
                                               String orderCode, String timeField, LocalDateTime dateFrom,
                                               LocalDateTime dateTo) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(studyRoot.get("orderId"), orderRoot.get("id")));
        predicates.add(cb.equal(orderRoot.get("patientId"), patientRoot.get("id")));
        predicates.add(cb.equal(studyRoot.get("serviceItemId"), serviceItemRoot.get("id")));

        if (deviceIds != null && !deviceIds.isEmpty()) {
            predicates.add(studyRoot.get("deviceId").in(deviceIds));
        }
        if (modality != null && !modality.isBlank()) {
            predicates.add(cb.equal(serviceItemRoot.get("modality"), modality));
        }
        if (patientClass != null && !patientClass.isBlank()) {
            predicates.add(cb.equal(orderRoot.get("patientClass"), patientClass));
        }
        if (patientCode != null && !patientCode.isBlank()) {
            predicates.add(containsIgnoreCase(cb, patientRoot.get("patientCode"), patientCode));
        }
        if (fullName != null && !fullName.isBlank()) {
            predicates.add(containsIgnoreCase(cb, patientRoot.get("fullName"), fullName));
        }
        if (visitCode != null && !visitCode.isBlank()) {
            predicates.add(containsIgnoreCase(cb, orderRoot.get("visitCode"), visitCode));
        }
        if (orderCode != null && !orderCode.isBlank()) {
            predicates.add(containsIgnoreCase(cb, orderRoot.get("orderCode"), orderCode));
        }
        if (departmentIds != null && !departmentIds.isEmpty()) {
            predicates.add(departmentPredicate(cb, query, studyRoot, orderRoot, departmentIds));
        }
        if (dateFrom != null || dateTo != null) {
            Path<LocalDateTime> timePath = timeFieldPath(studyRoot, orderRoot, timeField);
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(timePath, dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(timePath, dateTo));
            }
        }
        return predicates;
    }

    private Predicate containsIgnoreCase(CriteriaBuilder cb, Path<String> path, String value) {
        return cb.like(cb.lower(path), "%" + value.toLowerCase() + "%");
    }

    private Path<LocalDateTime> timeFieldPath(Root<Study> studyRoot, Root<ImagingOrder> orderRoot, String timeField) {
        if ("orderedAt".equals(timeField)) {
            return orderRoot.get("orderedAt");
        }
        return studyRoot.get(timeField); // "scheduledAt" hoac "completedAt"
    }

    /**
     * Khop department: dung nhanh ordering_department_id (imaging_order) HOAC nhanh
     * device.department_id (device thuc hien, co the NULL - dung subquery EXISTS de khong lam
     * mat cac Study chua gan may khi loc theo nhanh cua order).
     */
    private Predicate departmentPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Study> studyRoot,
                                           Root<ImagingOrder> orderRoot, Set<Long> departmentIds) {
        Subquery<Long> deviceDept = query.subquery(Long.class);
        Root<Device> deviceRoot = deviceDept.from(Device.class);
        deviceDept.select(deviceRoot.get("id"))
                .where(cb.equal(deviceRoot.get("id"), studyRoot.get("deviceId")),
                        deviceRoot.get("departmentId").in(departmentIds));
        return cb.or(cb.exists(deviceDept), orderRoot.get("orderingDepartmentId").in(departmentIds));
    }

    private jakarta.persistence.criteria.Order order(CriteriaBuilder cb, Root<Study> studyRoot,
                                                       Root<ImagingOrder> orderRoot, Root<Patient> patientRoot,
                                                       SearchCriteria criteria) {
        Path<?> sortPath = switch (criteria.sortField()) {
            case "accessionNumber" -> studyRoot.get("accessionNumber");
            case "patientCode" -> patientRoot.get("patientCode");
            case "fullName" -> patientRoot.get("fullName");
            case "orderCode" -> orderRoot.get("orderCode");
            case "orderedAt" -> orderRoot.get("orderedAt");
            case "completedAt" -> studyRoot.get("completedAt");
            case "status" -> studyRoot.get("status");
            case "createdAt" -> studyRoot.get("createdAt");
            default -> studyRoot.get("scheduledAt");
        };
        return criteria.ascending() ? cb.asc(sortPath) : cb.desc(sortPath);
    }
}
