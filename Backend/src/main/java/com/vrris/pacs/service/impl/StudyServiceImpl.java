package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.StudyMapper;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.rule.AccessionNumberGenerator;
import com.vrris.pacs.rule.PatientListCsvExporter;
import com.vrris.pacs.service.StudyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * IN-02/IN-03 (them ca chup vao chi dinh, GET /studies rut gon - xem Javadoc dao.StudyDao) + IN-15
 * (audit) qua audit.Audited, dong bo theo doc/15-kien-truc-backend.md muc 10.
 *
 * Worklist day du (IN-07: 2 tab, bo loc khoa/modality/thoi gian, dem theo trang thai IN-09),
 * chuyen trang thai (IN-06, rule.StudyStateMachine), bao cao (IN-10) deu CHUA lam o day - ngoai
 * pham vi "entity + DAO + mot luong rest->service->dao->model" cua lan nay.
 */
@Service
public class StudyServiceImpl implements StudyService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "scheduled_at", "scheduledAt",
            "status", "status",
            "created_at", "createdAt"
    );
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_EXPORT_ROWS = 5000;

    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final PatientDao patientDao;
    private final ServiceItemDao serviceItemDao;
    private final DeviceDao deviceDao;
    private final ReportDao reportDao;
    private final StudyMapper studyMapper;

    public StudyServiceImpl(StudyDao studyDao, ImagingOrderDao imagingOrderDao, PatientDao patientDao,
                             ServiceItemDao serviceItemDao, DeviceDao deviceDao, ReportDao reportDao,
                             StudyMapper studyMapper) {
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.patientDao = patientDao;
        this.serviceItemDao = serviceItemDao;
        this.deviceDao = deviceDao;
        this.reportDao = reportDao;
        this.studyMapper = studyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
    public StudyPageResponse search(Long orderId, String status, Long deviceId,
                                     Integer page, Integer pageSize, String sort) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        SortSpec sortSpec = parseSort(sort);
        String normalizedStatus = parseStatus(status);

        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                orderId, normalizedStatus, deviceId, safePage, safePageSize, sortSpec.field(), sortSpec.ascending());
        StudyDao.SearchResult result = studyDao.search(criteria);

        List<StudyResponse> items = result.items().stream().map(studyMapper::toResponse).toList();
        return new StudyPageResponse(safePage, safePageSize, result.total(), items);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
    public StudyResponse getById(Long id) {
        return studyMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyResponse> listByOrderId(Long orderId) {
        return studyDao.findByOrderId(orderId).stream().map(studyMapper::toResponse).toList();
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.CREATE, entityType = "STUDY")
    public StudyResponse addToOrder(Long orderId, StudyCreateRequest request) {
        imagingOrderDao.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGING_ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh id=" + orderId));
        serviceItemDao.findById(request.serviceItemId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_SERVICE_ITEM_NOT_FOUND",
                        "Khong tim thay dich vu id=" + request.serviceItemId()));
        if (request.deviceId() != null) {
            deviceDao.findById(request.deviceId())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_DEVICE_NOT_FOUND",
                            "Khong tim thay may id=" + request.deviceId()));
        }

        LocalDate today = LocalDate.now();
        String prefix = AccessionNumberGenerator.datePrefix(today);
        long countToday = studyDao.countByAccessionNumberStartingWith(prefix);
        String accessionNumber = AccessionNumberGenerator.format(today, (int) countToday + 1);

        Study entity = studyMapper.toEntity(request);
        entity.setOrderId(orderId);
        entity.setAccessionNumber(accessionNumber);
        entity.setStatus(StudyStatus.SCHEDULED.name());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Study saved = studyDao.save(entity);
        return studyMapper.toResponse(saved);
    }

    @Override
    // KHONG readOnly: AuditLogAspect ghi mot dong audit_log action EXPORT (IN-15, doc/09 muc 2.2).
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = "STUDY")
    public ExportDocumentResponse exportCsv(Long orderId, String status, Long deviceId,
                                            String q, String patientCode, String fullName, String sort) {
        SortSpec sortSpec = parseSort(sort);
        String normalizedStatus = parseStatus(status);

        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                orderId, normalizedStatus, deviceId, 1, MAX_EXPORT_ROWS, sortSpec.field(), sortSpec.ascending());
        StudyDao.SearchResult result = studyDao.search(criteria);

        String normQ = normalizeFilter(q);
        String normCode = normalizeFilter(patientCode);
        String normName = normalizeFilter(fullName);

        List<PatientListCsvExporter.StudyWorklistRow> rows = new ArrayList<>();
        Set<Long> distinctPatientIds = new LinkedHashSet<>();

        for (Study study : result.items()) {
            ImagingOrder order = imagingOrderDao.findById(study.getOrderId()).orElse(null);
            Patient patient = (order == null || order.getPatientId() == null)
                    ? null
                    : patientDao.findById(order.getPatientId()).orElse(null);

            if (!matchesPatientFilters(study, order, patient, normQ, normCode, normName)) {
                continue;
            }

            if (patient != null && patient.getId() != null) {
                distinctPatientIds.add(patient.getId());
            }

            ServiceItem service = study.getServiceItemId() == null
                    ? null
                    : serviceItemDao.findById(study.getServiceItemId()).orElse(null);
            Device device = study.getDeviceId() == null
                    ? null
                    : deviceDao.findById(study.getDeviceId()).orElse(null);
            Report report = reportDao.findCurrentByStudyId(study.getId()).orElse(null);

            rows.add(new PatientListCsvExporter.StudyWorklistRow(
                    study.getAccessionNumber(),
                    study.getStatus(),
                    order == null ? null : order.getOrderCode(),
                    order == null ? null : order.getVisitCode(),
                    order == null ? null : order.getPatientClass(),
                    patient == null ? null : patient.getPatientCode(),
                    patient == null ? null : patient.getFullName(),
                    patient == null ? null : patient.getDateOfBirth(),
                    patient == null || patient.getDobPrecision() == null
                            ? DobPrecision.DAY
                            : DobPrecision.valueOf(patient.getDobPrecision()),
                    patient == null ? null : patient.getGender(),
                    patient == null ? null : patient.getSocialInsuranceNo(),
                    service == null ? null : service.getName(),
                    service == null ? null : service.getModality(),
                    service == null ? null : service.getBodyPart(),
                    device == null ? null : device.getName(),
                    order == null ? null : order.getOrderingDoctorName(),
                    order == null ? null : order.getClinicalDiagnosis(),
                    report == null ? null : report.getConclusion(),
                    study.getScheduledAt(),
                    study.getStartedAt(),
                    study.getCompletedAt()
            ));
        }

        Long singlePatientId = distinctPatientIds.size() == 1 ? distinctPatientIds.iterator().next() : null;
        LocalDate today = LocalDate.now();
        byte[] content = PatientListCsvExporter.exportStudies(rows, today);
        return new ExportDocumentResponse(
                PatientListCsvExporter.studyListFileName(today),
                "text/csv;charset=UTF-8",
                content,
                rows.size(),
                singlePatientId
        );
    }

    private boolean matchesPatientFilters(Study study, ImagingOrder order, Patient patient,
                                          String normQ, String normCode, String normName) {
        String pCode = patient == null || patient.getPatientCode() == null
                ? "" : patient.getPatientCode().toLowerCase(Locale.ROOT);
        String pName = patient == null || patient.getFullName() == null
                ? "" : patient.getFullName().toLowerCase(Locale.ROOT);
        String oCode = order == null || order.getOrderCode() == null
                ? "" : order.getOrderCode().toLowerCase(Locale.ROOT);
        String accNo = study.getAccessionNumber() == null
                ? "" : study.getAccessionNumber().toLowerCase(Locale.ROOT);

        if (normCode != null && !pCode.contains(normCode)) {
            return false;
        }
        if (normName != null && !pName.contains(normName)) {
            return false;
        }
        if (normQ != null) {
            return pCode.contains(normQ) || pName.contains(normQ)
                    || oCode.contains(normQ) || accNo.contains(normQ);
        }
        return true;
    }

    private String normalizeFilter(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private Study findOrThrow(Long id) {
        return studyDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND",
                        "Khong tim thay ca chup id=" + id));
    }

    private String parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StudyStatus.valueOf(status).name();
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'status'");
        }
    }

    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("scheduledAt", true);
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

    private record SortSpec(String field, boolean ascending) {
    }
}
