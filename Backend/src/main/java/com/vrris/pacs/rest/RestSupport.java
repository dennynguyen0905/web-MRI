package com.vrris.pacs.rest;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Hang so phan quyen RBAC va tien ich dung chung cho tang REST controller (R-05: chi phu thuoc
 * Spring Web, khong phu thuoc dao/model).
 */
final class RestSupport {

    static final String READ_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";
    static final String WRITE_ORDER_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_CLINICIAN','ROLE_TECHNICIAN')";
    static final String TECH_OR_ADMIN_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_TECHNICIAN')";
    static final String TRANSITION_ROLES =
            "hasAnyAuthority('ROLE_ADMIN','ROLE_TECHNICIAN','ROLE_RADIOLOGIST','ROLE_APPROVER')";
    static final String RADIOLOGIST_ROLE =
            "hasAuthority('ROLE_RADIOLOGIST')";
    static final String ADMIN_ROLE =
            "hasAuthority('ROLE_ADMIN')";

    private RestSupport() {
    }

    static ResponseEntity<Resource> attachment(String fileName, String mediaType, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(mediaType))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }
}
