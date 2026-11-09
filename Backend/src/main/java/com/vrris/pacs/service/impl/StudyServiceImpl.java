package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.StudyPageResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.dto.response.ViewerLinkResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.StudyMapper;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.rule.AccessionNumberGenerator;
import com.vrris.pacs.service.StudyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final ServiceItemDao serviceItemDao;
    private final DeviceDao deviceDao;
    private final StudyMapper studyMapper;

    @Value("${app.ohif.base-url:http://localhost:8043}")
    private String ohifBaseUrl = "http://localhost:8043";

    public StudyServiceImpl(StudyDao studyDao, ImagingOrderDao imagingOrderDao, ServiceItemDao serviceItemDao,
                             DeviceDao deviceDao, StudyMapper studyMapper) {
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.serviceItemDao = serviceItemDao;
        this.deviceDao = deviceDao;
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
