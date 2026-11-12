package com.vrris.pacs.dao;

import com.vrris.pacs.model.Study;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface StudyDao extends BaseDao<Study, Long> {

    Optional<Study> findByAccessionNumber(String accessionNumber);

    /** Danh sach ca chup cua mot chi dinh (de ghep vao ImagingOrderResponse.studies). */
    List<Study> findByOrderId(Long orderId);

    /** Dung de sinh accession_number tiep theo trong ngay (rule.AccessionNumberGenerator). */
    long countByAccessionNumberStartingWith(String prefix);

    SearchResult search(SearchCriteria criteria);

    /**
     * IN-09 (GET /studies/status-counts): dem so ca theo tung gia tri status, ap dung CUNG bo dieu
     * kien loc voi search() TRU statuses/page/pageSize/sortField/ascending (khop mo ta openapi "ap
     * dung cung bo filter voi GET /studies, tru status va tab" - StatusCountCriteria vi vay khong co
     * cac truong do). Key cua map tra ve la gia tri status thuc co trong DB (co the rong neu khong
     * co ca nao khop) - service chiu trach nhiem dien du 0 cho cac gia tri enu.StudyStatus con thieu.
     */
    Map<String, Long> countByStatus(StatusCountCriteria criteria);

    /**
     * Worklist IN-07 (GET /studies ruc gon): mot phan kha day du cua doc/api/openapi.yaml -
     * con thieu phan ghep ImagingOrder/Patient/ServiceItem/Device/Report vao tung hang tra ve
     * (WorklistRow day du) - response hien tai van la StudyResponse (xem Javadoc StudyController),
     * CHUA lam o day, ngoai pham vi IN-07 cho lan sua nay.
     *
     * Tat ca danh sach null/rong = khong loc theo dieu kien do. statuses/deviceIds/departmentIds da
     * duoc service validate + chuan hoa (vi du StudyStatus.valueOf) truoc khi xuong day; departmentIds
     * da duoc mo rong ca nhanh con (rule.DepartmentTreeExpander) - DAO khong tu doan cay khoa.
     * sortField la ten field CUA ENTITY (vi du "scheduledAt", "patientCode") - service da doi ten
     * truoc khi goi xuong day (giong quy uoc dao.PatientDao.SearchCriteria).
     *
     * timeField la ten field entity dung de so sanh dateFrom/dateTo: "scheduledAt"/"completedAt"
     * (tren Study) hoac "orderedAt" (tren ImagingOrder) - khop 3 lua chon TimeField cua FE
     * (Frontend/src/shared/lib/types.ts: SCHEDULED/ORDERED/COMPLETED).
     *
     * Loc theo department: khop ca nhanh ordering_department_id (cua imaging_order) VA nhanh
     * device.department_id (cua may thuc hien, neu co) - khop mo ta openapi "Khoa hoac nhom trong
     * cay khoa -> nhom -> may".
     */
    record SearchCriteria(List<String> statuses, Set<Long> departmentIds, List<Long> deviceIds,
                           String modality, String patientClass, String patientCode, String fullName,
                           String visitCode, String orderCode, String timeField, LocalDateTime dateFrom,
                           LocalDateTime dateTo, int page, int pageSize, String sortField, boolean ascending) {
    }

    record SearchResult(List<Study> items, long total) {
    }

    /** Cac truong loc dung chung voi SearchCriteria, tru statuses/page/pageSize/sortField/ascending (IN-09). */
    record StatusCountCriteria(Set<Long> departmentIds, List<Long> deviceIds, String modality, String patientClass,
                                String patientCode, String fullName, String visitCode, String orderCode,
                                String timeField, LocalDateTime dateFrom, LocalDateTime dateTo) {
    }
}
