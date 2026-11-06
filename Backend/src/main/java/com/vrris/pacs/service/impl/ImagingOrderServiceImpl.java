package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dto.request.ImagingOrderCreateRequest;
import com.vrris.pacs.dto.request.ImagingOrderUpdateRequest;
import com.vrris.pacs.dto.response.ImagingOrderPageResponse;
import com.vrris.pacs.dto.response.ImagingOrderResponse;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.SyncStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.ImagingOrderMapper;
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

/**
 * IN-02 (tao tay, sua, gan benh nhan, nhieu ca chup tren mot chi dinh) + IN-15 (audit) qua
 * audit.Audited, dong bo theo doc/15-kien-truc-backend.md muc 10.
 *
 * create() tao imaging_order roi goi StudyService.addToOrder cho tung phan tu "studies" trong
 * CUNG mot transaction (propagation REQUIRED mac dinh cua Spring - @Transactional cua
 * StudyServiceImpl.addToOrder tham gia transaction da mo o day, khong mo moi) - tranh lap lai
 * logic sinh accession_number o hai noi.
 *
 * Doi chieu identity_conflict (chan tao study khi benh nhan co lech PENDING, mo ta trong
 * createImagingOrder cua doc/api/openapi.yaml) la ❓ CHUA duoc nhom xac nhan (doc/02-erd.md muc 9)
 * va can model.IdentityConflict (chua co trong pham vi lan nay) - KHONG lam o day.
 */
@Service
public class ImagingOrderServiceImpl implements ImagingOrderService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "order_code", "orderCode",
            "ordered_at", "orderedAt",
            "created_at", "createdAt"
    );
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final ImagingOrderDao imagingOrderDao;
    private final PatientDao patientDao;
    private final DepartmentDao departmentDao;
    private final ImagingOrderMapper imagingOrderMapper;
    private final StudyService studyService;

    public ImagingOrderServiceImpl(ImagingOrderDao imagingOrderDao, PatientDao patientDao,
                                    DepartmentDao departmentDao, ImagingOrderMapper imagingOrderMapper,
                                    StudyService studyService) {
        this.imagingOrderDao = imagingOrderDao;
        this.patientDao = patientDao;
        this.departmentDao = departmentDao;
        this.imagingOrderMapper = imagingOrderMapper;
        this.studyService = studyService;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "IMAGING_ORDER")
    public ImagingOrderPageResponse search(Long patientId, String orderCode, String visitCode,
                                            Integer page, Integer pageSize, String sort) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        SortSpec sortSpec = parseSort(sort);

        ImagingOrderDao.SearchCriteria criteria = new ImagingOrderDao.SearchCriteria(
                patientId, blankToNull(orderCode), blankToNull(visitCode),
                safePage, safePageSize, sortSpec.field(), sortSpec.ascending());
        ImagingOrderDao.SearchResult result = imagingOrderDao.search(criteria);

        List<ImagingOrderResponse> items = result.items().stream().map(this::toResponseWithStudies).toList();
        return new ImagingOrderPageResponse(safePage, safePageSize, result.total(), items);
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
        if (departmentDao.findById(request.orderingDepartmentId()).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGING_ORDER_DEPARTMENT_NOT_FOUND",
                    "Khong tim thay khoa id=" + request.orderingDepartmentId());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String prefix = OrderCodeGenerator.datePrefix(today);
        long countToday = imagingOrderDao.countByOrderCodeStartingWith(prefix);
        String orderCode = OrderCodeGenerator.format(today, (int) countToday + 1);

        ImagingOrder entity = imagingOrderMapper.toEntity(request);
        entity.setOrderCode(orderCode);
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
        if (request.orderingDepartmentId() != null && departmentDao.findById(request.orderingDepartmentId()).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGING_ORDER_DEPARTMENT_NOT_FOUND",
                    "Khong tim thay khoa id=" + request.orderingDepartmentId());
        }

        imagingOrderMapper.updateEntityFromRequest(request, order);
        order.setUpdatedAt(LocalDateTime.now());

        ImagingOrder saved = imagingOrderDao.save(order);
        return toResponseWithStudies(saved);
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
        return new ImagingOrderResponse(base.id(), base.orderCode(), base.patientId(), base.visitCode(),
                base.source(), base.hisOrderId(), base.syncStatus(), base.orderingDepartmentId(),
                base.orderingDoctorName(), base.patientClass(), base.room(), base.bed(),
                base.clinicalDiagnosis(), base.orderedAt(), studies, base.createdAt(), base.updatedAt());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("orderedAt", false);
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
