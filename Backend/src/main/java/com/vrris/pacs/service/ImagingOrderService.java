package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.ImagingOrderCreateRequest;
import com.vrris.pacs.dto.request.ImagingOrderUpdateRequest;
import com.vrris.pacs.dto.response.ClinicalSnapshotResponse;
import com.vrris.pacs.dto.response.ImagingOrderPageResponse;
import com.vrris.pacs.dto.response.ImagingOrderResponse;

public interface ImagingOrderService {

    /**
     * patientId/orderCode/visitCode: null = khong loc theo dieu kien do (GET /imaging-orders).
     * page/pageSize/sort: null = dung mac dinh (xem service.impl.ImagingOrderServiceImpl).
     */
    ImagingOrderPageResponse search(Long patientId, String orderCode, String visitCode,
                                     Integer page, Integer pageSize, String sort);

    /** Khong tim thay -> ApiException (IMAGING_ORDER_NOT_FOUND, 404). */
    ImagingOrderResponse getById(Long id);

    /**
     * Tao imaging_order (source = MANUAL) kem it nhat mot study SCHEDULED (IN-02, doc/api/openapi.yaml
     * operationId createImagingOrder). order_code sinh tu dong (rule.OrderCodeGenerator).
     * patient_id/ordering_department_id khong ton tai -> ApiException (IMAGING_ORDER_PATIENT_NOT_FOUND /
     * IMAGING_ORDER_DEPARTMENT_NOT_FOUND, 422) - khop nhom response 400/409/422 cua endpoint nay
     * trong openapi.yaml (khong co 404 cho loi FK trong body).
     */
    ImagingOrderResponse create(ImagingOrderCreateRequest request);

    /** Khong tim thay -> ApiException (IMAGING_ORDER_NOT_FOUND, 404). */
    ImagingOrderResponse update(Long id, ImagingOrderUpdateRequest request);

    /**
     * Snapshot lam sang (clinical_snapshot, IN-11, operationId getClinicalSnapshot). Chi dinh
     * khong ton tai -> ApiException (IMAGING_ORDER_NOT_FOUND, 404); chi dinh ton tai nhung chua co
     * snapshot -> ApiException (CLINICAL_SNAPSHOT_NOT_FOUND, 404). CLINICIAN goi chi dinh khong
     * phai do minh chi dinh (D34, doc/06-rbac-matrix.md muc 5) -> ApiException (FORBIDDEN, 403).
     */
    ClinicalSnapshotResponse getClinicalSnapshot(Long orderId);
}
