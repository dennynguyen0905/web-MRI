package com.vrris.pacs.dao;

import com.vrris.pacs.model.Report;

import java.util.Optional;

public interface ReportDao extends BaseDao<Report, Long> {

    /**
     * Bao cao hien hanh cua mot ca chup: ban co version lon nhat (UK(study_id, version),
     * doc/api/openapi.yaml operationId getCurrentReport).
     */
    Optional<Report> findCurrentByStudyId(Long studyId);
}
