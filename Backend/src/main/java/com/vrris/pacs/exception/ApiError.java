package com.vrris.pacs.exception;

import java.util.List;

/**
 * Khop dung schema "Error" trong doc/api/openapi.yaml (components.schemas.Error):
 * required [code, message], "details" la danh sach tuy chon.
 */
public record ApiError(String code, String message, List<ApiErrorDetail> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<ApiErrorDetail> details) {
        return new ApiError(code, message, details);
    }
}
