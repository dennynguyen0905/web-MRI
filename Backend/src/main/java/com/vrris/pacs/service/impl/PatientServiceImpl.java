package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dto.request.ListExportEventRequest;
import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.PatientPageResponse;
import com.vrris.pacs.dto.response.PatientResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.rule.AgeCalculator;
import com.vrris.pacs.rule.PatientListCsvExporter;
import com.vrris.pacs.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * IN-01 (danh sach, tim theo ma/ten, tao, xem, sua - chan trung ma) + IN-15 (audit) qua
 * audit.Audited, dong bo theo doc/15-kien-truc-backend.md muc 10. Chua gan @Audited cho entity_id/
 * patient_id/before_value/after_value (audit.AuditLogAspect hien chi ghi actor/action/entity_type -
 * no ky thuat da ghi o do, khong mo rong trong lan nay).
 *
 * Doi chieu HIS <-> RIS day du (identity_conflict, 04-his-ris-reconciliation.md) thuoc IN-05/IN-02,
 * gan voi luong imaging_order/HisGateway - KHONG lam o day. Endpoint nay chi ap dung truoc mot
 * phan cua muc 1.1 file do (truong hop C: BHXH khop benh nhan khac -> 409) vi doc/api/openapi.yaml
 * da ghi ro yeu cau nay ngay trong mo ta cua createPatient/response 409.
 */
@Service
public class PatientServiceImpl implements PatientService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "patient_code", "patientCode",
            "full_name", "fullName",
            "date_of_birth", "dateOfBirth",
            "created_at", "createdAt"
    );
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_EXPORT_ROWS = 5000;

    private final PatientDao patientDao;
    private final PatientMapper patientMapper;

    public PatientServiceImpl(PatientDao patientDao, PatientMapper patientMapper) {
        this.patientDao = patientDao;
        this.patientMapper = patientMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "PATIENT")
    public PatientPageResponse search(String q, String patientCode, String fullName,
                                       Integer page, Integer pageSize, String sort) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        SortSpec sortSpec = parseSort(sort);

        PatientDao.SearchCriteria criteria = new PatientDao.SearchCriteria(
                blankToNull(q), blankToNull(patientCode), blankToNull(fullName),
                safePage, safePageSize, sortSpec.field(), sortSpec.ascending());
        PatientDao.SearchResult result = patientDao.search(criteria);

        List<PatientResponse> items = result.items().stream().map(this::toResponseWithAge).toList();
        return new PatientPageResponse(safePage, safePageSize, result.total(), items);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "PATIENT")
    public PatientResponse getById(Long id) {
        return toResponseWithAge(findOrThrow(id));
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.CREATE, entityType = "PATIENT")
    public PatientResponse create(PatientCreateRequest request) {
        if (patientDao.findByPatientCode(request.patientCode()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PATIENT_DUPLICATE_CODE",
                    "Ma benh nhan '" + request.patientCode() + "' da ton tai");
        }
        rejectSuspectedDuplicateBhxh(request.socialInsuranceNo(), null);

        Patient entity = patientMapper.toEntity(request);
        String actor = currentUsername();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);

        Patient saved = patientDao.save(entity);
        return toResponseWithAge(saved);
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = "PATIENT")
    public PatientResponse update(Long id, PatientUpdateRequest request) {
        Patient patient = findOrThrow(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTechOrAdmin = hasAuthority(auth, "ROLE_ADMIN") || hasAuthority(auth, "ROLE_TECHNICIAN");

        if (!isTechOrAdmin) {
            // CLINICIAN khong duoc sua cac truong dinh danh benh nhan (D24, doc/06 muc 5)
            if (request.fullName() != null || request.dateOfBirth() != null ||
                    request.dobPrecision() != null || request.gender() != null ||
                    request.socialInsuranceNo() != null) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "CLINICIAN khong duoc sua cac truong dinh danh benh nhan (D24)");
            }
        }

        rejectSuspectedDuplicateBhxh(request.socialInsuranceNo(), id);

        patientMapper.updateEntityFromRequest(request, patient);
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setUpdatedBy(currentUsername());

        Patient saved = patientDao.save(patient);
        return toResponseWithAge(saved);
    }

    @Override
    // KHONG readOnly: AuditLogAspect ghi mot dong audit_log action EXPORT (IN-15, doc/09 muc 2.2).
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = "PATIENT")
    public ExportDocumentResponse exportCsv(String q, String patientCode, String fullName, String sort) {
        SortSpec sortSpec = parseSort(sort);
        PatientDao.SearchCriteria criteria = new PatientDao.SearchCriteria(
                blankToNull(q), blankToNull(patientCode), blankToNull(fullName),
                1, MAX_EXPORT_ROWS, sortSpec.field(), sortSpec.ascending());
        PatientDao.SearchResult result = patientDao.search(criteria);

        List<PatientListCsvExporter.PatientRow> rows = result.items().stream()
                .map(p -> new PatientListCsvExporter.PatientRow(
                        p.getPatientCode(),
                        p.getHisPatientId(),
                        p.getFullName(),
                        p.getDateOfBirth(),
                        p.getDobPrecision() == null ? DobPrecision.DAY : DobPrecision.valueOf(p.getDobPrecision()),
                        p.getGender(),
                        p.getSocialInsuranceNo(),
                        p.getPhone(),
                        p.getAddress(),
                        p.getCreatedAt()
                ))
                .toList();

        Long singlePatientId = result.items().size() == 1 ? result.items().get(0).getId() : null;
        LocalDate today = LocalDate.now();
        byte[] content = PatientListCsvExporter.exportPatients(rows, today);
        return new ExportDocumentResponse(
                PatientListCsvExporter.patientListFileName(today),
                "text/csv;charset=UTF-8",
                content,
                rows.size(),
                singlePatientId
        );
    }

    @Override
    // KHONG readOnly: AuditLogAspect ghi mot dong audit_log action EXPORT (IN-15, doc/09 muc 2.2).
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = "PATIENT")
    public void recordExportEvent(ListExportEventRequest request) {
        if (request.patientId() != null) {
            findOrThrow(request.patientId());
        }
    }

    private Patient findOrThrow(Long id) {
        return patientDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND",
                        "Khong tim thay benh nhan id=" + id));
    }

    private void rejectSuspectedDuplicateBhxh(String socialInsuranceNo, Long excludePatientId) {
        if (socialInsuranceNo == null || socialInsuranceNo.isBlank()) {
            return;
        }
        patientDao.findBySocialInsuranceNo(socialInsuranceNo)
                .filter(existing -> excludePatientId == null || !existing.getId().equals(excludePatientId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "PATIENT_SUSPECTED_DUPLICATE",
                            "So BHXH da gan voi benh nhan khac (ma " + existing.getPatientCode()
                                    + ") - nghi trung nguoi (04-his-ris-reconciliation.md muc 1.1 truong hop C)");
                });
    }

    private PatientResponse toResponseWithAge(Patient patient) {
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

    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("fullName", true);
        }
        String[] parts = sort.split(",", 2);
        String rawField = parts[0].trim();
        String entityField = SORTABLE_FIELDS.get(rawField);
        if (entityField == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'sort': truong '" + rawField + "' khong ho tro sap xep");
        }
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'sort': chieu sap xep phai la 'asc' hoac 'desc'");
        }
        return new SortSpec(entityField, direction.equals("asc"));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
    }

    private record SortSpec(String field, boolean ascending) {
    }
}
