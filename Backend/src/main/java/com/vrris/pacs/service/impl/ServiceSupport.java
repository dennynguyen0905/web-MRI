package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dto.response.PatientResponse;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.rule.AgeCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Tien ich noi bo dung chung cho tang service.impl: chuan hoa phan trang, sap xep, chuoi tim kiem,
 * truy xuat nguoi dung dang dang nhap tu SecurityContext va anh xa PatientResponse kem tuoi.
 * Đặt package-private trong service.impl để giữ nguyên kiến trúc phân tầng R-05 (ArchUnit L1-L9).
 */
final class ServiceSupport {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 200;
    static final int MAX_EXPORT_ROWS = 5000;

    record SortSpec(String field, boolean ascending) {
    }

    private ServiceSupport() {
    }

    static int safePage(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    static int safePageSize(Integer pageSize) {
        return safePageSize(pageSize, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    static int safePageSize(Integer pageSize, int defaultSize, int maxSize) {
        return (pageSize == null || pageSize < 1) ? defaultSize : Math.min(pageSize, maxSize);
    }

    static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    static String normalizeLower(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    static SortSpec parseSort(String sort, Map<String, String> sortableFields,
                              String defaultField, boolean defaultAscending) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec(defaultField, defaultAscending);
        }
        String[] parts = sort.split(",", 2);
        String rawField = parts[0].trim();
        String entityField = sortableFields.get(rawField);
        if (entityField == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'sort': truong '" + rawField + "' khong ho tro sap xep");
        }
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'sort': chieu sap xep phai la 'asc' hoac 'desc'");
        }
        return new SortSpec(entityField, direction.equals("asc"));
    }

    static <E extends Enum<E>> String parseEnumOrNull(String raw, Class<E> enumType, String paramName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, raw).name();
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so '" + paramName + "'");
        }
    }

    static Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    static String currentUsername() {
        Authentication auth = currentAuth();
        return (auth == null || auth.getPrincipal() == null) ? "system" : String.valueOf(auth.getPrincipal());
    }

    static Optional<AppUser> currentUser(AppUserDao appUserDao) {
        Authentication auth = currentAuth();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return appUserDao.findByUsername(auth.getName());
    }

    static boolean hasAuthority(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    static boolean hasAnyAuthority(Authentication auth, String... authorities) {
        if (auth == null) {
            return false;
        }
        for (String authority : authorities) {
            if (hasAuthority(auth, authority)) {
                return true;
            }
        }
        return false;
    }

    static PatientResponse toPatientResponseWithAge(Patient patient, PatientMapper patientMapper) {
        PatientResponse base = patientMapper.toResponse(patient);
        if (patient.getDateOfBirth() == null) {
            return base;
        }
        AgeCalculator.Result result = AgeCalculator.calculate(
                patient.getDateOfBirth(), DobPrecision.valueOf(patient.getDobPrecision()), LocalDate.now());
        return new PatientResponse(base.id(), base.patientCode(), base.hisPatientId(), base.fullName(),
                base.dateOfBirth(), base.dobPrecision(), result.years(), result.approximate(),
                base.gender(), base.socialInsuranceNo(), base.phone(), base.address(),
                base.createdAt(), base.updatedAt());
    }
}
