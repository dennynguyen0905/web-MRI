package com.vrris.pacs.dao;

import com.vrris.pacs.model.ClinicalSnapshot;

import java.util.Optional;

public interface ClinicalSnapshotDao extends BaseDao<ClinicalSnapshot, Long> {

    /** Moi chi dinh co nhieu nhat mot snapshot (uk_clinical_snapshot_order). */
    Optional<ClinicalSnapshot> findByOrderId(Long orderId);
}
