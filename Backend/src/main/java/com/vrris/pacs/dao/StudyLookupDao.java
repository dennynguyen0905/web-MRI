package com.vrris.pacs.dao;

import com.vrris.pacs.model.Report;

import java.util.Collection;
import java.util.List;

/**
 * Truy van theo lo phuc vu luong ca chup (xuat CSV worklist, kiem trung study_instance_uid) - tranh
 * N+1 khi ghep order/patient/service_item/device/report cho nhieu ca chup cung luc.
 */
public interface StudyLookupDao {

    /**
     * Nap nhieu entity cung loai theo id (thuoc tinh "id") trong MOT truy van. ids rong -> danh sach
     * rong, khong truy van DB.
     */
    <T> List<T> findAllByIds(Class<T> entityType, Collection<Long> ids);

    /** Bao cao hien hanh (version lon nhat) cua tung ca chup trong danh sach - mot truy van. */
    List<Report> findCurrentReportsByStudyIds(Collection<Long> studyIds);

    /** true neu da co ca chup KHAC (id khac excludeStudyId) dung study_instance_uid nay. */
    boolean existsOtherStudyWithInstanceUid(String studyInstanceUid, Long excludeStudyId);
}
