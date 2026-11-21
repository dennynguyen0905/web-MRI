package com.vrris.pacs.dao;

import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogDao extends BaseDao<AuditLog, Long> {

    SearchResult search(SearchCriteria criteria);

    record SearchCriteria(Long patientId, Long actorUserId, AuditAction action, AuditEntityType entityType,
                          Long entityId, LocalDateTime occurredFrom, LocalDateTime occurredTo,
                          int page, int pageSize) {
    }

    record SearchResult(List<AuditLog> items, long total) {
    }
}
