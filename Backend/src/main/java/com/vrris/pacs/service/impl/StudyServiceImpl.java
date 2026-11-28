package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.DailySequenceDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dao.StudyLookupDao;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.request.StudyUpdateRequest;
import com.vrris.pacs.dto.response.StatusCountsResponse;
import com.vrris.pacs.dto.response.StudyDetailResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.enu.UserRole;
import com.vrris.pacs.enu.WorklistTab;
import com.vrris.pacs.enu.WorklistTimeField;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.DeviceMapper;
import com.vrris.pacs.mapper.ImagingOrderMapper;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.mapper.ReportMapper;
import com.vrris.pacs.mapper.ServiceItemMapper;
import com.vrris.pacs.mapper.StudyMapper;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.vrris.pacs.service.impl.ServiceSupport.SortSpec;
import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.currentAuth;
import static com.vrris.pacs.service.impl.ServiceSupport.parseEnumOrNull;
import static com.vrris.pacs.service.impl.ServiceSupport.parseSort;
import static com.vrris.pacs.service.impl.ServiceSupport.safePage;
import static com.vrris.pacs.service.impl.ServiceSupport.safePageSize;
import static com.vrris.pacs.service.impl.ServiceSupport.toPatientResponseWithAge;
import static com.vrris.pacs.service.impl.StudyServiceSupport.findOrderOrThrow;
import static com.vrris.pacs.service.impl.StudyServiceSupport.findStudyOrThrow;
import static com.vrris.pacs.service.impl.StudyServiceSupport.hasAnyRole;
import static com.vrris.pacs.service.impl.StudyServiceSupport.parseStoredStatus;
import static com.vrris.pacs.service.impl.StudyServiceSupport.resolvePatientId;

/**
 * Worklist, chi tiet, deep-link, tao va sua ca chup. Chuyen trang thai o StudyTransitionServiceImpl,
 * xuat CSV o StudyExportServiceImpl (tach tu lop cu ~800 dong).
 */
@Service
public class StudyServiceImpl implements StudyService {

    static final Map<String, String> SORTABLE_FIELDS = Map.ofEntries(
            Map.entry("scheduled_at", "scheduledAt"),
            Map.entry("status", "status"),
            Map.entry("created_at", "createdAt"),
            Map.entry("completed_at", "completedAt"),
            Map.entry("patient_code", "patientCode"),
            Map.entry("full_name", "fullName"),
            Map.entry("order_code", "orderCode"),
            Map.entry("ordered_at", "orderedAt")
    );
    private static final WorklistTimeField DEFAULT_TIME_FIELD = WorklistTimeField.SCHEDULED;
    /** ❓ doc/03, doc/05 chua chot trang thai nao duoc PATCH - tam chan trang thai cuoi / da duyet. */
    private static final Set<StudyStatus> NON_EDITABLE_STATUSES = Set.of(StudyStatus.APPROVED, StudyStatus.CANCELLED);
    private static final String DEFAULT_OHIF_BASE_URL = "http://localhost:8043";

    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final ServiceItemDao serviceItemDao;
    private final DeviceDao deviceDao;
    private final DepartmentDao departmentDao;
    private final PatientDao patientDao;
    private final ReportDao reportDao;
    private final DailySequenceDao dailySequenceDao;
    private final StudyLookupDao studyLookupDao;
    private final StudyMapper studyMapper;
    private final ImagingOrderMapper imagingOrderMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final DeviceMapper deviceMapper;
    private final PatientMapper patientMapper;
    private final ReportMapper reportMapper;

    @Value("${app.ohif.base-url:" + DEFAULT_OHIF_BASE_URL + "}")
    private String ohifBaseUrl = DEFAULT_OHIF_BASE_URL;

