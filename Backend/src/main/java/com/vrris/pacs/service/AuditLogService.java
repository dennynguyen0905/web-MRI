package com.vrris.pacs.service;

import com.vrris.pacs.dto.response.AuditLogPageResponse;

import java.time.LocalDateTime;

public interface AuditLogService {

    AuditLogPageResponse search(Long patientId, Long actorUserId, String action, String entityType,
                                 Long entityId, LocalDateTime occurredFrom, LocalDateTime occurredTo,
                                 Integer page, Integer pageSize);
}
