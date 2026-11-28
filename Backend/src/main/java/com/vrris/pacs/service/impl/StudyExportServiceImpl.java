package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dao.StudyLookupDao;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.enu.WorklistTimeField;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.rule.PatientListCsvExporter;
import com.vrris.pacs.service.StudyExportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.vrris.pacs.service.impl.ServiceSupport.MAX_EXPORT_ROWS;
import static com.vrris.pacs.service.impl.ServiceSupport.SortSpec;
import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.normalizeLower;
import static com.vrris.pacs.service.impl.ServiceSupport.parseEnumOrNull;
import static com.vrris.pacs.service.impl.ServiceSupport.parseSort;

/**
 * Xuat CSV worklist (IN-25). So truy van CO DINH (khong N+1): 1 truy van tim ca chup (hoac theo
 * order_id), roi moi loai order/patient/service_item/device/report nap theo lo bang StudyLookupDao.
 *
 * Khong cat bot im lang: neu bo loc khop nhieu hon MAX_EXPORT_ROWS dong -> 422 EXPORT_TOO_MANY_ROWS.
 * Tham so q van loc trong bo nho (StudyDao.SearchCriteria chua co dieu kien "q" OR nhieu cot), nhung
 * vi da chan truong hop vuot gioi han nen khong con mat dong.
 */
@Service
public class StudyExportServiceImpl implements StudyExportService {

    private static final WorklistTimeField DEFAULT_TIME_FIELD = WorklistTimeField.SCHEDULED;

    private final StudyDao studyDao;
    private final StudyLookupDao studyLookupDao;

    // Khởi tạo service với DAO ca chụp và DAO tra cứu theo lô (StudyLookupDao).
    public StudyExportServiceImpl(StudyDao studyDao, StudyLookupDao studyLookupDao) {
        this.studyDao = studyDao;
        this.studyLookupDao = studyLookupDao;
    }