    // Khởi tạo service với các DAO/mapper cần cho worklist, chi tiết ca chụp, tạo/sửa ca và deep-link OHIF.
    public StudyServiceImpl(StudyDao studyDao, ImagingOrderDao imagingOrderDao, ServiceItemDao serviceItemDao,
                            DeviceDao deviceDao, DepartmentDao departmentDao, PatientDao patientDao,
                            ReportDao reportDao, DailySequenceDao dailySequenceDao, StudyLookupDao studyLookupDao,
                            StudyMapper studyMapper, ImagingOrderMapper imagingOrderMapper,
                            ServiceItemMapper serviceItemMapper, DeviceMapper deviceMapper,
                            PatientMapper patientMapper, ReportMapper reportMapper) {
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.serviceItemDao = serviceItemDao;
        this.deviceDao = deviceDao;
        this.departmentDao = departmentDao;
        this.patientDao = patientDao;
        this.reportDao = reportDao;
        this.dailySequenceDao = dailySequenceDao;
        this.studyLookupDao = studyLookupDao;
        this.studyMapper = studyMapper;
        this.imagingOrderMapper = imagingOrderMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.deviceMapper = deviceMapper;
        this.patientMapper = patientMapper;
        this.reportMapper = reportMapper;
    }

    // Tra cứu danh sách ca chụp (worklist) theo nhiều điều kiện lọc (tab, trạng thái, khoa, máy, thời gian, bệnh nhân...), có phân trang và sắp xếp.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.STUDY)
    public StudyPageResponse search(String tab, List<String> status, List<Long> departmentId, List<Long> deviceId,
                                     String modality, String patientClass, String timeField, LocalDate dateFrom,
                                     LocalDate dateTo, String patientCode, String fullName, String visitCode,
                                     String orderCode, Integer page, Integer pageSize, String sort) {
        int resolvedPage = safePage(page);
        int resolvedPageSize = safePageSize(pageSize);
        SortSpec sortSpec = parseSort(sort, SORTABLE_FIELDS, "scheduledAt", true);
        NormalizedFilter f = normalizeCommonFilter(
                departmentId, deviceId, modality, patientClass, timeField,
                dateFrom, dateTo, patientCode, fullName, visitCode, orderCode);

        List<StudyStatus> statuses = resolveStatuses(parseEnumOrNull(tab, WorklistTab.class, "tab"), parseStatuses(status));
        if (statuses != null && statuses.isEmpty()) {
            // tab va status giao nhau rong (vi du tab=PENDING&status=READ): khong ca nao khop.
            return new StudyPageResponse(resolvedPage, resolvedPageSize, 0, List.of());
        }

        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                statuses,
                f.departmentIds(), f.deviceIds(), f.modality(), f.patientClass(),
                f.patientCode(), f.fullName(), f.visitCode(), f.orderCode(),
                f.timeField(), f.fromDateTime(), f.toDateTime(),
                resolvedPage, resolvedPageSize, sortSpec.field(), sortSpec.ascending());

