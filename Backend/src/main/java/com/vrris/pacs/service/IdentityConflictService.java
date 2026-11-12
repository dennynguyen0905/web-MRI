package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.ConflictResolutionRequest;
import com.vrris.pacs.dto.response.IdentityConflictPageResponse;
import com.vrris.pacs.dto.response.IdentityConflictResponse;
import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.ConflictSource;

import java.time.LocalDateTime;

public interface IdentityConflictService {

    /**
     * resolution/source/fieldName/patientId/detectedFrom/detectedTo: null = khong loc theo dieu
     * kien do (GET /identity-conflicts). page/pageSize/sort: null = dung mac dinh (xem
     * service.impl.IdentityConflictServiceImpl).
     */
    IdentityConflictPageResponse search(ConflictResolution resolution, ConflictSource source, String fieldName,
                                         Long patientId, LocalDateTime detectedFrom, LocalDateTime detectedTo,
                                         Integer page, Integer pageSize, String sort);

    /** Khong tim thay -> ApiException (IDENTITY_CONFLICT_NOT_FOUND, 404). */
    IdentityConflictResponse getById(Long id);

    /**
     * Xu ly mot conflict (KEEP_RIS/TAKE_HIS/MANUAL_EDIT, doc/04-his-ris-reconciliation.md muc 2).
     * Conflict khong o trang thai PENDING -> ApiException (IDENTITY_CONFLICT_ALREADY_RESOLVED, 409).
     * resolution = PENDING hoac MANUAL_EDIT thieu manual_value -> ApiException (VALIDATION_ERROR, 400).
     * TAKE_HIS/MANUAL_EDIT ma conflict chua gan patient_id, truong khong duoc ho tro ap dung tu dong,
     * hoac gia tri khong hop le voi truong do -> ApiException (422, xem service.impl).
     */
    IdentityConflictResponse resolve(Long id, ConflictResolutionRequest request);
}