    // Xuất danh sách ca chụp (worklist) ra CSV: lấy ca chụp theo điều kiện, nạp theo lô dữ liệu liên quan (chỉ định, bệnh nhân, dịch vụ, máy, báo cáo) để tránh N+1, rồi lọc và dựng từng dòng.
    @Override
    @Transactional
    @Audited(action = AuditAction.EXPORT, entityType = AuditEntityType.STUDY)
    public ExportDocumentResponse exportCsv(Long orderId, String status, Long deviceId,
                                            String q, String patientCode, String fullName, String sort) {
        SortSpec sortSpec = parseSort(sort, StudyServiceImpl.SORTABLE_FIELDS, "scheduledAt", true);
        StudyStatus normalizedStatus = parseEnumOrNull(status, StudyStatus.class, "status");

        List<Study> studies = orderId != null
                ? findByOrder(orderId, normalizedStatus, deviceId)
                : searchStudies(normalizedStatus, deviceId, patientCode, fullName, sortSpec);

        Map<Long, ImagingOrder> orders = byId(studyLookupDao.findAllByIds(ImagingOrder.class,
                collect(studies, Study::getOrderId)), ImagingOrder::getId);
        Map<Long, Patient> patients = byId(studyLookupDao.findAllByIds(Patient.class,
                collect(orders.values(), ImagingOrder::getPatientId)), Patient::getId);
        Map<Long, ServiceItem> services = byId(studyLookupDao.findAllByIds(ServiceItem.class,
                collect(studies, Study::getServiceItemId)), ServiceItem::getId);
        Map<Long, Device> devices = byId(studyLookupDao.findAllByIds(Device.class,
                collect(studies, Study::getDeviceId)), Device::getId);
        Map<Long, Report> reports = byId(studyLookupDao.findCurrentReportsByStudyIds(
                collect(studies, Study::getId)), Report::getStudyId);

        String normQ = normalizeLower(q);
        String normCode = normalizeLower(patientCode);
        String normName = normalizeLower(fullName);

        List<PatientListCsvExporter.StudyWorklistRow> rows = new ArrayList<>();
        Set<Long> distinctPatientIds = new LinkedHashSet<>();
        for (Study study : studies) {
            ImagingOrder order = orders.get(study.getOrderId());
            Patient patient = order == null ? null : patients.get(order.getPatientId());
            if (!matchesPatientFilters(study, order, patient, normQ, normCode, normName)) {
                continue;
            }
            if (patient != null && patient.getId() != null) {
                distinctPatientIds.add(patient.getId());
            }
            rows.add(toCsvRow(study, order, patient, services.get(study.getServiceItemId()),
                    devices.get(study.getDeviceId()), reports.get(study.getId())));
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

    /** order_id: lay toan bo ca cua chi dinh (so luong nho) roi loc status/device trong bo nho. */
    // Lấy toàn bộ ca chụp của một chỉ định rồi lọc theo trạng thái/máy trong bộ nhớ.
    private List<Study> findByOrder(Long orderId, StudyStatus status, Long deviceId) {
        return studyDao.findByOrderId(orderId).stream()
                .filter(s -> status == null || status == s.getStatus())
                .filter(s -> deviceId == null || deviceId.equals(s.getDeviceId()))
                .toList();
    }

    // Tìm ca chụp theo điều kiện lọc qua DAO; báo lỗi 422 nếu số dòng khớp vượt giới hạn xuất tối đa.
    private List<Study> searchStudies(StudyStatus status, Long deviceId, String patientCode, String fullName,
                                      SortSpec sortSpec) {
        StudyDao.SearchCriteria criteria = new StudyDao.SearchCriteria(
                status == null ? null : List.of(status),
                null,
                deviceId == null ? null : List.of(deviceId),
                null, null,
                blankToNull(patientCode), blankToNull(fullName), null, null,
                DEFAULT_TIME_FIELD, null, null,
                1, MAX_EXPORT_ROWS, sortSpec.field(), sortSpec.ascending());
        StudyDao.SearchResult result = studyDao.search(criteria);
        if (result.total() > MAX_EXPORT_ROWS) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "EXPORT_TOO_MANY_ROWS",
                    "Bo loc khop " + result.total() + " ca chup, vuot gioi han xuat " + MAX_EXPORT_ROWS
                            + " dong - hay thu hep bo loc");
        }
        return result.items();
    }

    // Lấy danh sách id (không null, không trùng) từ một tập đối tượng theo hàm lấy id truyền vào.
    private static <T> List<Long> collect(Collection<T> items, Function<T, Long> idGetter) {
        return items.stream().map(idGetter).filter(Objects::nonNull).distinct().toList();
    }

    // Dựng map tra nhanh theo id từ một danh sách đối tượng.
    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> idGetter) {
        return items.stream().collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }

    // Dựng một dòng CSV worklist từ ca chụp cùng các dữ liệu liên quan (chỉ định, bệnh nhân, dịch vụ, máy, báo cáo).
    private PatientListCsvExporter.StudyWorklistRow toCsvRow(Study study, ImagingOrder order, Patient patient,
                                                             ServiceItem service, Device device, Report report) {
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
                        ? DobPrecision.DAY : patient.getDobPrecision(),
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

    // Kiểm tra một ca chụp có khớp các điều kiện tìm kiếm theo mã/tên bệnh nhân, mã phiếu, số accession hay không.
    private boolean matchesPatientFilters(Study study, ImagingOrder order, Patient patient,
                                          String normQ, String normCode, String normName) {
        String pCode = lower(patient == null ? null : patient.getPatientCode());
        String pName = lower(patient == null ? null : patient.getFullName());
        String oCode = lower(order == null ? null : order.getOrderCode());
        String accNo = lower(study.getAccessionNumber());

        if (normCode != null && !pCode.contains(normCode)) return false;
        if (normName != null && !pName.contains(normName)) return false;
        return normQ == null || pCode.contains(normQ) || pName.contains(normQ)
                || oCode.contains(normQ) || accNo.contains(normQ);
    }

    // Chuyển chuỗi về dạng viết thường để so sánh không phân biệt hoa/thường, trả chuỗi rỗng nếu null.
    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
