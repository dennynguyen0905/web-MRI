package com.vrris.pacs.dto.response;

/**
 * File danh sach xuat ra (CSV UTF-8 kem BOM) cho chuc nang "Tai danh sach" (IN-25).
 *
 * patientId: co gia tri neu danh sach xuat chi thuoc DUNG MOT benh nhan, null neu 0 hoac nhieu
 * benh nhan - khop quy tac ghi audit_log EXPORT o doc/09-bao-mat-toan-ven-du-lieu.md muc 2.2.
 */
public record ExportDocumentResponse(
        String fileName,
        String contentType,
        byte[] content,
        int rowCount,
        Long patientId
) {
}

