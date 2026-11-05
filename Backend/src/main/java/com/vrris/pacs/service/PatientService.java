package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
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
}
