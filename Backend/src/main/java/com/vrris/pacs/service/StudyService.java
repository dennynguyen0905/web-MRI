package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.StatusCountsResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;

import java.time.LocalDate;
import java.util.List;

public interface StudyService {

    /**
     * Worklist IN-07 (GET /studies, ban rut gon - response van la StudyResponse, chua ghep
     * patient/order/service_item/device/report nhu WorklistRow day du cua doc/api/openapi.yaml,
     * xem Javadoc StudyController). Moi tham so null/rong (hoac danh sach rong) = khong loc theo
     * dieu kien do.
     *
     * tab: "PENDING" (chi SCHEDULED) | "DONE" (cac trang thai con lai) | null (ca hai, dung cho
     * "cac ca khac cua cung benh nhan"). Ket hop AND voi status neu ca hai cung duoc truyen.
     * timeField: "SCHEDULED" | "ORDERED" | "COMPLETED" (mac dinh SCHEDULED khi rong), quyet dinh
     * cot nao dung voi dateFrom/dateTo (ca hai gom luon ngay bien, theo gio dia phuong).
     * departmentId: id khoa/nhom, tu dong gom nhanh con (rule.DepartmentTreeExpander).
     * Gia tri enum sai (tab/status/modality/patientClass/timeField) -> ApiException
     * (VALIDATION_ERROR, 400), giong cach da lam voi status o ban rut gon truoc day.
     */
    StudyPageResponse search(String tab, List<String> status, List<Long> departmentId, List<Long> deviceId,
                              String modality, String patientClass, String timeField, LocalDate dateFrom,
                              LocalDate dateTo, String patientCode, String fullName, String visitCode,
                              String orderCode, Integer page, Integer pageSize, String sort);

    /**
     * IN-09 (GET /studies/status-counts): dem so ca theo 7 nhom trang thai cua enu.StudyStatus, ap
     * dung CUNG bo dieu kien loc voi search() TRU tab/status/page/pageSize/sort (khop doc/api/openapi.yaml
     * operationId getStudyStatusCounts: "ap dung cung bo filter voi GET /studies, tru status va
     * tab"). Tham so con lai giong het search() va duoc chuan hoa theo cach giong nhau (department
     * gom nhanh con, modality/patientClass/timeField validate theo enum). Trang thai khong co ca
     * nao van tra 0 (khong bi thieu key trong by_status).
     */
    StatusCountsResponse getStatusCounts(List<Long> departmentId, List<Long> deviceId, String modality,
                                          String patientClass, String timeField, LocalDate dateFrom,
                                          LocalDate dateTo, String patientCode, String fullName,
                                          String visitCode, String orderCode);

    /** Khong tim thay -> ApiException (STUDY_NOT_FOUND, 404). */
    StudyResponse getById(Long id);

    /**
     * Sinh deep-link xem anh OHIF (IN-19, IN-23).
     * Khong tim thay -> ApiException (STUDY_NOT_FOUND, 404).
     * study_instance_uid null -> ApiException (STUDY_NO_DICOM_IMAGE, 422).
     */
    ViewerLinkResponse getViewerLink(Long id);

    /** Dung noi bo de ghep ImagingOrderResponse.studies - khong tu ghi audit rieng (xem Javadoc impl). */
    List<StudyResponse> listByOrderId(Long orderId);

    /**
     * Them mot ca chup vao chi dinh da co (IN-02, operationId addStudyToOrder). Trang thai luon
     * SCHEDULED, accession_number sinh tu dong (rule.AccessionNumberGenerator).
     * Khong tim thay chi dinh -> ApiException (IMAGING_ORDER_NOT_FOUND, 404).
     * service_item_id/device_id khong ton tai -> ApiException (STUDY_SERVICE_ITEM_NOT_FOUND /
     * STUDY_DEVICE_NOT_FOUND, 422).
     */
    StudyResponse addToOrder(Long orderId, StudyCreateRequest request);
}
