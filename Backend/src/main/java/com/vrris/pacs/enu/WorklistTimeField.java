package com.vrris.pacs.enu;

/**
 * Gia tri tham so "time_field" cua GET /studies va GET /studies/status-counts (doc/api/openapi.yaml):
 * chon cot thoi gian ma date_from/date_to ap vao. Khong luu CSDL.
 */
public enum WorklistTimeField {
    SCHEDULED,  // 0 - study.scheduled_at (mac dinh)
    ORDERED,    // 1 - imaging_order.ordered_at
    COMPLETED   // 2 - study.completed_at
}
