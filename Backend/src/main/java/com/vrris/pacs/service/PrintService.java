package com.vrris.pacs.service;

import com.vrris.pacs.dto.response.PrintDocumentResponse;
import com.vrris.pacs.enu.PrintSection;

import java.util.List;

/**
 * In phieu ket qua cua MOT benh nhan (Task 5).
 *
 * Luu y pham vi: in an la chuc nang phu H-05 [HOAN] o doc/01-scope.md muc 4 va nut in/tai con la
 * cau hoi ❓ o muc 9. Lat doc nay lam ban toi thieu duoc goi y ngay trong mo ta task: phan
 * FUNCTIONAL (ten dich vu + noi dung bao cao) la du lieu that cua RIS; cac phan con lai doc tu
 * clinical_snapshot hoac bao "chua co du lieu". Muon dua han vao IN thi phai ghi bang thay doi
 * pham vi o doc/01-scope.md muc 9.
 */
public interface PrintService {

    /**
     * Dung mot file PDF cho MOT phan in. Moi phan la mot file rieng - goi nhieu lan neu can nhieu phan.
     * Ghi audit_log action PRINT kem patient_id (IN-15, doc/09 muc 2.2).
     */
    PrintDocumentResponse render(Long patientId, Long studyId, PrintSection section);

    /**
     * Chi ghi audit_log action PRINT, khong tra file - dung khi nguoi dung in bang hop thoai cua
     * trinh duyet (du lieu van roi khoi he thong nen van phai ghi log).
     */
    void recordPrintEvent(Long patientId, Long studyId, List<PrintSection> sections);
}
