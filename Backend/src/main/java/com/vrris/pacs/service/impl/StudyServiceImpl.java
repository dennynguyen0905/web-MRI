package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.StatusCountsResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.StudyMapper;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.rule.AccessionNumberGenerator;
import com.vrris.pacs.rule.DepartmentTreeExpander;
import com.vrris.pacs.service.StudyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IN-02/IN-03 (them ca chup vao chi dinh, GET /studies - xem Javadoc dao.StudyDao) + IN-15
 * (audit) qua audit.Audited, dong bo theo doc/15-kien-truc-backend.md muc 10.
 *
 * IN-07 (worklist): 2 tab (PENDING/DONE), bo loc khoa (ca nhanh con)/device/modality/patient_class/
 * khoang thoi gian theo time_field/tim theo benh nhan, phan trang + sap xep - da lam o search()
 * duoi day. Response van la StudyResponse (chua ghep patient/order/service_item/device/report
 * thanh WorklistRow day du - xem Javadoc StudyController).
 *
 * IN-09 (dem theo nhom trang thai) - da lam o getStatusCounts() duoi day, dung chung cac ham
 * chuan hoa filter voi search() (expandDepartmentIds/parseEnumOrNull/parseTimeField/blankToNull).
 * Chuyen trang thai (IN-06, rule.StudyStateMachine), bao cao (IN-10) van CHUA lam o day.
 */
