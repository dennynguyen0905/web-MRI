package com.vrris.pacs.enu;

/**
 * Khop cot "imaging_order.sync_status" (doc/02-erd.md, database/migrations/V3__patient_order_study.sql).
 */
public enum SyncStatus {
    NOT_SYNCED,   // 0
    SYNCED,       // 1
    RESULT_SENT,  // 2
    FAILED        // 3
}
