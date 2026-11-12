package com.vrris.pacs.dao;

import com.vrris.pacs.model.IdentityConflict;

import java.time.LocalDateTime;
import java.util.List;

public interface IdentityConflictDao extends BaseDao<IdentityConflict, Long> {

    SearchResult search(SearchCriteria criteria);

    /**
     * Tat ca tham so loc (tru page/pageSize/sortField/ascending): null = khong loc theo dieu kien
     * do. Khop tham so resolution/source/field_name/patient_id/detected_from/detected_to cua
     * GET /identity-conflicts (doc/api/openapi.yaml). resolution/source la String ("PENDING",
     * "HIS"...) - service da doi tu enu.ConflictResolution/enu.ConflictSource truoc khi goi xuong
     * day, giong cach dao.ImagingOrderDao.SearchCriteria dang lam voi orderCode/visitCode.
     */
    record SearchCriteria(String resolution, String source, String fieldName, Long patientId,
                           LocalDateTime detectedFrom, LocalDateTime detectedTo,
                           int page, int pageSize, String sortField, boolean ascending) {
    }

    record SearchResult(List<IdentityConflict> items, long total) {
    }
}
