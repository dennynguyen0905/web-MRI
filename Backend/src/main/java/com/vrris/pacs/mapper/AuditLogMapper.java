package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.AuditLogResponse;
import com.vrris.pacs.model.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
