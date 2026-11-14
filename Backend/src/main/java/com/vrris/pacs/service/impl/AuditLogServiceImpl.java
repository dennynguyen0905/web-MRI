package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.dto.response.AuditLogPageResponse;
import com.vrris.pacs.dto.response.AuditLogResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.mapper.AuditLogMapper;
import com.vrris.pacs.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogDao auditLogDao;
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogDao auditLogDao, AuditLogMapper auditLogMapper) {
        this.auditLogDao = auditLogDao;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "AUDIT_LOG")
    public AuditLogPageResponse search(Long patientId, Long actorUserId, String action, String entityType,
                                        Long entityId, LocalDateTime occurredFrom, LocalDateTime occurredTo,
                                        Integer page, Integer pageSize) {
        int resolvedPage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int resolvedPageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        AuditLogDao.SearchCriteria criteria = new AuditLogDao.SearchCriteria(
                patientId, actorUserId, action, entityType, entityId,
                occurredFrom, occurredTo, resolvedPage, resolvedPageSize);

        AuditLogDao.SearchResult result = auditLogDao.search(criteria);
        List<AuditLogResponse> items = result.items().stream()
                .map(auditLogMapper::toResponse)
                .toList();

        int totalPages = result.total() == 0 ? 0 : (int) Math.ceil((double) result.total() / resolvedPageSize);

        return new AuditLogPageResponse(items, result.total(), resolvedPage, resolvedPageSize, totalPages);
    }
}
