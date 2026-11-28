package com.vrris.pacs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.ClinicalSnapshotDao;
import com.vrris.pacs.dao.DailySequenceDao;
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
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.SyncStatus;
import com.vrris.pacs.enu.UserRole;
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
    private final DailySequenceDao dailySequenceDao;

    // Khởi tạo service với các DAO/mapper/service liên quan đến chỉ định chụp.
    public ImagingOrderServiceImpl(ImagingOrderDao imagingOrderDao, PatientDao patientDao,
                                    DepartmentDao departmentDao, ImagingOrderMapper imagingOrderMapper,
                                    StudyService studyService, ClinicalSnapshotDao clinicalSnapshotDao,
                                    AppUserDao appUserDao, ObjectMapper objectMapper,
                                    DailySequenceDao dailySequenceDao) {
        this.imagingOrderDao = imagingOrderDao;
        this.patientDao = patientDao;
        this.departmentDao = departmentDao;
        this.imagingOrderMapper = imagingOrderMapper;
        this.studyService = studyService;
        this.clinicalSnapshotDao = clinicalSnapshotDao;
        this.appUserDao = appUserDao;
        this.objectMapper = objectMapper;
        this.dailySequenceDao = dailySequenceDao;
    }

    // Tra cứu danh sách chỉ định chụp theo bệnh nhân/mã phiếu/mã LSVV, có phân trang và sắp xếp.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.IMAGING_ORDER)
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

    // Lấy chi tiết một chỉ định chụp theo id, kèm danh sách ca chụp con.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.IMAGING_ORDER)
    public ImagingOrderResponse getById(Long id) {
        return toResponseWithStudies(findOrThrow(id));
    }

    // Tạo chỉ định chụp mới (sinh order_code nguyên tử qua daily_sequence), đồng thời tạo các ca chụp con kèm theo.
    @Override
    @Transactional
    @Audited(action = AuditAction.CREATE, entityType = AuditEntityType.IMAGING_ORDER)
    public ImagingOrderResponse create(ImagingOrderCreateRequest request) {
        if (patientDao.findById(request.patientId()).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGING_ORDER_PATIENT_NOT_FOUND",
                    "Khong tim thay benh nhan id=" + request.patientId());
        }
        validateDepartment(request.orderingDepartmentId());

        LocalDateTime now = LocalDateTime.now();

        ImagingOrder entity = imagingOrderMapper.toEntity(request);
        entity.setOrderCode(nextOrderCode(now.toLocalDate()));
        entity.setSource(OrderSource.MANUAL);
        entity.setSyncStatus(SyncStatus.NOT_SYNCED);
        entity.setOrderedAt(request.orderedAt() != null ? request.orderedAt() : now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        ImagingOrder saved = imagingOrderDao.save(entity);
        List<StudyResponse> studies = request.studies().stream()
                .map(studyRequest -> studyService.addToOrder(saved.getId(), studyRequest))
                .toList();
        return imagingOrderMapper.toResponse(saved, studies);
    }

    // Cập nhật thông tin chỉ định chụp (khoa chỉ định, bác sĩ, chẩn đoán lâm sàng...).
    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = AuditEntityType.IMAGING_ORDER)
    public ImagingOrderResponse update(Long id, ImagingOrderUpdateRequest request) {
        ImagingOrder order = findOrThrow(id);
        if (request.orderingDepartmentId() != null) {
            validateDepartment(request.orderingDepartmentId());
        }

        imagingOrderMapper.updateEntityFromRequest(request, order);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponseWithStudies(imagingOrderDao.save(order));
    }

    // Lấy dữ liệu lâm sàng (clinical_snapshot) của một chỉ định chụp; chặn CLINICIAN xem chỉ định không phải của mình.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.CLINICAL_SNAPSHOT, entityIdArg = 0)
    public ClinicalSnapshotResponse getClinicalSnapshot(Long orderId) {
        ImagingOrder order = findOrThrow(orderId);
        enforceClinicianScope(order);

        ClinicalSnapshot snapshot = clinicalSnapshotDao.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLINICAL_SNAPSHOT_NOT_FOUND",
                        "Chi dinh id=" + orderId + " chua co du lieu lam sang."));

        return new ClinicalSnapshotResponse(
                snapshot.getOrderId(), snapshot.getSource(), snapshot.getFetchedAt(), readPayload(snapshot));
    }

    // Kiểm tra khoa chỉ định có tồn tại không, báo lỗi 422 nếu không tìm thấy.
    private void validateDepartment(Long departmentId) {
        if (departmentDao.findById(departmentId).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGING_ORDER_DEPARTMENT_NOT_FOUND",
                    "Khong tim thay khoa id=" + departmentId);
        }
    }

    // Nếu người gọi là CLINICIAN thì chỉ cho xem chỉ định do chính bác sĩ đó chỉ định, báo lỗi 403 nếu không khớp.
    private void enforceClinicianScope(ImagingOrder order) {
        if (!hasAuthority(currentAuth(), UserRole.CLINICIAN.authority())) {
            return;
        }
        String fullName = appUserDao.findByUsername(currentUsername()).map(AppUser::getFullName).orElse(null);
        if (fullName == null || !fullName.equals(order.getOrderingDoctorName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Chi dinh id=" + order.getId() + " khong thuoc pham vi cua CLINICIAN dang dang nhap.");
        }
    }

    // Parse payload JSON của clinical_snapshot, báo lỗi 500 nếu dữ liệu lưu không phải JSON hợp lệ.
    private JsonNode readPayload(ClinicalSnapshot snapshot) {
        try {
            return objectMapper.readTree(snapshot.getPayload());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "clinical_snapshot cua chi dinh id=" + snapshot.getOrderId() + " khong phai JSON hop le.");
        }
    }

    // Tìm chỉ định chụp theo id, báo lỗi 404 nếu không tồn tại.
    private ImagingOrder findOrThrow(Long id) {
        return imagingOrderDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGING_ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh id=" + id));
    }

    // Chuyển chỉ định chụp sang DTO kèm danh sách ca chụp con lấy từ StudyService.
    private ImagingOrderResponse toResponseWithStudies(ImagingOrder order) {
        return imagingOrderMapper.toResponse(order, studyService.listByOrderId(order.getId()));
    }

    /**
     * Cap order_code nguyen tu qua bang daily_sequence (V9); bo qua so da bi dung (du lieu tao sau
     * khi migration khoi tao sequence) thay vi de UNIQUE uk_imaging_order_code vo -> 500.
     */
    // Cấp order_code kế tiếp theo ngày, bỏ qua số đã bị dùng trước đó để tránh vi phạm UNIQUE.
    private String nextOrderCode(LocalDate date) {
        String key = OrderCodeGenerator.sequenceKey(date);
        while (true) {
            int sequence = dailySequenceDao.next(key);
            if (sequence > OrderCodeGenerator.MAX_SEQUENCE_PER_DAY) {
                throw new ApiException(HttpStatus.CONFLICT, "ORDER_CODE_SEQUENCE_EXHAUSTED",
                        "Da het so phieu chi dinh trong ngay " + date
                                + " (toi da " + OrderCodeGenerator.MAX_SEQUENCE_PER_DAY + ")");
            }
            String candidate = OrderCodeGenerator.format(date, sequence);
            if (imagingOrderDao.findByOrderCode(candidate).isEmpty()) {
                return candidate;
            }
        }
    }
}
