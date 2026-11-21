package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.dto.response.AuditLogPageResponse;
import com.vrris.pacs.dto.response.AuditLogResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.mapper.AuditLogMapper;
import com.vrris.pacs.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.vrris.pacs.service.impl.ServiceSupport.DEFAULT_PAGE_SIZE;
import static com.vrris.pacs.service.impl.ServiceSupport.parseEnumOrNull;
import static com.vrris.pacs.service.impl.ServiceSupport.safePage;
import static com.vrris.pacs.service.impl.ServiceSupport.safePageSize;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_AUDIT_PAGE_SIZE = 100;

    private final AuditLogDao auditLogDao;
    private final AuditLogMapper auditLogMapper;

    // Khởi tạo service với DAO và mapper của audit_log.
    public AuditLogServiceImpl(AuditLogDao auditLogDao, AuditLogMapper auditLogMapper) {
        this.auditLogDao = auditLogDao;
        this.auditLogMapper = auditLogMapper;
    }

    // Tra cứu audit_log theo nhiều điều kiện lọc (bệnh nhân, người thực hiện, hành động, loại entity, khoảng thời gian), có phân trang.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.AUDIT_LOG)
    public AuditLogPageResponse search(Long patientId, Long actorUserId, String action, String entityType,
                                        Long entityId, LocalDateTime occurredFrom, LocalDateTime occurredTo,
                                        Integer page, Integer pageSize) {
        int resolvedPage = safePage(page);
        int resolvedPageSize = safePageSize(pageSize, DEFAULT_PAGE_SIZE, MAX_AUDIT_PAGE_SIZE);

        AuditLogDao.SearchCriteria criteria = new AuditLogDao.SearchCriteria(
                patientId, actorUserId,
                parseEnumOrNull(action, AuditAction.class, "action"),
                parseEnumOrNull(entityType, AuditEntityType.class, "entity_type"),
                entityId,
                occurredFrom, occurredTo, resolvedPage, resolvedPageSize);

        AuditLogDao.SearchResult result = auditLogDao.search(criteria);
        List<AuditLogResponse> items = result.items().stream().map(auditLogMapper::toResponse).toList();
        int totalPages = result.total() == 0 ? 0 : (int) Math.ceil((double) result.total() / resolvedPageSize);

        return new AuditLogPageResponse(items, result.total(), resolvedPage, resolvedPageSize, totalPages);
    }
}
