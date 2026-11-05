package com.vrris.pacs.enu;

/**
 * Khop cot "audit_log.action" (doc/02-erd.md muc 4.11, database/migrations/V5__audit_and_conflict.sql).
 */
public enum AuditAction {
    READ, VIEW_IMAGE, CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN, PRINT, EXPORT
}
