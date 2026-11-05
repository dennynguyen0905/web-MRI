package com.vrris.pacs.exception;

/**
 * Khop field "details[]" cua schema Error trong doc/api/openapi.yaml.
 */
public record ApiErrorDetail(String field, String issue) {
}
