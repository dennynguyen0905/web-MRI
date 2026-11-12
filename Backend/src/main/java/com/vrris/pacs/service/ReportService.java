package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.ReportWriteRequest;
import com.vrris.pacs.dto.response.ReportResponse;

import java.util.List;

/**
 * IN-10: bao cao ket qua cua mot ca chup (ky thuat/mo ta/chan doan/ket luan/khuyen nghi), nhieu
 * version (UK(study_id, version), doc/02-erd.md 4.10). Ba endpoint theo doc/api/openapi.yaml:
 * listStudyReports, getCurrentReport, saveCurrentReportDraft.
 *
 * Tao version MOI (tra lai roi chot lai T8, mo lai ca da duyet T9) la viec cua chuyen trang thai
 * (rule.StudyStateMachine qua mot service rieng, chua lam - xem doc/03-state-machine.md, KAN-B9) -
 * KHONG lam o day, dung theo mo ta operationId saveCurrentReportDraft trong openapi.yaml ("Version
 * moi do backend tao khi chuyen trang thai... KHONG tao qua endpoint nay"). updateCurrent() o day
 * chi UPDATE duoc ban hien hanh dang DRAFT.
 */
public interface ReportService {

    /**
     * Moi version cua mot ca chup, version lon nhat truoc. Khong tim thay study -> ApiException
     * (STUDY_NOT_FOUND, 404).
     */
    List<ReportResponse> listVersions(Long studyId);

    /**
     * Ban hien hanh (version lon nhat). Khong tim thay study -> ApiException (STUDY_NOT_FOUND, 404);
     * study chua co bao cao nao -> ApiException (REPORT_NOT_FOUND, 404).
     */
    ReportResponse getCurrent(Long studyId);

    /**
     * Luu nhap ban hien hanh - CHI khi ca dang READING, nguoi goi la nguoi dang giu ca
     * (study.reading_user_id) va ban hien hanh dang DRAFT. Khong bat buoc du 5 truong.
     *
     * Khong tim thay study -> STUDY_NOT_FOUND (404). Study chua co bao cao -> REPORT_NOT_FOUND
     * (404). Nguoi goi khong phai nguoi dang giu ca -> REPORT_NOT_HOLDER (403, RBAC-04/05/09).
     * Ca khong o trang thai READING -> STUDY_NOT_READING (409). Ban hien hanh khong con la DRAFT
     * (da FINAL/APPROVED/RETURNED) -> REPORT_NOT_DRAFT (409).
     */
    ReportResponse updateCurrent(Long studyId, ReportWriteRequest request);
}
