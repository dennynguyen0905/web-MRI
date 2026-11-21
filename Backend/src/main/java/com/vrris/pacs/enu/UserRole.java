package com.vrris.pacs.enu;

/**
 * 5 vai tro nguoi dung (README Phan 5, doc/06-rbac-matrix.md) - khop bang role.code (V2) va claim
 * "roles" cua JWT. Spring Security gan authority "ROLE_" + ten vai tro (security.JwtAuthenticationFilter).
 *
 * Dung enum nay thay cho chuoi "ROLE_ADMIN"... viet cung rai rac trong service.impl. Chuoi trong
 * @PreAuthorize (rest.RestSupport) van la SpEL nen giu dang hang so chuoi o do.
 */
public enum UserRole {
    ADMIN, CLINICIAN, TECHNICIAN, RADIOLOGIST, APPROVER;

    private static final String AUTHORITY_PREFIX = "ROLE_";

    /** Ten authority Spring Security tuong ung, vi du "ROLE_ADMIN". */
    public String authority() {
        return AUTHORITY_PREFIX + name();
    }

    /** Doi authority ("ROLE_ADMIN") ve enum; null khi khong phai mot trong 5 vai tro. */
    public static UserRole fromAuthority(String authority) {
        if (authority == null || !authority.startsWith(AUTHORITY_PREFIX)) {
            return null;
        }
        String code = authority.substring(AUTHORITY_PREFIX.length());
        for (UserRole role : values()) {
            if (role.name().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
