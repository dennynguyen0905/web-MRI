package com.vrris.pacs.service;

import com.vrris.pacs.dto.response.ExportDocumentResponse;

/** Xuat danh sach ca chup tren worklist ra CSV (IN-25, GET /studies/export). */
public interface StudyExportService {

    /**
     * File CSV UTF-8 kem BOM. Ghi audit_log action EXPORT (patient_id co neu danh sach chi thuoc
     * 1 benh nhan, NULL neu nhieu - doc/09-bao-mat-toan-ven-du-lieu.md muc 2.2).
     * Bo loc khop qua nhieu dong (vuot gioi han xuat) -> ApiException (EXPORT_TOO_MANY_ROWS, 422)
     * thay vi cat bot im lang.
     */
    ExportDocumentResponse exportCsv(Long orderId, String status, Long deviceId,
                                     String q, String patientCode, String fullName, String sort);
}
