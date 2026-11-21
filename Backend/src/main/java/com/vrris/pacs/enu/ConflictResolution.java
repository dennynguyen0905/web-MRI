package com.vrris.pacs.enu;

/**
 * Khop cot "identity_conflict.resolution" (doc/04-his-ris-reconciliation.md muc 2,
 * database/migrations/V5__audit_and_conflict.sql).
 */
public enum ConflictResolution {
    PENDING,      // 0
    KEEP_RIS,     // 1
    TAKE_HIS,     // 2
    MANUAL_EDIT   // 3
}