@Service
public class StudyServiceImpl implements StudyService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.ofEntries(
            Map.entry("scheduled_at", "scheduledAt"),
            Map.entry("status", "status"),
            Map.entry("created_at", "createdAt"),
            Map.entry("completed_at", "completedAt"),
            Map.entry("patient_code", "patientCode"),
            Map.entry("full_name", "fullName"),
            Map.entry("order_code", "orderCode"),
            Map.entry("ordered_at", "orderedAt")
    );
    /** Khop 3 lua chon cua FE (Frontend/src/shared/lib/types.ts: TimeField) -> ten field entity dung de so sanh. */
    private static final Map<String, String> TIME_FIELDS = Map.of(
            "SCHEDULED", "scheduledAt",
            "ORDERED", "orderedAt",
            "COMPLETED", "completedAt"
    );
    private static final String DEFAULT_TIME_FIELD = "SCHEDULED";
    private static final String TAB_PENDING = "PENDING";
    private static final String TAB_DONE = "DONE";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final ServiceItemDao serviceItemDao;
    private final DeviceDao deviceDao;
    private final DepartmentDao departmentDao;
    private final StudyMapper studyMapper;

    @Value("${app.ohif.base-url:http://localhost:8043}")
    private String ohifBaseUrl = "http://localhost:8043";

    public StudyServiceImpl(StudyDao studyDao, ImagingOrderDao imagingOrderDao, ServiceItemDao serviceItemDao,
                             DeviceDao deviceDao, DepartmentDao departmentDao, StudyMapper studyMapper) {
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.serviceItemDao = serviceItemDao;
        this.deviceDao = deviceDao;
        this.departmentDao = departmentDao;
        this.studyMapper = studyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
    public StudyPageResponse search(String tab, List<String> status, List<Long> departmentId, List<Long> deviceId,
                                     String modality, String patientClass, String timeField, LocalDate dateFrom,
                                     LocalDate dateTo, String patientCode, String fullName, String visitCode,
                                     String orderCode, Integer page, Integer pageSize, String sort) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        SortSpec sortSpec = parseSort(sort);

        List<String> effectiveStatuses = resolveStatuses(parseTab(tab), parseStatuses(status));
        Set<Long> expandedDepartmentIds = expandDepartmentIds(departmentId);
        String normalizedModality = parseEnumOrNull(modality, Modality.class, "modality");
        String normalizedPatientClass = parseEnumOrNull(patientClass, PatientClass.class, "patient_class");
        String resolvedTimeField = parseTimeField(timeField);

        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                effectiveStatuses, expandedDepartmentIds, deviceId, normalizedModality, normalizedPatientClass,
                blankToNull(patientCode), blankToNull(fullName), blankToNull(visitCode), blankToNull(orderCode),
                resolvedTimeField, dateFrom == null ? null : dateFrom.atStartOfDay(),
                dateTo == null ? null : dateTo.atTime(23, 59, 59),
                safePage, safePageSize, sortSpec.field(), sortSpec.ascending());
        StudyDao.SearchResult result = studyDao.search(criteria);

        List<StudyResponse> items = result.items().stream().map(studyMapper::toResponse).toList();
        return new StudyPageResponse(safePage, safePageSize, result.total(), items);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
    public StatusCountsResponse getStatusCounts(List<Long> departmentId, List<Long> deviceId, String modality,
                                                 String patientClass, String timeField, LocalDate dateFrom,
                                                 LocalDate dateTo, String patientCode, String fullName,
                                                 String visitCode, String orderCode) {
        Set<Long> expandedDepartmentIds = expandDepartmentIds(departmentId);
        String normalizedModality = parseEnumOrNull(modality, Modality.class, "modality");
        String normalizedPatientClass = parseEnumOrNull(patientClass, PatientClass.class, "patient_class");
        String resolvedTimeField = parseTimeField(timeField);

        StudyDao.StatusCountCriteria criteria = new StudyDao.StatusCountCriteria(
                expandedDepartmentIds, deviceId, normalizedModality, normalizedPatientClass,
                blankToNull(patientCode), blankToNull(fullName), blankToNull(visitCode), blankToNull(orderCode),
                resolvedTimeField, dateFrom == null ? null : dateFrom.atStartOfDay(),
                dateTo == null ? null : dateTo.atTime(23, 59, 59));
        Map<String, Long> countsByStatus = studyDao.countByStatus(criteria);

        // Dien du 0 cho moi gia tri enu.StudyStatus (ke ca khong co ca nao khop) - khop openapi.yaml
        // "trạng thái không có ca thì trả 0". total cong tat ca gia tri DB tra ve (khong chi gioi
        // han trong enu.StudyStatus) de luon dung voi so ca thuc te khop bo loc, phong truong hop DB
        // co gia tri status la (status la VARCHAR, khong rang buoc ENUM - xem doc/02-erd.md 4.6).
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (StudyStatus s : StudyStatus.values()) {
            byStatus.put(s.name(), countsByStatus.getOrDefault(s.name(), 0L));
        }
        long total = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new StatusCountsResponse(total, byStatus);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String parseTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return null;
        }
        if (!tab.equals(TAB_PENDING) && !tab.equals(TAB_DONE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'tab'");
        }
        return tab;
    }

    private List<String> parseStatuses(List<String> rawStatuses) {
        if (rawStatuses == null || rawStatuses.isEmpty()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String raw : rawStatuses) {
            if (raw != null && !raw.isBlank()) {
                parsed.add(parseStatus(raw));
            }
        }
        return parsed;
    }

    /**
     * Ket hop tab (PENDING = chi SCHEDULED, DONE = cac trang thai con lai - 03-state-machine.md
     * bang 2.1) voi status[] cua sidebar bang AND: tab thu hep truoc, status[] loc tiep trong do.
     * Tra null = khong loc gi (ca hai tab, dung cho "cac ca khac cua cung benh nhan").
     */
    private List<String> resolveStatuses(String tab, List<String> requestedStatuses) {
        Set<String> tabStatuses = null;
        if (TAB_PENDING.equals(tab)) {
            tabStatuses = Set.of(StudyStatus.SCHEDULED.name());
        } else if (TAB_DONE.equals(tab)) {
            tabStatuses = new LinkedHashSet<>();
            for (StudyStatus s : StudyStatus.values()) {
                if (s != StudyStatus.SCHEDULED) {
                    tabStatuses.add(s.name());
                }
            }
        }

        if (requestedStatuses.isEmpty()) {
            return tabStatuses == null ? null : List.copyOf(tabStatuses);
        }
        if (tabStatuses == null) {
            return requestedStatuses;
        }
        List<String> intersection = new ArrayList<>(requestedStatuses);
        intersection.retainAll(tabStatuses);
        return intersection;
    }

    private Set<Long> expandDepartmentIds(List<Long> departmentId) {
        if (departmentId == null || departmentId.isEmpty()) {
            return Set.of();
        }
        List<DepartmentTreeExpander.Node> nodes = departmentDao.findAll().stream()
                .map((Department d) -> new DepartmentTreeExpander.Node(d.getId(), d.getParentId()))
                .toList();
        return DepartmentTreeExpander.expand(nodes, departmentId);
    }

    private <E extends Enum<E>> String parseEnumOrNull(String raw, Class<E> enumType, String paramName) {
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

    private String parseTimeField(String timeField) {
        String key = (timeField == null || timeField.isBlank()) ? DEFAULT_TIME_FIELD : timeField;
        String resolved = TIME_FIELDS.get(key);
        if (resolved == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'time_field'");
        }
        return resolved;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
    public StudyResponse getById(Long id) {
        return studyMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.VIEW_IMAGE, entityType = "STUDY", entityIdArg = 0)
    public ViewerLinkResponse getViewerLink(Long id) {
        Study study = findOrThrow(id);
        if (study.getStudyInstanceUid() == null || study.getStudyInstanceUid().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_NO_DICOM_IMAGE",
                    "Ca chụp chưa có ảnh trong PACS (study_instance_uid is null)");
        }
        ImagingOrder order = imagingOrderDao.findById(study.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGING_ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh id=" + study.getOrderId()));
        String base = (ohifBaseUrl != null && !ohifBaseUrl.isBlank()) ? ohifBaseUrl.trim() : "http://localhost:8043";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // UID phai nam trong PATH ("/viewer/<uid>"), KHONG phai query string ("?StudyInstanceUIDs=").
        // Ban OHIF dang chay (ohif/viewer:v4.12.51.21579) dung route cu "ConnectedStandaloneRouting"
        // ung voi schema cau hinh "servers.dicomWeb", chi khop path "/viewer/:studyInstanceUids";
        // dang "?StudyInstanceUIDs=" roi ve route khac va bao loi "No URL was specified" (phat hien
        // bang tay 2026-11-09, xem doc/07-pacs-dicom.md).
        String viewerUrl = base + "/viewer/" + study.getStudyInstanceUid();
        return new ViewerLinkResponse(study.getId(), order.getPatientId(), study.getStudyInstanceUid(), viewerUrl);
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
