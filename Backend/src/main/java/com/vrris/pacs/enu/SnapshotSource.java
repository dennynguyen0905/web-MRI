package com.vrris.pacs.enu;

/** Khop cot "clinical_snapshot.source" (database/migrations/V6__clinical_snapshot.sql: 'SEED|HIS'). */
public enum SnapshotSource {
    SEED,  // 0 - du lieu gia lap nap san (seed V7/V8)
    HIS    // 1 - lay tu HIS qua integration.HisGateway
}
