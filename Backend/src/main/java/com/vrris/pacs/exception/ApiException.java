package com.vrris.pacs.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception nghiep vu dung chung, mang theo ma loi (ApiError.code) va HTTP status
 * theo bang "Ma loi" trong doc/api/openapi.yaml.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
