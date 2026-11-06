package com.vrris.pacs.dao;

import com.vrris.pacs.model.ImagingOrder;

import java.util.List;
import java.util.Optional;

public interface ImagingOrderDao extends BaseDao<ImagingOrder, Long> {

    Optional<ImagingOrder> findByOrderCode(String orderCode);

    /** Dung de sinh order_code tiep theo trong ngay (rule.OrderCodeGenerator). */
    long countByOrderCodeStartingWith(String prefix);

    SearchResult search(SearchCriteria criteria);

    /**
     * patientId/orderCode/visitCode: null = khong loc theo dieu kien do. Khop tham so
     * patient_id/order_code/visit_code cua GET /imaging-orders (doc/api/openapi.yaml).
     * sortField: ten field CUA ENTITY (vi du "orderedAt") - service da validate/doi ten truoc khi
     * goi xuong day, giong cach dao.PatientDao.SearchCriteria dang lam.
     */
    record SearchCriteria(Long patientId, String orderCode, String visitCode,
                           int page, int pageSize, String sortField, boolean ascending) {
    }

    record SearchResult(List<ImagingOrder> items, long total) {
    }
}
