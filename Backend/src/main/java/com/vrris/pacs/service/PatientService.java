package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.ListExportEventRequest;
import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.PatientPageResponse;
import com.vrris.pacs.dto.response.PatientResponse;

public interface PatientService {

    /**
     * q/patientCode/fullName: tham so loc, null = khong loc theo dieu kien do (GET /patients).
     * page/pageSize/sort: null = dung mac dinh (xem service.impl.PatientServiceImpl).
     * sort khong dung dinh dang "<field>,<asc|desc>" hoac field khong nam trong danh sach cho phep
     * -> nem ApiException (VALIDATION_ERROR, 400), giong cach DeviceServiceImpl xu ly modality sai.
     */
    PatientPageResponse search(String q, String patientCode, String fullName,
                                Integer page, Integer pageSize, String sort);

    /** Khong tim thay -> ApiException (PATIENT_NOT_FOUND, 404). */
    PatientResponse getById(Long id);

    /**
     * Trung patient_code -> ApiException (PATIENT_DUPLICATE_CODE, 409, IN-01).
     * BHXH da gan benh nhan khac -> ApiException (PATIENT_SUSPECTED_DUPLICATE, 409,
     * doc/04-his-ris-reconciliation.md muc 1.1 truong hop C).
     */
    PatientResponse create(PatientCreateRequest request);

    /**
     * Khong tim thay -> ApiException (PATIENT_NOT_FOUND, 404).
     * Doi social_insurance_no sang gia tri da gan benh nhan khac -> ApiException
     * (PATIENT_SUSPECTED_DUPLICATE, 409) - ap dung lai cung quy tac nhu create.
     */
    PatientResponse update(Long id, PatientUpdateRequest request);

    /**
     * Xuat danh sach benh nhan ra file CSV UTF-8 kem BOM (IN-25, GET /patients/export).
     * Ghi audit_log action EXPORT (patient_id co neu ket qua chi co 1 benh nhan, NULL neu nhieu -
     * doc/09-bao-mat-toan-ven-du-lieu.md muc 2.2).
     */
    ExportDocumentResponse exportCsv(String q, String patientCode, String fullName, String sort);

    /**
     * Ghi nhan mot lan xuat hoac in danh sach tu trinh duyet (IN-25, POST /patients/export-events).
     */
    void recordExportEvent(ListExportEventRequest request);
}
