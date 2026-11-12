package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.IdentityConflictDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dto.request.ConflictResolutionRequest;
import com.vrris.pacs.dto.response.IdentityConflictPageResponse;
import com.vrris.pacs.dto.response.IdentityConflictResponse;
import com.vrris.pacs.dto.response.UserRef;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.ConflictSource;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.IdentityConflictMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.IdentityConflict;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.service.IdentityConflictService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * IN-05 (04-his-ris-reconciliation.md muc 2, 4): xu ly cac dong identity_conflict DA CO. Pipeline
 * phat hien/tao conflict luc dong bo HIS (04-his-ris-reconciliation.md muc 1, 9) KHONG thuoc pham
 * vi nay - xem comment o ImagingOrderServiceImpl/PatientServiceImpl.
 *
 * RBAC (RBAC-11, RC-18): chan o rest.IdentityConflictController bang @PreAuthorize - service khong
 * lap lai kiem tra quyen o day.
 */
@Service
public class IdentityConflictServiceImpl implements IdentityConflictService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final Pattern MONTH_PATTERN = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern DAY_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final IdentityConflictDao identityConflictDao;
    private final PatientDao patientDao;
    private final AppUserDao appUserDao;
    private final IdentityConflictMapper identityConflictMapper;

    public IdentityConflictServiceImpl(IdentityConflictDao identityConflictDao, PatientDao patientDao,
                                        AppUserDao appUserDao, IdentityConflictMapper identityConflictMapper) {
        this.identityConflictDao = identityConflictDao;
        this.patientDao = patientDao;
        this.appUserDao = appUserDao;
        this.identityConflictMapper = identityConflictMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "IDENTITY_CONFLICT")
    public IdentityConflictPageResponse search(ConflictResolution resolution, ConflictSource source, String fieldName,
                                                Long patientId, LocalDateTime detectedFrom, LocalDateTime detectedTo,
                                                Integer page, Integer pageSize, String sort) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        SortSpec sortSpec = parseSort(sort);

        IdentityConflictDao.SearchCriteria criteria = new IdentityConflictDao.SearchCriteria(
                resolution == null ? null : resolution.name(),
                source == null ? null : source.name(),
                blankToNull(fieldName), patientId, detectedFrom, detectedTo,
                safePage, safePageSize, sortSpec.field(), sortSpec.ascending());
        IdentityConflictDao.SearchResult result = identityConflictDao.search(criteria);

        List<IdentityConflictResponse> items = result.items().stream().map(this::toResponse).toList();
        return new IdentityConflictPageResponse(safePage, safePageSize, result.total(), items);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "IDENTITY_CONFLICT", entityIdArg = 0)
    public IdentityConflictResponse getById(Long id) {
        IdentityConflict conflict = findOrThrow(id);
        AuditContext.recordPatientId(conflict.getPatientId());
        return toResponse(conflict);
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = "IDENTITY_CONFLICT", entityIdArg = 0)
    public IdentityConflictResponse resolve(Long id, ConflictResolutionRequest request) {
        IdentityConflict conflict = findOrThrow(id);
        AuditContext.recordPatientId(conflict.getPatientId());

        if (!ConflictResolution.PENDING.name().equals(conflict.getResolution())) {
            throw new ApiException(HttpStatus.CONFLICT, "IDENTITY_CONFLICT_ALREADY_RESOLVED",
                    "Lech du lieu id=" + id + " khong con o trang thai PENDING");
        }
        if (request.resolution() == ConflictResolution.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "resolution phai la KEEP_RIS, TAKE_HIS hoac MANUAL_EDIT");
        }
        if (request.resolution() == ConflictResolution.MANUAL_EDIT
                && (request.manualValue() == null || request.manualValue().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "manual_value la bat buoc khi resolution = MANUAL_EDIT");
        }

        if (request.resolution() == ConflictResolution.TAKE_HIS || request.resolution() == ConflictResolution.MANUAL_EDIT) {
            String newValue = request.resolution() == ConflictResolution.TAKE_HIS
                    ? conflict.getHisValue() : request.manualValue();
            applyToPatient(conflict, newValue);
        }

        conflict.setResolution(request.resolution().name());
        conflict.setResolvedByUserId(currentUserId());
        conflict.setResolvedAt(LocalDateTime.now());
        IdentityConflict saved = identityConflictDao.save(conflict);

        return toResponse(saved);
    }

    /**
     * Doi du lieu patient theo field_name cua conflict (04-his-ris-reconciliation.md muc 2, 3) va
     * ghi before/after qua AuditContext (AuditLogAspect doc va serialize - khong tu ghi audit_log
     * o day, dung quy uoc doc/15-kien-truc-backend.md muc 10). Khong co patient_id (truong hop C/D
     * cua muc 1.1 - chua khop duoc benh nhan) thi KHONG co cho de ghi gia tri -> 422.
     */
    private void applyToPatient(IdentityConflict conflict, String newValue) {
        if (conflict.getPatientId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_PATIENT_NOT_LINKED",
                    "Lech du lieu id=" + conflict.getId() + " chua gan voi benh nhan nao - khong the TAKE_HIS/MANUAL_EDIT");
        }
        Patient patient = patientDao.findById(conflict.getPatientId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_PATIENT_NOT_LINKED",
                        "Khong tim thay benh nhan id=" + conflict.getPatientId()));

        String fieldName = conflict.getFieldName();
        Object oldValue = currentFieldValue(patient, fieldName);
        applyFieldValue(patient, fieldName, newValue);

        String actor = currentUsername();
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setUpdatedBy(actor);
        patientDao.save(patient);

        AuditContext.recordBefore(Map.of("field_name", fieldName, "value", String.valueOf(oldValue)));
        AuditContext.recordAfter(Map.of("field_name", fieldName, "value", newValue));
    }

    private Object currentFieldValue(Patient patient, String fieldName) {
        return switch (fieldName) {
            case "patient_code" -> patient.getPatientCode();
            case "full_name" -> patient.getFullName();
            case "social_insurance_no" -> patient.getSocialInsuranceNo();
            case "phone" -> patient.getPhone();
            case "address" -> patient.getAddress();
            case "gender" -> patient.getGender();
            case "date_of_birth" -> patient.getDateOfBirth();
            default -> throw unsupportedField(fieldName);
        };
    }

    /**
     * Chi 6 truong o bang "04-his-ris-reconciliation.md" muc 2.1 duoc ap dung tu dong; patient_code
     * (khoa chinh, truong hop C/D muc 1.1 - gop danh tinh) duoc liet ke nhung chua co xu ly gop ban
     * ghi day du (ngoai pham vi IN-05 lan nay, van ghi gia tri moi o day de khop mo ta openapi
     * "TAKE_HIS/MANUAL_EDIT doi du lieu patient") ❓ chua duoc nhom xac nhan.
     */
    private void applyFieldValue(Patient patient, String fieldName, String rawValue) {
        switch (fieldName) {
            case "patient_code" -> patient.setPatientCode(rawValue);
            case "full_name" -> patient.setFullName(rawValue);
            case "social_insurance_no" -> patient.setSocialInsuranceNo(rawValue);
            case "phone" -> patient.setPhone(rawValue);
            case "address" -> patient.setAddress(rawValue);
            case "gender" -> patient.setGender(parseGender(rawValue));
            case "date_of_birth" -> applyDateOfBirth(patient, rawValue);
            default -> throw unsupportedField(fieldName);
        }
    }

    private String parseGender(String rawValue) {
        try {
            return Gender.valueOf(rawValue.toUpperCase()).name();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                    "Gia tri gioi tinh '" + rawValue + "' khong hop le (M/F/O/U)");
        }
    }

    /**
     * Dinh dang gia tri da luu trong identity_conflict.his_value/manual_value - "YYYY" (nam),
     * "YYYY-MM" (thang) hoac "YYYY-MM-DD" (ngay), khop cach ris_value/his_value duoc the hien o vi
     * du cua 04-his-ris-reconciliation.md muc 2.2 (vi du "1958-03-15", "1958"). Doi voi du lieu HIS
     * tho dang HL7 PID-7 (4/6/8 ky tu lien tiep), buoc chuan hoa ve dang nay thuoc pipeline dong bo
     * (muc 6.2) - KHONG lam o day (ngoai pham vi IN-05 lan nay).
     */
    private void applyDateOfBirth(Patient patient, String rawValue) {
        if (rawValue == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                    "Gia tri ngay sinh rong");
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() == 4 && trimmed.chars().allMatch(Character::isDigit)) {
            patient.setDateOfBirth(LocalDate.of(Integer.parseInt(trimmed), 1, 1));
            patient.setDobPrecision(DobPrecision.YEAR.name());
            return;
        }
        if (MONTH_PATTERN.matcher(trimmed).matches()) {
            patient.setDateOfBirth(LocalDate.parse(trimmed + "-01"));
            patient.setDobPrecision(DobPrecision.MONTH.name());
            return;
        }
        if (DAY_PATTERN.matcher(trimmed).matches()) {
            patient.setDateOfBirth(LocalDate.parse(trimmed));
            patient.setDobPrecision(DobPrecision.DAY.name());
            return;
        }
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                "Khong parse duoc gia tri ngay sinh '" + rawValue + "' (ho tro YYYY, YYYY-MM, YYYY-MM-DD)");
    }

    private ApiException unsupportedField(String fieldName) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_FIELD_UNSUPPORTED",
                "Truong '" + fieldName + "' chua duoc ho tro ap dung tu dong (04-his-ris-reconciliation.md muc 2.1)");
    }

    private IdentityConflict findOrThrow(Long id) {
        return identityConflictDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IDENTITY_CONFLICT_NOT_FOUND",
                        "Khong tim thay lech du lieu id=" + id));
    }

    private IdentityConflictResponse toResponse(IdentityConflict conflict) {
        IdentityConflictResponse base = identityConflictMapper.toResponse(conflict);
        UserRef resolvedBy = conflict.getResolvedByUserId() == null ? null
                : appUserDao.findById(conflict.getResolvedByUserId())
                        .map(u -> new UserRef(u.getId(), u.getUsername(), u.getFullName()))
                        .orElse(null);
        return new IdentityConflictResponse(base.id(), base.patientId(), base.orderId(), base.source(),
                base.fieldName(), base.risValue(), base.hisValue(), base.detectedAt(), base.resolution(),
                resolvedBy, base.resolvedAt());
    }

    private Long currentUserId() {
        return appUserDao.findByUsername(currentUsername()).map(AppUser::getId).orElse(null);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * Chi ho tro sap theo detected_at - khop index IDX(resolution, detected_at) da co
     * (database/migrations/V5__audit_and_conflict.sql). Mac dinh TANG DAN (cu nhat truoc) de phuc
     * vu hang cho ra soat theo FIFO (04-his-ris-reconciliation.md muc 4: canh bao PENDING qua 3 ngay
     * lam viec) ❓ chua duoc nhom xac nhan chieu mac dinh.
     */
    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("detectedAt", true);
        }
        String[] parts = sort.split(",", 2);
        String rawField = parts[0].trim();
        if (!"detected_at".equals(rawField)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'sort': truong '" + rawField + "' khong ho tro sap xep");
        }
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'sort': chieu sap xep phai la 'asc' hoac 'desc'");
        }
        return new SortSpec("detectedAt", direction.equals("asc"));
    }

    private record SortSpec(String field, boolean ascending) {
    }
}
