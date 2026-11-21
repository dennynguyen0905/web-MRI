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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
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
    private static final int MIN_BIRTH_YEAR = 1900;

    private final IdentityConflictDao identityConflictDao;
    private final PatientDao patientDao;
    private final AppUserDao appUserDao;
    private final IdentityConflictMapper identityConflictMapper;

    // Khởi tạo service với các DAO liên quan (lệch dữ liệu, bệnh nhân, người dùng) và mapper.
    public IdentityConflictServiceImpl(IdentityConflictDao identityConflictDao, PatientDao patientDao,
                                        AppUserDao appUserDao, IdentityConflictMapper identityConflictMapper) {
        this.identityConflictDao = identityConflictDao;
        this.patientDao = patientDao;
        this.appUserDao = appUserDao;
        this.identityConflictMapper = identityConflictMapper;
    }

    // Tra cứu danh sách lệch dữ liệu HIS/RIS theo điều kiện lọc (trạng thái, nguồn, trường, bệnh nhân, thời gian), có phân trang và sắp xếp.
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

    // Lấy chi tiết một bản ghi lệch dữ liệu theo id.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "IDENTITY_CONFLICT", entityIdArg = 0)
    public IdentityConflictResponse getById(Long id) {
        IdentityConflict conflict = findOrThrow(id);
        AuditContext.recordPatientId(conflict.getPatientId());
        return toResponse(conflict);
    }

    // Giải quyết một bản ghi lệch dữ liệu đang PENDING: áp giá trị từ HIS hoặc giá trị nhập tay vào bệnh nhân, hoặc giữ nguyên RIS; chặn nếu HIS không có giá trị.
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
            if (newValue == null || newValue.isBlank()) {
                // Chi xay ra voi TAKE_HIS (MANUAL_EDIT da kiem o tren): truoc day gay NPE trong Map.of
                // hoac ghi null vao cot NOT NULL (full_name, gender...) -> 500.
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                        "Lech du lieu id=" + id + " khong co gia tri HIS - dung KEEP_RIS hoac MANUAL_EDIT");
            }
            applyToPatient(conflict, newValue.trim());
        }

        conflict.setResolution(request.resolution().name());
        conflict.setResolvedByUserId(appUserDao.findByUsername(currentUsername()).map(AppUser::getId).orElse(null));
        conflict.setResolvedAt(LocalDateTime.now());
        return toResponse(identityConflictDao.save(conflict));
    }

    // Áp giá trị mới vào trường tương ứng của bệnh nhân (sau khi chặn trùng định danh) và ghi lại giá trị trước/sau vào audit context.
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
        rejectDuplicateIdentity(patient, fieldName, newValue);
        applyFieldValue(patient, fieldName, newValue);

        patient.setUpdatedAt(LocalDateTime.now());
        patient.setUpdatedBy(currentUsername());
        patientDao.save(patient);

        // LinkedHashMap (khong phai Map.of) vi gia tri cu co the null.
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("field_name", fieldName);
        before.put("value", oldValue == null ? null : String.valueOf(oldValue));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("field_name", fieldName);
        after.put("value", newValue);
        AuditContext.recordBefore(before);
        AuditContext.recordAfter(after);
    }

    /**
     * Cung quy tac voi PatientServiceImpl: patient_code la khoa duy nhat (UNIQUE uk_patient_code) va
     * so BHXH trung voi benh nhan khac la dau hieu nghi trung nguoi - bao loi nghiep vu ro rang thay vi
     * de DB tu choi luc commit (500/409 chung chung) hoac tao hai ho so cung BHXH.
     */
    // Chặn áp giá trị mới nếu mã bệnh nhân hoặc số BHXH mới đã gắn với một bệnh nhân khác (nghi trùng người).
    private void rejectDuplicateIdentity(Patient patient, String fieldName, String newValue) {
        if ("patient_code".equals(fieldName)) {
            patientDao.findByPatientCode(newValue)
                    .filter(existing -> !existing.getId().equals(patient.getId()))
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "PATIENT_DUPLICATE_CODE",
                                "Ma benh nhan '" + newValue + "' da ton tai");
                    });
        }
        if ("social_insurance_no".equals(fieldName)) {
            patientDao.findBySocialInsuranceNo(newValue)
                    .filter(existing -> !existing.getId().equals(patient.getId()))
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "PATIENT_SUSPECTED_DUPLICATE",
                                "So BHXH da gan voi benh nhan khac (ma " + existing.getPatientCode()
                                        + ") - nghi trung nguoi (04-his-ris-reconciliation.md muc 1.1 truong hop C)");
                    });
        }
    }

    // Đọc giá trị hiện tại của một trường định danh trên bệnh nhân theo tên trường.
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

    // Gán giá trị mới (dạng chuỗi) vào trường tương ứng trên bệnh nhân theo tên trường.
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

    // Parse chuỗi giới tính thành giá trị enum Gender hợp lệ, báo lỗi 422 nếu không parse được.
    private String parseGender(String rawValue) {
        try {
            return Gender.valueOf(rawValue.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                    "Gia tri gioi tinh '" + rawValue + "' khong hop le (M/F/O/U)");
        }
    }

    // Parse chuỗi ngày sinh (hỗ trợ YYYY, YYYY-MM, YYYY-MM-DD) và gán vào bệnh nhân kèm độ chính xác tương ứng.
    private void applyDateOfBirth(Patient patient, String rawValue) {
        if (rawValue == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                    "Gia tri ngay sinh rong");
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() == 4 && trimmed.chars().allMatch(Character::isDigit)) {
            setDateOfBirth(patient, rawValue, () -> LocalDate.of(Integer.parseInt(trimmed), 1, 1), DobPrecision.YEAR);
            return;
        }
        if (MONTH_PATTERN.matcher(trimmed).matches()) {
            setDateOfBirth(patient, rawValue, () -> LocalDate.parse(trimmed + "-01"), DobPrecision.MONTH);
            return;
        }
        if (DAY_PATTERN.matcher(trimmed).matches()) {
            setDateOfBirth(patient, rawValue, () -> LocalDate.parse(trimmed), DobPrecision.DAY);
            return;
        }
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                "Khong parse duoc gia tri ngay sinh '" + rawValue + "' (ho tro YYYY, YYYY-MM, YYYY-MM-DD)");
    }

    /**
     * Regex chi kiem dinh dang - ngay khong ton tai (2026-13-45, 2025-02-30) lam LocalDate nem
     * DateTimeException -> truoc day thanh 500. Ngay sinh o tuong lai hoac truoc nam MIN_BIRTH_YEAR
     * cung bi tu choi: rule.AgeCalculator nem loi khi asOf truoc dateOfBirth, nghia la moi lan doc
     * benh nhan do ve sau deu 500; MySQL DATE khong nhan nam truoc 1000.
     */
    // Parse và kiểm tra ngày sinh hợp lệ (tồn tại thật, không ở tương lai, không trước năm tối thiểu) trước khi gán vào bệnh nhân.
    private void setDateOfBirth(Patient patient, String rawValue, Supplier<LocalDate> parser, DobPrecision precision) {
        LocalDate dob;
        try {
            dob = parser.get();
        } catch (DateTimeException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                    "Ngay sinh '" + rawValue + "' khong ton tai");
        }
        if (dob.isAfter(LocalDate.now()) || dob.getYear() < MIN_BIRTH_YEAR) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_VALUE_INVALID",
                    "Ngay sinh '" + rawValue + "' nam ngoai khoang hop le (" + MIN_BIRTH_YEAR + " den hom nay)");
        }
        patient.setDateOfBirth(dob);
        patient.setDobPrecision(precision.name());
    }

    // Tạo exception báo trường dữ liệu chưa được hỗ trợ áp dụng tự động.
    private ApiException unsupportedField(String fieldName) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDENTITY_CONFLICT_FIELD_UNSUPPORTED",
                "Truong '" + fieldName + "' chua duoc ho tro ap dung tu dong (04-his-ris-reconciliation.md muc 2.1)");
    }

    // Tìm bản ghi lệch dữ liệu theo id, báo lỗi 404 nếu không tồn tại.
    private IdentityConflict findOrThrow(Long id) {
        return identityConflictDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IDENTITY_CONFLICT_NOT_FOUND",
                        "Khong tim thay lech du lieu id=" + id));
    }

    // Chuyển entity IdentityConflict sang DTO, kèm thông tin người đã giải quyết (nếu có).
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
