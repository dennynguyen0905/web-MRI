package com.vrris.pacs.dao;

import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.ConflictSource;
import com.vrris.pacs.model.IdentityConflict;

import java.time.LocalDateTime;
import java.util.List;

public interface IdentityConflictDao extends BaseDao<IdentityConflict, Long> {

    SearchResult search(SearchCriteria criteria);

    /**
     * Tat ca tham so loc (tru page/pageSize/sortField/ascending): null = khong loc theo dieu kien
     * do. Khop tham so resolution/source/field_name/patient_id/detected_from/detected_to cua
     * GET /identity-conflicts (doc/api/openapi.yaml). resolution/source la enum (null = khong loc).
     */
    record SearchCriteria(ConflictResolution resolution, ConflictSource source, String fieldName, Long patientId,
                           LocalDateTime detectedFrom, LocalDateTime detectedTo,
                           int page, int pageSize, String sortField, boolean ascending) {
    }

    record SearchResult(List<IdentityConflict> items, long total) {
    }
}
