package com.vrris.pacs.enu;

/**
 * Khop cot "imaging_order.sync_status" (doc/02-erd.md, database/migrations/V3__patient_order_study.sql).
 */
public enum SyncStatus {
    NOT_SYNCED, SYNCED, RESULT_SENT, FAILED
}
