package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dto.request.ListExportEventRequest;
import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.PatientPageResponse;
import com.vrris.pacs.dto.response.PatientResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.UserRole;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.rule.PatientListCsvExporter;
import com.vrris.pacs.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.vrris.pacs.service.impl.ServiceSupport.MAX_EXPORT_ROWS;
import static com.vrris.pacs.service.impl.ServiceSupport.SortSpec;
import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.currentAuth;
import static com.vrris.pacs.service.impl.ServiceSupport.currentUsername;
import static com.vrris.pacs.service.impl.ServiceSupport.hasAnyAuthority;
import static com.vrris.pacs.service.impl.ServiceSupport.parseSort;
import static com.vrris.pacs.service.impl.ServiceSupport.safePage;
import static com.vrris.pacs.service.impl.ServiceSupport.safePageSize;
import static com.vrris.pacs.service.impl.ServiceSupport.toPatientResponseWithAge;

@Service
public class PatientServiceImpl implements PatientService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "patient_code", "patientCode",
            "full_name", "fullName",
            "date_of_birth", "dateOfBirth",
            "created_at", "createdAt"
    );

    private final PatientDao patientDao;
    private final PatientMapper patientMapper;

    // Khởi tạo service với DAO và mapper của bệnh nhân.
    public PatientServiceImpl(PatientDao patientDao, PatientMapper patientMapper) {
        this.patientDao = patientDao;
        this.patientMapper = patientMapper;
    }

    // Tra cứu danh sách bệnh nhân theo từ khóa/mã/tên, có phân trang và sắp xếp, kèm tính tuổi.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.PATIENT)
    public PatientPageResponse search(String q, String patientCode, String fullName,
                                       Integer page, Integer pageSize, String sort) {
        int resolvedPage = safePage(page);
        int resolvedPageSize = safePageSize(pageSize);
        SortSpec sortSpec = parseSort(sort, SORTABLE_FIELDS, "fullName", true);

        PatientDao.SearchCriteria criteria = new PatientDao.SearchCriteria(
                blankToNull(q), blankToNull(patientCode), blankToNull(fullName),
                resolvedPage, resolvedPageSize, sortSpec.field(), sortSpec.ascending());
        PatientDao.SearchResult result = patientDao.search(criteria);

        List<PatientResponse> items = result.items().stream()
                .map(p -> toPatientResponseWithAge(p, patientMapper))
                .toList();
        return new PatientPageResponse(resolvedPage, resolvedPageSize, result.total(), items);
    }

    // Lấy chi tiết một bệnh nhân theo id, kèm tính tuổi.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.PATIENT, patientIdArg = 0, entityIdArg = 0)
    public PatientResponse getById(Long id) {
        return toPatientResponseWithAge(findOrThrow(id), patientMapper);
    }

    // Tạo bệnh nhân mới; chặn trùng mã bệnh nhân và nghi trùng người qua số BHXH.
    @Override
    @Transactional
    @Audited(action = AuditAction.CREATE, entityType = AuditEntityType.PATIENT)
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
        AuditContext.recordPatientId(saved.getId());
        return toPatientResponseWithAge(saved, patientMapper);
    }

    // Cập nhật thông tin bệnh nhân; chặn CLINICIAN sửa trường định danh thực sự thay đổi (D24) và chặn nghi trùng BHXH.
    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = AuditEntityType.PATIENT, patientIdArg = 0, entityIdArg = 0)
    public PatientResponse update(Long id, PatientUpdateRequest request) {
        Patient patient = findOrThrow(id);

        if (!hasAnyAuthority(currentAuth(), UserRole.ADMIN.authority(), UserRole.TECHNICIAN.authority()) && hasIdentityFieldChange(patient, request)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "CLINICIAN khong duoc sua cac truong dinh danh benh nhan (D24)");
        }

        rejectSuspectedDuplicateBhxh(request.socialInsuranceNo(), id);

        patientMapper.updateEntityFromRequest(request, patient);
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setUpdatedBy(currentUsername());

        return toPatientResponseWithAge(patientDao.save(patient), patientMapper);
    }

    // Xuất danh sách bệnh nhân (theo điều kiện lọc hiện tại) ra file CSV.
    @Override
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = AuditEntityType.PATIENT)
    public ExportDocumentResponse exportCsv(String q, String patientCode, String fullName, String sort) {
        SortSpec sortSpec = parseSort(sort, SORTABLE_FIELDS, "fullName", true);
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
                        p.getDobPrecision() == null ? DobPrecision.DAY : p.getDobPrecision(),
                        p.getGender(),
                        p.getSocialInsuranceNo(),
                        p.getPhone(),
                        p.getAddress(),
                        p.getCreatedAt()
                ))
                .toList();

        Long singlePatientId = result.items().size() == 1 ? result.items().get(0).getId() : null;
        LocalDate today = LocalDate.now();
        return new ExportDocumentResponse(
                PatientListCsvExporter.patientListFileName(today),
                "text/csv;charset=UTF-8",
                PatientListCsvExporter.exportPatients(rows, today),
                rows.size(),
                singlePatientId);
    }

    // Ghi nhận sự kiện đã xuất danh sách bệnh nhân (phục vụ audit_log), không trả dữ liệu.
    @Override
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = AuditEntityType.PATIENT)
    public void recordExportEvent(ListExportEventRequest request) {
        if (request.patientId() != null) {
            findOrThrow(request.patientId());
        }
    }

    /**
     * Chi coi la "sua truong dinh danh" khi gia tri gui len KHAC gia tri dang luu - form FE gui lai ca
     * cac truong khong doi (ho ten, ngay sinh...) khi CLINICIAN chi sua SDT/dia chi, khong duoc chan 403.
     */
    // Kiểm tra request cập nhật có thực sự đổi giá trị trường định danh (họ tên, ngày sinh, giới tính, BHXH) so với dữ liệu đang lưu hay không.
    private boolean hasIdentityFieldChange(Patient patient, PatientUpdateRequest request) {
        return changed(request.fullName(), patient.getFullName())
                || changed(request.dateOfBirth(), patient.getDateOfBirth())
                || changed(request.dobPrecision(), patient.getDobPrecision())
                || changed(request.gender(), patient.getGender())
                || changed(request.socialInsuranceNo(), patient.getSocialInsuranceNo());
    }

    // So sánh giá trị mới gửi lên với giá trị hiện tại, coi là "đổi" chỉ khi giá trị mới khác null và khác giá trị cũ.
    private static boolean changed(Object requested, Object current) {
        return requested != null && !requested.equals(current);
    }

    // Tìm bệnh nhân theo id, báo lỗi 404 nếu không tồn tại.
    private Patient findOrThrow(Long id) {
        return patientDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND",
                        "Khong tim thay benh nhan id=" + id));
    }

    // Chặn tạo/sửa bệnh nhân nếu số BHXH đã gắn với một bệnh nhân khác (nghi trùng người).
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
}
