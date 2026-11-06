package com.vrris.pacs.dao;

import com.vrris.pacs.model.Study;

import java.util.List;
import java.util.Optional;

public interface StudyDao extends BaseDao<Study, Long> {

    Optional<Study> findByAccessionNumber(String accessionNumber);

    /** Danh sach ca chup cua mot chi dinh (de ghep vao ImagingOrderResponse.studies). */
    List<Study> findByOrderId(Long orderId);

    /** Dung de sinh accession_number tiep theo trong ngay (rule.AccessionNumberGenerator). */
    long countByAccessionNumberStartingWith(String prefix);

    SearchResult search(SearchCriteria criteria);

    /**
     * orderId/status/deviceId: null = khong loc theo dieu kien do.
     *
     * Day la mot phan rut gon cua GET /studies (worklist) trong doc/api/openapi.yaml: ban day du
     * con loc theo department_id (ca cay khoa->nhom), modality, patient_class, patient_code,
     * full_name, visit_code, order_code, khoang thoi gian (time_field/date_from/date_to) va tab
     * PENDING/DONE - thuoc IN-07 (worklist), KHONG lam trong pham vi lat doc nay (chi can dao+mot
     * luong rest->service->dao->model cho entity Study).
     */
    record SearchCriteria(Long orderId, String status, Long deviceId,
                           int page, int pageSize, String sortField, boolean ascending) {
    }

    record SearchResult(List<Study> items, long total) {
    }
}
