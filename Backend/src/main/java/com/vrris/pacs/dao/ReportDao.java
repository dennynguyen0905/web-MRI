package com.vrris.pacs.dao;

import com.vrris.pacs.model.Report;

import java.util.List;
import java.util.Optional;

public interface ReportDao extends BaseDao<Report, Long> {

    /**
     * Bao cao hien hanh cua mot ca chup: ban co version lon nhat (UK(study_id, version),
     * doc/api/openapi.yaml operationId getCurrentReport).
     */
    Optional<Report> findCurrentByStudyId(Long studyId);

    /**
     * Moi version cua mot ca chup, version lon nhat truoc (doc/api/openapi.yaml operationId
     * listStudyReports, IN-10) - moi version la mot ban ghi rieng, ban cu khong bi sua/ghi de.
     */
    List<Report> findAllByStudyId(Long studyId);
}
