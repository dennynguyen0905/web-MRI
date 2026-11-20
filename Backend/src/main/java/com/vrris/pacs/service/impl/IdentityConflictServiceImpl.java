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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.vrris.pacs.service.impl.ServiceSupport.SortSpec;
import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.currentUsername;
import static com.vrris.pacs.service.impl.ServiceSupport.parseSort;
import static com.vrris.pacs.service.impl.ServiceSupport.safePage;
import static com.vrris.pacs.service.impl.ServiceSupport.safePageSize;

@Service
public class IdentityConflictServiceImpl implements IdentityConflictService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of("detected_at", "detectedAt");
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
        int resolvedPage = safePage(page);
        int resolvedPageSize = safePageSize(pageSize);
        SortSpec sortSpec = parseSort(sort, SORTABLE_FIELDS, "detectedAt", true);

        IdentityConflictDao.SearchCriteria criteria = new IdentityConflictDao.SearchCriteria(
                resolution == null ? null : resolution.name(),
                source == null ? null : source.name(),
                blankToNull(fieldName), patientId, detectedFrom, detectedTo,
                resolvedPage, resolvedPageSize, sortSpec.field(), sortSpec.ascending());
        IdentityConflictDao.SearchResult result = identityConflictDao.search(criteria);

        List<IdentityConflictResponse> items = result.items().stream().map(this::toResponse).toList();
        return new IdentityConflictPageResponse(resolvedPage, resolvedPageSize, result.total(), items);
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
        conflict.setResolvedByUserId(appUserDao.findByUsername(currentUsername()).map(AppUser::getId).orElse(null));
        conflict.setResolvedAt(LocalDateTime.now());
        return toResponse(identityConflictDao.save(conflict));
    }

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

        patient.setUpdatedAt(LocalDateTime.now());
        patient.setUpdatedBy(currentUsername());
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
        return new IdentityConflictResponse(
                base.id(), base.patientId(), base.orderId(), base.source(),
                base.fieldName(), base.risValue(), base.hisValue(), base.detectedAt(), base.resolution(),
                resolvedBy, base.resolvedAt());
    }
}
