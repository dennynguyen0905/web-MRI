package com.vrris.pacs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.ClinicalSnapshotDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dto.request.ImagingOrderCreateRequest;
import com.vrris.pacs.dto.request.ImagingOrderUpdateRequest;
import com.vrris.pacs.dto.response.ClinicalSnapshotResponse;
import com.vrris.pacs.dto.response.ImagingOrderPageResponse;
import com.vrris.pacs.dto.response.ImagingOrderResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.SyncStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.ImagingOrderMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.ClinicalSnapshot;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.rule.OrderCodeGenerator;
import com.vrris.pacs.service.ImagingOrderService;
import com.vrris.pacs.service.StudyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.vrris.pacs.service.impl.ServiceSupport.SortSpec;
import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.currentAuth;
import static com.vrris.pacs.service.impl.ServiceSupport.currentUsername;
import static com.vrris.pacs.service.impl.ServiceSupport.hasAuthority;
import static com.vrris.pacs.service.impl.ServiceSupport.parseSort;
import static com.vrris.pacs.service.impl.ServiceSupport.safePage;
import static com.vrris.pacs.service.impl.ServiceSupport.safePageSize;

@Service
public class ImagingOrderServiceImpl implements ImagingOrderService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "order_code", "orderCode",
            "ordered_at", "orderedAt",
            "created_at", "createdAt"
    );

    private final ImagingOrderDao imagingOrderDao;
    private final PatientDao patientDao;
    private final DepartmentDao departmentDao;
    private final ImagingOrderMapper imagingOrderMapper;
    private final StudyService studyService;
    private final ClinicalSnapshotDao clinicalSnapshotDao;
    private final AppUserDao appUserDao;
    private final ObjectMapper objectMapper;

    public ImagingOrderServiceImpl(ImagingOrderDao imagingOrderDao, PatientDao patientDao,
                                    DepartmentDao departmentDao, ImagingOrderMapper imagingOrderMapper,
                                    StudyService studyService, ClinicalSnapshotDao clinicalSnapshotDao,
                                    AppUserDao appUserDao, ObjectMapper objectMapper) {
        this.imagingOrderDao = imagingOrderDao;
        this.patientDao = patientDao;
        this.departmentDao = departmentDao;
        this.imagingOrderMapper = imagingOrderMapper;
        this.studyService = studyService;
        this.clinicalSnapshotDao = clinicalSnapshotDao;
        this.appUserDao = appUserDao;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "IMAGING_ORDER")
    public ImagingOrderPageResponse search(Long patientId, String orderCode, String visitCode,
                                            Integer page, Integer pageSize, String sort) {
        int resolvedPage = safePage(page);
        int resolvedPageSize = safePageSize(pageSize);
        SortSpec sortSpec = parseSort(sort, SORTABLE_FIELDS, "orderedAt", false);

        ImagingOrderDao.SearchCriteria criteria = new ImagingOrderDao.SearchCriteria(
                patientId, blankToNull(orderCode), blankToNull(visitCode),
                resolvedPage, resolvedPageSize, sortSpec.field(), sortSpec.ascending());
        ImagingOrderDao.SearchResult result = imagingOrderDao.search(criteria);

        List<ImagingOrderResponse> items = result.items().stream().map(this::toResponseWithStudies).toList();
        return new ImagingOrderPageResponse(resolvedPage, resolvedPageSize, result.total(), items);
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "IMAGING_ORDER")
    public ImagingOrderResponse getById(Long id) {
        return toResponseWithStudies(findOrThrow(id));
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.CREATE, entityType = "IMAGING_ORDER")
    public ImagingOrderResponse create(ImagingOrderCreateRequest request) {
        if (patientDao.findById(request.patientId()).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGING_ORDER_PATIENT_NOT_FOUND",
                    "Khong tim thay benh nhan id=" + request.patientId());
        }
        validateDepartment(request.orderingDepartmentId());

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        long countToday = imagingOrderDao.countByOrderCodeStartingWith(OrderCodeGenerator.datePrefix(today));

        ImagingOrder entity = imagingOrderMapper.toEntity(request);
        entity.setOrderCode(OrderCodeGenerator.format(today, (int) countToday + 1));
        entity.setSource(OrderSource.MANUAL.name());
        entity.setSyncStatus(SyncStatus.NOT_SYNCED.name());
        entity.setOrderedAt(request.orderedAt() != null ? request.orderedAt() : now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        ImagingOrder saved = imagingOrderDao.save(entity);
        List<StudyResponse> studies = request.studies().stream()
                .map(studyRequest -> studyService.addToOrder(saved.getId(), studyRequest))
                .toList();
        return toResponse(saved, studies);
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = "IMAGING_ORDER")
    public ImagingOrderResponse update(Long id, ImagingOrderUpdateRequest request) {
        ImagingOrder order = findOrThrow(id);
        if (request.orderingDepartmentId() != null) {
            validateDepartment(request.orderingDepartmentId());
        }

        imagingOrderMapper.updateEntityFromRequest(request, order);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponseWithStudies(imagingOrderDao.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "CLINICAL_SNAPSHOT", entityIdArg = 0)
    public ClinicalSnapshotResponse getClinicalSnapshot(Long orderId) {
        ImagingOrder order = findOrThrow(orderId);
        enforceClinicianScope(order);

        ClinicalSnapshot snapshot = clinicalSnapshotDao.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLINICAL_SNAPSHOT_NOT_FOUND",
                        "Chi dinh id=" + orderId + " chua co du lieu lam sang."));

        return new ClinicalSnapshotResponse(
                snapshot.getOrderId(), snapshot.getSource(), snapshot.getFetchedAt(), readPayload(snapshot));
    }

    private void validateDepartment(Long departmentId) {
        if (departmentDao.findById(departmentId).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGING_ORDER_DEPARTMENT_NOT_FOUND",
                    "Khong tim thay khoa id=" + departmentId);
        }
    }

    private void enforceClinicianScope(ImagingOrder order) {
        if (!hasAuthority(currentAuth(), "ROLE_CLINICIAN")) {
            return;
        }
        String fullName = appUserDao.findByUsername(currentUsername()).map(AppUser::getFullName).orElse(null);
        if (fullName == null || !fullName.equals(order.getOrderingDoctorName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Chi dinh id=" + order.getId() + " khong thuoc pham vi cua CLINICIAN dang dang nhap.");
        }
    }

    private JsonNode readPayload(ClinicalSnapshot snapshot) {
        try {
            return objectMapper.readTree(snapshot.getPayload());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "clinical_snapshot cua chi dinh id=" + snapshot.getOrderId() + " khong phai JSON hop le.");
        }
    }

    private ImagingOrder findOrThrow(Long id) {
        return imagingOrderDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGING_ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh id=" + id));
    }

    private ImagingOrderResponse toResponseWithStudies(ImagingOrder order) {
        return toResponse(order, studyService.listByOrderId(order.getId()));
    }

    private ImagingOrderResponse toResponse(ImagingOrder order, List<StudyResponse> studies) {
        ImagingOrderResponse base = imagingOrderMapper.toResponse(order);
        return new ImagingOrderResponse(
                base.id(), base.orderCode(), base.patientId(), base.visitCode(),
                base.source(), base.hisOrderId(), base.syncStatus(), base.orderingDepartmentId(),
                base.orderingDoctorName(), base.patientClass(), base.room(), base.bed(),
                base.clinicalDiagnosis(), base.orderedAt(), studies, base.createdAt(), base.updatedAt());
    }
}
