package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dao.StudyStatusHistoryDao;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.request.StudyUpdateRequest;
import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.dto.response.ImagingOrderResponse;
import com.vrris.pacs.dto.response.ReportResponse;
import com.vrris.pacs.dto.response.StatusCountsResponse;
import com.vrris.pacs.dto.response.StatusHistoryEntry;
import com.vrris.pacs.dto.response.StudyDetailResponse;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.TransitionResponse;
import com.vrris.pacs.dto.response.UserRefResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.DeviceMapper;
import com.vrris.pacs.mapper.ImagingOrderMapper;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.mapper.ReportMapper;
import com.vrris.pacs.mapper.ServiceItemMapper;
import com.vrris.pacs.mapper.StudyMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.model.StudyStatusHistory;
import com.vrris.pacs.rule.AccessionNumberGenerator;
import com.vrris.pacs.rule.DepartmentTreeExpander;
import com.vrris.pacs.rule.PatientListCsvExporter;
import com.vrris.pacs.rule.StudyStateMachine;
import com.vrris.pacs.service.StudyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.vrris.pacs.service.impl.ServiceSupport.MAX_EXPORT_ROWS;
import static com.vrris.pacs.service.impl.ServiceSupport.SortSpec;
import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.currentAuth;
import static com.vrris.pacs.service.impl.ServiceSupport.hasAnyAuthority;
import static com.vrris.pacs.service.impl.ServiceSupport.hasAuthority;
import static com.vrris.pacs.service.impl.ServiceSupport.normalizeLower;
import static com.vrris.pacs.service.impl.ServiceSupport.parseEnumOrNull;
import static com.vrris.pacs.service.impl.ServiceSupport.parseSort;
import static com.vrris.pacs.service.impl.ServiceSupport.safePage;
import static com.vrris.pacs.service.impl.ServiceSupport.safePageSize;
import static com.vrris.pacs.service.impl.ServiceSupport.toPatientResponseWithAge;

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
    private static final Map<String, String> TIME_FIELDS = Map.of(
            "SCHEDULED", "scheduledAt",
            "ORDERED", "orderedAt",
            "COMPLETED", "completedAt"
    );
    private static final String DEFAULT_TIME_FIELD = "SCHEDULED";
    private static final String TAB_PENDING = "PENDING";
    private static final String TAB_DONE = "DONE";

    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final ServiceItemDao serviceItemDao;
    private final DeviceDao deviceDao;
    private final DepartmentDao departmentDao;
    private final PatientDao patientDao;
    private final ReportDao reportDao;
    private final AppUserDao appUserDao;
    private final StudyStatusHistoryDao studyStatusHistoryDao;
    private final StudyMapper studyMapper;
    private final ImagingOrderMapper imagingOrderMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final DeviceMapper deviceMapper;
    private final PatientMapper patientMapper;
    private final ReportMapper reportMapper;

    @Value("${app.ohif.base-url:http://localhost:8043}")
    private String ohifBaseUrl = "http://localhost:8043";

    public StudyServiceImpl(StudyDao studyDao, ImagingOrderDao imagingOrderDao, ServiceItemDao serviceItemDao,
                            DeviceDao deviceDao, DepartmentDao departmentDao, PatientDao patientDao,
                            ReportDao reportDao, AppUserDao appUserDao, StudyStatusHistoryDao studyStatusHistoryDao,
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
        this.appUserDao = appUserDao;
        this.studyStatusHistoryDao = studyStatusHistoryDao;
        this.studyMapper = studyMapper;
        this.imagingOrderMapper = imagingOrderMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.deviceMapper = deviceMapper;
        this.patientMapper = patientMapper;
        this.reportMapper = reportMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
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

        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                resolveStatuses(parseTab(tab), parseStatuses(status)),
                f.departmentIds(), f.deviceIds(), f.modality(), f.patientClass(),
                f.patientCode(), f.fullName(), f.visitCode(), f.orderCode(),
                f.timeField(), f.fromDateTime(), f.toDateTime(),
                resolvedPage, resolvedPageSize, sortSpec.field(), sortSpec.ascending());

        StudyDao.SearchResult result = studyDao.search(criteria);
        List<StudyResponse> items = result.items().stream().map(studyMapper::toResponse).toList();
        return new StudyPageResponse(resolvedPage, resolvedPageSize, result.total(), items);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
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

        Map<String, Long> countsByStatus = studyDao.countByStatus(criteria);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (StudyStatus s : StudyStatus.values()) {
            byStatus.put(s.name(), countsByStatus.getOrDefault(s.name(), 0L));
        }
        long total = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new StatusCountsResponse(total, byStatus);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY")
    public StudyDetailResponse getDetailById(Long id) {
        Study study = findOrThrow(id);
        ImagingOrder order = imagingOrderDao.findById(study.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGING_ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh id=" + study.getOrderId()));
        Patient patient = patientDao.findById(order.getPatientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND",
                        "Khong tim thay benh nhan id=" + order.getPatientId()));
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
                toImagingOrderResponse(order),
                serviceItemMapper.toResponse(serviceItem),
                device == null ? null : deviceMapper.toResponse(device),
                currentReport == null ? null : reportMapper.toResponse(currentReport));
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
        return new ViewerLinkResponse(
                study.getId(), order.getPatientId(), study.getStudyInstanceUid(),
                base + "/viewer/" + study.getStudyInstanceUid());
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
        validateServiceItemAndDevice(request.serviceItemId(), request.deviceId());

        LocalDate today = LocalDate.now();
        long countToday = studyDao.countByAccessionNumberStartingWith(AccessionNumberGenerator.datePrefix(today));
        LocalDateTime now = LocalDateTime.now();

        Study entity = studyMapper.toEntity(request);
        entity.setOrderId(orderId);
        entity.setAccessionNumber(AccessionNumberGenerator.format(today, (int) countToday + 1));
        entity.setStatus(StudyStatus.SCHEDULED.name());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return studyMapper.toResponse(studyDao.save(entity));
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = "STUDY")
    public ExportDocumentResponse exportCsv(Long orderId, String status, Long deviceId,
                                            String q, String patientCode, String fullName, String sort) {
        SortSpec sortSpec = parseSort(sort, SORTABLE_FIELDS, "scheduledAt", true);
        String normalizedStatus = parseStatus(status);
        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                normalizedStatus == null ? null : List.of(normalizedStatus),
                null,
                deviceId == null ? null : List.of(deviceId),
                null, null,
                blankToNull(patientCode), blankToNull(fullName), null, null,
                DEFAULT_TIME_FIELD, null, null,
                1, MAX_EXPORT_ROWS, sortSpec.field(), sortSpec.ascending());

        StudyDao.SearchResult result = studyDao.search(criteria);
        String normQ = normalizeLower(q);
        String normCode = normalizeLower(patientCode);
        String normName = normalizeLower(fullName);

        List<PatientListCsvExporter.StudyWorklistRow> rows = new ArrayList<>();
        Set<Long> distinctPatientIds = new LinkedHashSet<>();

        for (Study study : result.items()) {
            if (orderId != null && !orderId.equals(study.getOrderId())) {
                continue;
            }
            ImagingOrder order = imagingOrderDao.findById(study.getOrderId()).orElse(null);
            Patient patient = (order == null || order.getPatientId() == null)
                    ? null : patientDao.findById(order.getPatientId()).orElse(null);
            if (!matchesPatientFilters(study, order, patient, normQ, normCode, normName)) {
                continue;
            }
            if (patient != null && patient.getId() != null) {
                distinctPatientIds.add(patient.getId());
            }
            rows.add(toCsvWorklistRow(study, order, patient));
        }

        Long singlePatientId = distinctPatientIds.size() == 1 ? distinctPatientIds.iterator().next() : null;
        LocalDate today = LocalDate.now();
        return new ExportDocumentResponse(
                PatientListCsvExporter.studyListFileName(today),
                "text/csv;charset=UTF-8",
                PatientListCsvExporter.exportStudies(rows, today),
                rows.size(),
                singlePatientId);
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = "STUDY")
    public StudyResponse update(Long id, StudyUpdateRequest request) {
        Study study = findOrThrow(id);
        AuditContext.recordPatientId(resolvePatientId(study));

        if (request.studyInstanceUid() != null && !hasAnyAuthority(currentAuth(), "ROLE_ADMIN", "ROLE_TECHNICIAN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Chi TECHNICIAN va ADMIN duoc dat study_instance_uid");
        }
        validateServiceItemAndDevice(request.serviceItemId(), request.deviceId());

        if (request.serviceItemId() != null) study.setServiceItemId(request.serviceItemId());
        if (request.deviceId() != null) study.setDeviceId(request.deviceId());
        if (request.scheduledAt() != null) study.setScheduledAt(request.scheduledAt());
        if (request.startedAt() != null) study.setStartedAt(request.startedAt());
        if (request.completedAt() != null) study.setCompletedAt(request.completedAt());
        if (request.studyInstanceUid() != null) study.setStudyInstanceUid(request.studyInstanceUid());

        study.setUpdatedAt(LocalDateTime.now());
        return studyMapper.toResponse(studyDao.save(study));
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.STATUS_CHANGE, entityType = "STUDY")
    public TransitionResponse transition(Long id, TransitionRequest request) {
        Study study = findOrThrow(id);
        AuditContext.recordPatientId(resolvePatientId(study));

        Authentication auth = currentAuth();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chua dang nhap");
        }
        AppUser caller = appUserDao.findByUsername(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Nguoi dung khong ton tai"));
        Long callerId = caller.getId();

        boolean isAdmin = hasAuthority(auth, "ROLE_ADMIN");
        boolean isTech = hasAuthority(auth, "ROLE_TECHNICIAN");
        boolean isRadiologist = hasAuthority(auth, "ROLE_RADIOLOGIST");
        boolean isApprover = hasAuthority(auth, "ROLE_APPROVER");
        boolean isClinician = hasAuthority(auth, "ROLE_CLINICIAN");

        if (isClinician && !isAdmin && !isTech && !isRadiologist && !isApprover) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "CLINICIAN khong co quyen chuyen trang thai ca chup");
        }

        String fromStatus = study.getStatus();
        StudyStatus fromEnum = StudyStatus.valueOf(fromStatus);
        StudyStatus toEnum = request.toStatus();
        String transitionCode = StudyStateMachine.resolveTransitionCode(fromEnum, toEnum);
        if (transitionCode == null) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Khong cho phep chuyen trang thai tu " + fromStatus + " sang " + toEnum.name());
        }

        Report currentReport = reportDao.findCurrentByStudyId(id).orElse(null);

        switch (transitionCode) {
            case "T2" -> applyT2(study, isAdmin, isTech);
            case "T3" -> currentReport = applyT3(study, currentReport, callerId, isRadiologist);
            case "T4" -> applyT4(study, request.reason(), callerId, isAdmin, isRadiologist);
            case "T5" -> currentReport = applyT5(study, currentReport, callerId, isRadiologist);
            case "T6" -> applyT6(study, currentReport, callerId, isApprover);
            case "T7" -> currentReport = applyT7(study, currentReport, callerId, isAdmin, isApprover);
            case "T8" -> currentReport = applyT8(study, currentReport, request.reason(), isApprover);
            case "T9" -> currentReport = applyT9(study, currentReport, request.reason(), callerId, isAdmin, isApprover);
            case "T10" -> applyT10(study, request.reason(), isAdmin, isTech);
            default -> throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Khong cho phep chuyen trang thai tu " + fromStatus + " sang " + toEnum.name());
        }

        study.setUpdatedAt(LocalDateTime.now());
        Study savedStudy = studyDao.save(study);

        StudyStatusHistory history = new StudyStatusHistory();
        history.setStudyId(id);
        history.setFromStatus(fromStatus);
        history.setToStatus(toEnum.name());
        history.setChangedByUserId(callerId);
        history.setChangedAt(LocalDateTime.now());
        history.setReason(blankToNull(request.reason()));
        history.setNote(blankToNull(request.note()));
        StudyStatusHistory savedHistory = studyStatusHistoryDao.save(history);

        StatusHistoryEntry entry = toStatusHistoryEntry(savedHistory, caller, transitionCode);
        ReportResponse reportRes = currentReport == null ? null : reportMapper.toResponse(currentReport);
        return new TransitionResponse(studyMapper.toResponse(savedStudy), entry, reportRes);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "STUDY", entityIdArg = 0)
    public List<StatusHistoryEntry> listStatusHistory(Long studyId) {
        Study study = findOrThrow(studyId);
        AuditContext.recordPatientId(resolvePatientId(study));

        return studyStatusHistoryDao.findByStudyIdOrderByChangedAtAsc(studyId).stream()
                .map(h -> {
                    AppUser user = h.getChangedByUserId() == null
                            ? null : appUserDao.findById(h.getChangedByUserId()).orElse(null);
                    return toStatusHistoryEntry(h, user, resolveHistoryTransitionCode(h));
                })
                .toList();
    }

    /* ----------------------------------------- Cac buoc chuyen trang thai T2..T10 */

    private void applyT2(Study study, boolean isAdmin, boolean isTech) {
        requireRole(isAdmin || isTech, "Chi TECHNICIAN va ADMIN duoc chuyen SCHEDULED -> UNREAD");
        if (study.getStudyInstanceUid() == null || study.getStudyInstanceUid().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "STUDY_NO_DICOM_IMAGE",
                    "Chua co study_instance_uid, khong the chuyen sang UNREAD");
        }
        study.setStatus(StudyStatus.UNREAD.name());
    }

    private Report applyT3(Study study, Report currentReport, Long callerId, boolean isRadiologist) {
        requireRole(isRadiologist, "Chi RADIOLOGIST duoc mo ca de doc");
        if (study.getReadingUserId() != null && !study.getReadingUserId().equals(callerId)) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDY_LOCKED_BY_ANOTHER",
                    "Ca chup dang duoc mo doc boi bac si khac");
        }
        study.setStatus(StudyStatus.READING.name());
        study.setReadingUserId(callerId);
        if (study.getStartedAt() == null) {
            study.setStartedAt(LocalDateTime.now());
        }
        if (currentReport == null) {
            return reportDao.save(createDraftReport(study.getId(), 1, null, callerId));
        }
        if ("DRAFT".equals(currentReport.getStatus())) {
            currentReport.setCreatedByUserId(callerId);
            return reportDao.save(currentReport);
        }
        return currentReport;
    }

    private void applyT4(Study study, String reason, Long callerId, boolean isAdmin, boolean isRadiologist) {
        if (!isAdmin) {
            requireRole(isRadiologist, "Khong co quyen giai phong khoa hoac bo do ca chup");
            requireReadingHolder(study, callerId, "Chi nguoi dang giu ca duoc bo do");
        }
        requireReason(reason, "T4 (bo do) bat buoc co ly do");
        study.setStatus(StudyStatus.UNREAD.name());
        study.setReadingUserId(null);
    }

    private Report applyT5(Study study, Report currentReport, Long callerId, boolean isRadiologist) {
        requireRole(isRadiologist, "Chi RADIOLOGIST duoc chot doc ca chup");
        requireReadingHolder(study, callerId, "Chi nguoi dang giu ca duoc chot doc");
        if (currentReport == null
                || blankToNull(currentReport.getTechnique()) == null
                || blankToNull(currentReport.getConclusion()) == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REPORT_INCOMPLETE",
                    "Ky thuat va ket luan khong duoc de trong khi chot doc");
        }
        study.setStatus(StudyStatus.READ.name());
        currentReport.setStatus("FINAL");
        return reportDao.save(currentReport);
    }

    private void applyT6(Study study, Report currentReport, Long callerId, boolean isApprover) {
        requireRole(isApprover, "Chi APPROVER duoc mo ca de duyet");
        requireApproverNotReader(study, currentReport, callerId);
        study.setStatus(StudyStatus.APPROVING.name());
        study.setApprovingUserId(callerId);
    }

    private Report applyT7(Study study, Report currentReport, Long callerId, boolean isAdmin, boolean isApprover) {
        if (isAdmin && !isApprover) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_CANNOT_APPROVE", "ADMIN khong duoc duyet bao cao");
        }
        requireRole(isApprover, "Chi APPROVER duoc duyet ca chup");
        if (study.getApprovingUserId() == null || !study.getApprovingUserId().equals(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_APPROVING_HOLDER",
                    "Chi nguoi da mo duyet duoc duyet ca chup nay");
        }
        requireApproverNotReader(study, currentReport, callerId);

        LocalDateTime now = LocalDateTime.now();
        study.setStatus(StudyStatus.APPROVED.name());
        study.setCompletedAt(now);
        if (currentReport != null) {
            currentReport.setStatus("APPROVED");
            currentReport.setApprovedByUserId(callerId);
            currentReport.setApprovedAt(now);
            return reportDao.save(currentReport);
        }
        return null;
    }

    private Report applyT8(Study study, Report currentReport, String reason, boolean isApprover) {
        requireRole(isApprover, "Chi APPROVER duoc tra lai ca chup de sua");
        requireReason(reason, "T8 (tra lai) bat buoc co ly do");
        study.setStatus(StudyStatus.READING.name());
        study.setApprovingUserId(null);
        if (currentReport != null) {
            currentReport.setStatus("RETURNED");
            return reportDao.save(currentReport);
        }
        return null;
    }

    private Report applyT9(Study study, Report currentReport, String reason,
                           Long callerId, boolean isAdmin, boolean isApprover) {
        requireRole(isAdmin || isApprover, "Chi ADMIN hoac APPROVER duoc mo lai ca da duyet");
        requireReason(reason, "T9 (mo lai ca da duyet) bat buoc co ly do");
        study.setStatus(StudyStatus.READING.name());
        study.setApprovingUserId(null);
        if (currentReport == null) {
            return null;
        }
        Long authorId = study.getReadingUserId() != null ? study.getReadingUserId() : callerId;
        return reportDao.save(createDraftReport(study.getId(), currentReport.getVersion() + 1, currentReport, authorId));
    }

    private void applyT10(Study study, String reason, boolean isAdmin, boolean isTech) {
        requireRole(isAdmin || isTech, "Chi ADMIN va TECHNICIAN duoc huy ca chup");
        if (study.getStudyInstanceUid() != null && !study.getStudyInstanceUid().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CANNOT_CANCEL_STUDY_WITH_IMAGE",
                    "Khong the huy ca chup da co anh trong PACS (study_instance_uid khong null)");
        }
        requireReason(reason, "T10 (huy ca chup) bat buoc co ly do");
        study.setStatus(StudyStatus.CANCELLED.name());
    }

    /* ------------------------------------------------------------ Tien ich noi bo */

    private record NormalizedFilter(
            Set<Long> departmentIds,
            List<Long> deviceIds,
            String modality,
            String patientClass,
            String timeField,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            String patientCode,
            String fullName,
            String visitCode,
            String orderCode
    ) {
    }

    private NormalizedFilter normalizeCommonFilter(
            List<Long> departmentId, List<Long> deviceId, String modality, String patientClass,
            String timeField, LocalDate dateFrom, LocalDate dateTo,
            String patientCode, String fullName, String visitCode, String orderCode) {
        return new NormalizedFilter(
                expandDepartmentIds(departmentId),
                deviceId,
                parseEnumOrNull(modality, Modality.class, "modality"),
                parseEnumOrNull(patientClass, PatientClass.class, "patient_class"),
                parseTimeField(timeField),
                dateFrom == null ? null : dateFrom.atStartOfDay(),
                dateTo == null ? null : dateTo.atTime(23, 59, 59),
                blankToNull(patientCode),
                blankToNull(fullName),
                blankToNull(visitCode),
                blankToNull(orderCode));
    }

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

    private Report createDraftReport(Long studyId, int version, Report template, Long createdByUserId) {
        Report r = new Report();
        r.setStudyId(studyId);
        r.setVersion(version);
        r.setStatus("DRAFT");
        r.setOrigin(template == null ? "MANUAL" : template.getOrigin());
        if (template != null) {
            r.setTechnique(template.getTechnique());
            r.setFindings(template.getFindings());
            r.setDiagnosis(template.getDiagnosis());
            r.setConclusion(template.getConclusion());
            r.setRecommendation(template.getRecommendation());
        }
        r.setCreatedByUserId(createdByUserId);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private StatusHistoryEntry toStatusHistoryEntry(StudyStatusHistory h, AppUser user, String transitionCode) {
        String username = user == null ? null : user.getUsername();
        UserRefResponse changedBy = user != null
                ? new UserRefResponse(user.getId(), user.getUsername(), user.getFullName())
                : new UserRefResponse(h.getChangedByUserId(), null,
                        h.getChangedByUserId() == null ? "Hệ thống" : "Người dùng #" + h.getChangedByUserId());
        return new StatusHistoryEntry(
                h.getId(), h.getStudyId(), h.getFromStatus(), h.getToStatus(),
                transitionCode, h.getChangedByUserId(), username, changedBy,
                h.getChangedAt(), h.getReason(), h.getNote());
    }

    private String resolveHistoryTransitionCode(StudyStatusHistory h) {
        try {
            if (h.getFromStatus() != null && h.getToStatus() != null) {
                return StudyStateMachine.resolveTransitionCode(
                        StudyStatus.valueOf(h.getFromStatus()),
                        StudyStatus.valueOf(h.getToStatus()));
            }
            if (h.getFromStatus() == null && "SCHEDULED".equals(h.getToStatus())) {
                return "T1";
            }
        } catch (IllegalArgumentException ignored) {
            // Bỏ qua nếu giá trị trạng thái lịch sử không thuộc enum
        }
        return null;
    }

    private void requireRole(boolean allowed, String message) {
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
        }
    }

    private void requireReadingHolder(Study study, Long callerId, String message) {
        if (study.getReadingUserId() == null || !study.getReadingUserId().equals(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_NOT_HOLDER", message);
        }
    }

    private void requireApproverNotReader(Study study, Report currentReport, Long callerId) {
        if (callerId.equals(study.getReadingUserId())
                || (currentReport != null && callerId.equals(currentReport.getCreatedByUserId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "APPROVER_CANNOT_BE_READER",
                    "Nguoi duyet khong duoc la nguoi doc bao cao");
        }
    }

    private void requireReason(String reason, String message) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSITION_REASON_REQUIRED", message);
        }
    }

    private ImagingOrderResponse toImagingOrderResponse(ImagingOrder order) {
        ImagingOrderResponse base = imagingOrderMapper.toResponse(order);
        return new ImagingOrderResponse(
                base.id(), base.orderCode(), base.patientId(), base.visitCode(),
                base.source(), base.hisOrderId(), base.syncStatus(), base.orderingDepartmentId(),
                base.orderingDoctorName(), base.patientClass(), base.room(), base.bed(),
                base.clinicalDiagnosis(), base.orderedAt(), listByOrderId(order.getId()),
                base.createdAt(), base.updatedAt());
    }

    private PatientListCsvExporter.StudyWorklistRow toCsvWorklistRow(Study study, ImagingOrder order, Patient patient) {
        ServiceItem service = study.getServiceItemId() == null
                ? null : serviceItemDao.findById(study.getServiceItemId()).orElse(null);
        Device device = study.getDeviceId() == null
                ? null : deviceDao.findById(study.getDeviceId()).orElse(null);
        Report report = reportDao.findCurrentByStudyId(study.getId()).orElse(null);

        return new PatientListCsvExporter.StudyWorklistRow(
                study.getAccessionNumber(),
                study.getStatus(),
                order == null ? null : order.getOrderCode(),
                order == null ? null : order.getVisitCode(),
                order == null ? null : order.getPatientClass(),
                patient == null ? null : patient.getPatientCode(),
                patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getDateOfBirth(),
                patient == null || patient.getDobPrecision() == null
                        ? DobPrecision.DAY : DobPrecision.valueOf(patient.getDobPrecision()),
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
                study.getCompletedAt());
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

        if (normCode != null && !pCode.contains(normCode)) return false;
        if (normName != null && !pName.contains(normName)) return false;
        return normQ == null || pCode.contains(normQ) || pName.contains(normQ)
                || oCode.contains(normQ) || accNo.contains(normQ);
    }

    private String parseTab(String tab) {
        if (tab == null || tab.isBlank()) return null;
        if (!tab.equals(TAB_PENDING) && !tab.equals(TAB_DONE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Gia tri khong hop le cho tham so 'tab'");
        }
        return tab;
    }

    private List<String> parseStatuses(List<String> rawStatuses) {
        if (rawStatuses == null || rawStatuses.isEmpty()) return List.of();
        List<String> parsed = new ArrayList<>();
        for (String raw : rawStatuses) {
            if (raw != null && !raw.isBlank()) {
                parsed.add(parseStatus(raw));
            }
        }
        return parsed;
    }

    private List<String> resolveStatuses(String tab, List<String> requestedStatuses) {
        Set<String> tabStatuses = null;
        if (TAB_PENDING.equals(tab)) {
            tabStatuses = Set.of(StudyStatus.SCHEDULED.name());
        } else if (TAB_DONE.equals(tab)) {
            tabStatuses = new LinkedHashSet<>();
            Arrays.stream(StudyStatus.values())
                    .filter(s -> s != StudyStatus.SCHEDULED)
                    .forEach(s -> tabStatuses.add(s.name()));
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
        if (departmentId == null || departmentId.isEmpty()) return Set.of();
        List<DepartmentTreeExpander.Node> nodes = departmentDao.findAll().stream()
                .map((Department d) -> new DepartmentTreeExpander.Node(d.getId(), d.getParentId()))
                .toList();
        return DepartmentTreeExpander.expand(nodes, departmentId);
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

    private String parseStatus(String status) {
        return parseEnumOrNull(status, StudyStatus.class, "status");
    }

    private Study findOrThrow(Long id) {
        return studyDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND",
                        "Khong tim thay ca chup id=" + id));
    }

    private Long resolvePatientId(Study study) {
        if (study == null || study.getOrderId() == null) return null;
        return imagingOrderDao.findById(study.getOrderId())
                .map(ImagingOrder::getPatientId)
                .orElse(null);
    }
}
