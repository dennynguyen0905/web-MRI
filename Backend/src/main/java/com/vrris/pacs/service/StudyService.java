package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;

import java.util.List;

public interface StudyService {

    /**
     * orderId/status/deviceId: null = khong loc theo dieu kien do (GET /studies, ban rut gon -
     * xem dao.StudyDao.SearchCriteria). status sai gia tri enu.StudyStatus -> ApiException
     * (VALIDATION_ERROR, 400), giong cach DeviceServiceImpl xu ly modality sai.
     */
    StudyPageResponse search(Long orderId, String status, Long deviceId,
                              Integer page, Integer pageSize, String sort);

    /** Khong tim thay -> ApiException (STUDY_NOT_FOUND, 404). */
    StudyResponse getById(Long id);

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

    /**
     * Xuat danh sach ca chup / benh nhan tren worklist ra file CSV UTF-8 kem BOM (IN-25,
     * GET /studies/export). Ghi audit_log action EXPORT (patient_id co neu danh sach chi thuoc
     * 1 benh nhan, NULL neu nhieu - doc/09-bao-mat-toan-ven-du-lieu.md muc 2.2).
     */
    ExportDocumentResponse exportCsv(Long orderId, String status, Long deviceId,
                                     String q, String patientCode, String fullName, String sort);
}