        StudyDao.SearchResult result = studyDao.search(criteria);
        List<StudyResponse> items = result.items().stream().map(studyMapper::toResponse).toList();
        return new StudyPageResponse(resolvedPage, resolvedPageSize, result.total(), items);
    }

    // Đếm số ca chụp theo từng trạng thái, dùng cùng bộ điều kiện lọc như search.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.STUDY)
    public StatusCountsResponse getStatusCounts(List<Long> departmentId, List<Long> deviceId, String modality,
                                                 String patientClass, String timeField, LocalDate dateFrom,
                                                 LocalDate dateTo, String patientCode, String fullName,
                                                 String visitCode, String orderCode) {
        NormalizedFilter f = normalizeCommonFilter(
                departmentId, deviceId, modality, patientClass, timeField,
                dateFrom, dateTo, patientCode, fullName, visitCode, orderCode);

        StudyDao.StatusCountCriteria criteria = new StudyDao.StatusCountCriteria(
                f.departmentIds(), f.deviceIds(), f.modality(), f.patientClass(),
                f.patientCode(), f.fullName(), f.visitCode(), f.orderCode(),
                f.timeField(), f.fromDateTime(), f.toDateTime());

        Map<StudyStatus, Long> countsByStatus = studyDao.countByStatus(criteria);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (StudyStatus s : StudyStatus.values()) {
            byStatus.put(s.name(), countsByStatus.getOrDefault(s, 0L));
        }
        long total = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new StatusCountsResponse(total, byStatus);
    }

    // Lấy chi tiết đầy đủ một ca chụp (kèm bệnh nhân, chỉ định, dịch vụ, máy, báo cáo hiện hành).
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.STUDY, entityIdArg = 0)
    public StudyDetailResponse getDetailById(Long id) {
        Study study = findStudyOrThrow(studyDao, id);
        ImagingOrder order = findOrderOrThrow(imagingOrderDao, study.getOrderId());
        Patient patient = patientDao.findById(order.getPatientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND",
                        "Khong tim thay benh nhan id=" + order.getPatientId()));
        // Doc chi tiet ca = doc du lieu benh nhan: audit_log.patient_id phai co (nguyen tac bat bien).
        AuditContext.recordPatientId(patient.getId());
        ServiceItem serviceItem = serviceItemDao.findById(study.getServiceItemId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SERVICE_ITEM_NOT_FOUND",
                        "Khong tim thay dich vu id=" + study.getServiceItemId()));
        Device device = study.getDeviceId() == null ? null : deviceDao.findById(study.getDeviceId()).orElse(null);
        Report currentReport = reportDao.findCurrentByStudyId(id).orElse(null);

        StudyResponse base = studyMapper.toResponse(study);
        return new StudyDetailResponse(
                base.id(), base.accessionNumber(), base.orderId(), base.serviceItemId(),
                base.deviceId(), base.status(), base.studyInstanceUid(), base.scheduledAt(), base.startedAt(),
                base.completedAt(), base.dueAt(), base.performedByUserId(), base.readingUserId(),
                base.approvingUserId(), base.createdAt(), base.updatedAt(),
                toPatientResponseWithAge(patient, patientMapper),
                imagingOrderMapper.toResponse(order, listByOrderId(order.getId())),
                serviceItemMapper.toResponse(serviceItem),
                device == null ? null : deviceMapper.toResponse(device),
                currentReport == null ? null : reportMapper.toResponse(currentReport));
    }

    // Dựng deep-link mở ảnh ca chụp trên OHIF Viewer; báo lỗi 422 nếu ca chưa có ảnh trong PACS.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.VIEW_IMAGE, entityType = AuditEntityType.STUDY, entityIdArg = 0)
    public ViewerLinkResponse getViewerLink(Long id) {
        Study study = findStudyOrThrow(studyDao, id);
        if (study.getStudyInstanceUid() == null || study.getStudyInstanceUid().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_NO_DICOM_IMAGE",
                    "Ca chụp chưa có ảnh trong PACS (study_instance_uid is null)");
        }
        ImagingOrder order = findOrderOrThrow(imagingOrderDao, study.getOrderId());
        String base = (ohifBaseUrl != null && !ohifBaseUrl.isBlank()) ? ohifBaseUrl.trim() : DEFAULT_OHIF_BASE_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return new ViewerLinkResponse(
                study.getId(), order.getPatientId(), study.getStudyInstanceUid(),
                base + "/viewer/" + study.getStudyInstanceUid());
    }

    // Lấy danh sách ca chụp thuộc một chỉ định chụp.
    @Override
    @Transactional(readOnly = true)
    public List<StudyResponse> listByOrderId(Long orderId) {
        return studyDao.findByOrderId(orderId).stream().map(studyMapper::toResponse).toList();
    }

    // Tạo ca chụp mới gắn vào một chỉ định chụp (sinh accession_number nguyên tử qua daily_sequence).
    @Override
    @Transactional
    @Audited(action = AuditAction.CREATE, entityType = AuditEntityType.STUDY)
    public StudyResponse addToOrder(Long orderId, StudyCreateRequest request) {
        findOrderOrThrow(imagingOrderDao, orderId);
        validateServiceItemAndDevice(request.serviceItemId(), request.deviceId());

        LocalDateTime now = LocalDateTime.now();
        Study entity = studyMapper.toEntity(request);
        entity.setOrderId(orderId);
        entity.setAccessionNumber(nextAccessionNumber(now.toLocalDate()));
        entity.setStatus(StudyStatus.SCHEDULED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return studyMapper.toResponse(studyDao.save(entity));
    }

    // Cập nhật ca chụp (dịch vụ, máy, lịch chụp, study_instance_uid...); chặn sửa ca ở trạng thái không cho sửa (APPROVED/CANCELLED) và chặn gán study_instance_uid trùng.
    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = AuditEntityType.STUDY, entityIdArg = 0)
    public StudyResponse update(Long id, StudyUpdateRequest request) {
        Study study = findStudyOrThrow(studyDao, id);
        AuditContext.recordPatientId(resolvePatientId(imagingOrderDao, study));

        String newUid = request.studyInstanceUid();
        if (newUid != null && !hasAnyRole(currentAuth(), UserRole.ADMIN, UserRole.TECHNICIAN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Chi TECHNICIAN va ADMIN duoc dat study_instance_uid");
        }
        StudyStatus status = parseStoredStatus(study);
        if (NON_EDITABLE_STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDY_NOT_EDITABLE",
                    "Ca chup o trang thai " + status.name() + " khong duoc sua");
        }
        validateServiceItemAndDevice(request.serviceItemId(), request.deviceId());
        if (newUid != null && !newUid.equals(study.getStudyInstanceUid())
                && studyLookupDao.existsOtherStudyWithInstanceUid(newUid, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDY_INSTANCE_UID_DUPLICATE",
                    "study_instance_uid da duoc gan cho mot ca chup khac");
        }

        if (request.serviceItemId() != null) study.setServiceItemId(request.serviceItemId());
        if (request.deviceId() != null) study.setDeviceId(request.deviceId());
        if (request.scheduledAt() != null) study.setScheduledAt(request.scheduledAt());
        if (request.startedAt() != null) study.setStartedAt(request.startedAt());
        if (request.completedAt() != null) study.setCompletedAt(request.completedAt());
        if (newUid != null) study.setStudyInstanceUid(newUid);

        study.setUpdatedAt(LocalDateTime.now());
        return studyMapper.toResponse(studyDao.save(study));
    }

    /* ------------------------------------------------------------ Tien ich noi bo */

    /**
     * Cap accession_number nguyen tu qua bang daily_sequence (V9). Bo qua so da bi dung (vi du du
     * lieu nhap tay/seed tao sau khi migration khoi tao sequence) thay vi de UNIQUE vo -> 500.
     */
    // Cấp accession_number kế tiếp theo ngày, bỏ qua số đã bị dùng trước đó để tránh vi phạm UNIQUE.
    private String nextAccessionNumber(LocalDate date) {
        String key = AccessionNumberGenerator.sequenceKey(date);
        while (true) {
            int sequence = dailySequenceDao.next(key);
            if (sequence > AccessionNumberGenerator.MAX_SEQUENCE_PER_DAY) {
                throw new ApiException(HttpStatus.CONFLICT, "ACCESSION_SEQUENCE_EXHAUSTED",
                        "Da het so accession_number trong ngay " + date
                                + " (toi da " + AccessionNumberGenerator.MAX_SEQUENCE_PER_DAY + ")");
            }
            String candidate = AccessionNumberGenerator.format(date, sequence);
            if (studyDao.findByAccessionNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
    }

    private record NormalizedFilter(
            Set<Long> departmentIds,
            List<Long> deviceIds,
            Modality modality,
            PatientClass patientClass,
            WorklistTimeField timeField,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            String patientCode,
            String fullName,
            String visitCode,
            String orderCode
    ) {
    }

    // Chuẩn hóa các tham số lọc dùng chung (khoa mở rộng theo cây, modality, patient_class, khoảng thời gian, các chuỗi tìm kiếm) thành một đối tượng lọc duy nhất.
    private NormalizedFilter normalizeCommonFilter(
            List<Long> departmentId, List<Long> deviceId, String modality, String patientClass,
            String timeField, LocalDate dateFrom, LocalDate dateTo,
            String patientCode, String fullName, String visitCode, String orderCode) {
        return new NormalizedFilter(
                expandDepartmentIds(departmentId),
                deviceId,
                parseEnumOrNull(modality, Modality.class, "modality"),
                parseEnumOrNull(patientClass, PatientClass.class, "patient_class"),
                timeField == null || timeField.isBlank()
                        ? DEFAULT_TIME_FIELD : parseEnumOrNull(timeField, WorklistTimeField.class, "time_field"),
                dateFrom == null ? null : dateFrom.atStartOfDay(),
                dateTo == null ? null : dateTo.atTime(23, 59, 59),
                blankToNull(patientCode),
                blankToNull(fullName),
                blankToNull(visitCode),
                blankToNull(orderCode));
    }

    // Kiểm tra dịch vụ chụp và máy chụp (nếu có truyền) có tồn tại không, báo lỗi 422 nếu không tìm thấy.
    private void validateServiceItemAndDevice(Long serviceItemId, Long deviceId) {
        if (serviceItemId != null && serviceItemDao.findById(serviceItemId).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_SERVICE_ITEM_NOT_FOUND",
                    "Khong tim thay dich vu id=" + serviceItemId);
        }
        if (deviceId != null && deviceDao.findById(deviceId).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_DEVICE_NOT_FOUND",
                    "Khong tim thay may id=" + deviceId);
        }
    }


    // Parse danh sách trạng thái truyền vào thành danh sách enum StudyStatus hợp lệ.
    private List<StudyStatus> parseStatuses(List<String> rawStatuses) {
        if (rawStatuses == null || rawStatuses.isEmpty()) return List.of();
        List<StudyStatus> parsed = new ArrayList<>();
        for (String raw : rawStatuses) {
            if (raw != null && !raw.isBlank()) {
                parsed.add(parseEnumOrNull(raw, StudyStatus.class, "status"));
            }
        }
        return parsed;
    }

    /** null = khong loc theo trang thai; danh sach rong = khong trang thai nao khop (giao rong). */
    // Kết hợp bộ trạng thái theo tab (PENDING/DONE) với danh sách trạng thái người dùng chọn thêm; trả về phần giao.
    private List<StudyStatus> resolveStatuses(WorklistTab tab, List<StudyStatus> requestedStatuses) {
        Set<StudyStatus> tabStatuses = null;
        if (tab == WorklistTab.PENDING) {
            tabStatuses = EnumSet.of(StudyStatus.SCHEDULED);
        } else if (tab == WorklistTab.DONE) {
            tabStatuses = EnumSet.complementOf(EnumSet.of(StudyStatus.SCHEDULED));
        }

        if (requestedStatuses.isEmpty()) {
            return tabStatuses == null ? null : List.copyOf(tabStatuses);
        }
        if (tabStatuses == null) {
            return requestedStatuses;
        }
        List<StudyStatus> intersection = new ArrayList<>(requestedStatuses);
        intersection.retainAll(tabStatuses);
        return intersection;
    }

    // Mở rộng danh sách id khoa được chọn thành toàn bộ id khoa con trong cây, để lọc theo cả khoa cha và khoa con.
    private Set<Long> expandDepartmentIds(List<Long> departmentId) {
        if (departmentId == null || departmentId.isEmpty()) return Set.of();
        List<DepartmentTreeExpander.Node> nodes = departmentDao.findAll().stream()
                .map((Department d) -> new DepartmentTreeExpander.Node(d.getId(), d.getParentId()))
                .toList();
        return DepartmentTreeExpander.expand(nodes, departmentId);
    }

}
