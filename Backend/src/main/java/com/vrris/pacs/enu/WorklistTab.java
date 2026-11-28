package com.vrris.pacs.enu;

/**
 * Gia tri tham so "tab" cua GET /studies (doc/api/openapi.yaml). Khong luu CSDL - chi la bo loc:
 * PENDING = chi ca SCHEDULED, DONE = moi trang thai con lai.
 */
public enum WorklistTab {
    PENDING,  // 0
    DONE      // 1
}
