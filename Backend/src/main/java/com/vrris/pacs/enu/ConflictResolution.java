package com.vrris.pacs.enu;

/**
 * Khop cot "identity_conflict.resolution" (doc/04-his-ris-reconciliation.md muc 2,
 * database/migrations/V5__audit_and_conflict.sql).
 */
public enum ConflictResolution {
    PENDING, KEEP_RIS, TAKE_HIS, MANUAL_EDIT
}
