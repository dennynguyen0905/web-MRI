package com.vrris.pacs.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.ClinicalSnapshotDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.response.PrintDocumentResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.PrintSection;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.model.ClinicalSnapshot;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.rule.AgeCalculator;
import com.vrris.pacs.rule.PrintDocumentNaming;
import com.vrris.pacs.service.PrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.vrris.pacs.service.impl.ServiceSupport.currentUsername;

@Service
public class PrintServiceImpl implements PrintService {

    private static final Logger log = LoggerFactory.getLogger(PrintServiceImpl.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PatientDao patientDao;
    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final ServiceItemDao serviceItemDao;
    private final DeviceDao deviceDao;
    private final ReportDao reportDao;
    private final ClinicalSnapshotDao clinicalSnapshotDao;
    private final PdfPrintRenderer renderer;
    private final ObjectMapper objectMapper;

    // Khởi tạo service với các DAO cần để gom dữ liệu phiếu in và renderer PDF.
    public PrintServiceImpl(PatientDao patientDao, StudyDao studyDao, ImagingOrderDao imagingOrderDao,
                            ServiceItemDao serviceItemDao, DeviceDao deviceDao, ReportDao reportDao,
                            ClinicalSnapshotDao clinicalSnapshotDao, PdfPrintRenderer renderer,
                            ObjectMapper objectMapper) {
        this.patientDao = patientDao;
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.serviceItemDao = serviceItemDao;
        this.deviceDao = deviceDao;
        this.reportDao = reportDao;
        this.clinicalSnapshotDao = clinicalSnapshotDao;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
    }

    // Gom dữ liệu bệnh nhân/ca chụp/báo cáo/clinical_snapshot theo từng phần (FUNCTIONAL/OVERVIEW/LABS/DRUGS/SURGERY) và render ra PDF.
    @Override
    @Transactional
    @Audited(action = AuditAction.PRINT, entityType = AuditEntityType.STUDY, patientIdArg = 0, entityIdArg = 1)
    public PrintDocumentResponse render(Long patientId, Long studyId, PrintSection section) {
        Context ctx = load(patientId, studyId);
        List<PdfPrintRenderer.Line> patientLines = patientLines(ctx);
        List<PdfPrintRenderer.Line> body = new ArrayList<>();
        PdfPrintRenderer.Table table = null;
        String emptyNote = "Chua co du lieu — cho tich hop HIS (doc/01-scope.md muc 3.3).";

        switch (section) {
            case FUNCTIONAL -> {
                Report report = ctx.report().orElse(null);
                body.add(new PdfPrintRenderer.Line("Ten dich vu", ctx.serviceItem().map(ServiceItem::getName).orElse(null)));
                body.add(new PdfPrintRenderer.Line("May chup", ctx.device().map(Device::getName).orElse(null)));
                body.add(new PdfPrintRenderer.Line("Ky thuat", report == null ? null : report.getTechnique()));
                body.add(new PdfPrintRenderer.Line("Mo ta", report == null ? null : report.getFindings()));
                body.add(new PdfPrintRenderer.Line("Chan doan", report == null
                        ? ctx.order().getClinicalDiagnosis() : report.getDiagnosis()));
                body.add(new PdfPrintRenderer.Line("Ket luan", report == null ? null : report.getConclusion()));
                body.add(new PdfPrintRenderer.Line("Khuyen nghi", report == null ? null : report.getRecommendation()));
                if (report == null) {
                    emptyNote = "Ca chup nay chua co bao cao ket qua.";
                }
            }
            case OVERVIEW -> {
                JsonNode overview = snapshotNode(ctx, "overview");
                if (overview != null) {
                    overview.fields().forEachRemaining(e ->
                            body.add(new PdfPrintRenderer.Line(e.getKey(), e.getValue().asText())));
                }
                emptyNote = "Chi dinh nay chua co du lieu tong quan kham trong clinical_snapshot.";
            }
            case LABS -> {
                table = arrayTable(snapshotNode(ctx, "labs"), List.of("stt", "ten", "trang_thai"),
                        List.of("STT", "Ten xet nghiem", "Trang thai"));
                emptyNote = "Chi dinh nay chua co ket qua xet nghiem trong clinical_snapshot.";
            }
            case DRUGS -> table = arrayTable(snapshotNode(ctx, "drugs"), List.of("ten", "so_luong", "don_vi"),
                    List.of("Ten thuoc/vat tu", "So luong", "Don vi"));
            case SURGERY -> table = arrayTable(snapshotNode(ctx, "surgery"), List.of("ten"), List.of("Noi dung"));
        }

        String footer = "Nguoi in: %s · In luc %s · Ma ho so %s · Ma benh nhan %s".formatted(
                currentUsername(), STAMP.format(LocalDateTime.now()),
                ctx.study().getAccessionNumber(), ctx.patient().getPatientCode());

        byte[] pdf = renderer.render(
                PrintDocumentNaming.title(section), patientLines, body, table, emptyNote, footer);

        return new PrintDocumentResponse(
                PrintDocumentNaming.fileName(
                        ctx.patient().getPatientCode(), ctx.study().getAccessionNumber(), section, LocalDate.now()),
                "application/pdf",
                pdf);
    }

    // Ghi nhận sự kiện đã in các phần của phiếu (phục vụ audit_log), không trả dữ liệu.
    @Override
    @Transactional
    @Audited(action = AuditAction.PRINT, entityType = AuditEntityType.STUDY, patientIdArg = 0, entityIdArg = 1)
    public void recordPrintEvent(Long patientId, Long studyId, List<PrintSection> sections) {
        Context ctx = load(patientId, studyId);
        log.info("Ghi su kien in: benh nhan {}, ca chup {}, cac phan {}",
                ctx.patient().getPatientCode(), ctx.study().getAccessionNumber(), sections);
    }

    private record Context(
            Patient patient,
            Study study,
            ImagingOrder order,
            Optional<ServiceItem> serviceItem,
            Optional<Device> device,
            Optional<Report> report,
            Optional<ClinicalSnapshot> snapshot
    ) {
    }

    // Nạp toàn bộ dữ liệu liên quan (bệnh nhân, ca chụp, chỉ định, dịch vụ, máy, báo cáo, clinical_snapshot) cho một lần in; kiểm ca chụp thuộc đúng bệnh nhân.
    private Context load(Long patientId, Long studyId) {
        Patient patient = patientDao.findById(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "Khong tim thay benh nhan."));
        Study study = studyDao.findById(studyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND", "Khong tim thay ca chup."));
        ImagingOrder order = imagingOrderDao.findById(study.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Khong tim thay chi dinh cua ca chup."));

        if (!order.getPatientId().equals(patientId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "STUDY_PATIENT_MISMATCH", "Ca chup khong thuoc benh nhan nay.");
        }

        return new Context(
                patient, study, order,
                serviceItemDao.findById(study.getServiceItemId()),
                study.getDeviceId() == null ? Optional.empty() : deviceDao.findById(study.getDeviceId()),
                reportDao.findCurrentByStudyId(studyId),
                clinicalSnapshotDao.findByOrderId(order.getId()));
    }

    // Dựng danh sách dòng "nhãn : giá trị" thông tin bệnh nhân/chỉ định để in ở đầu phiếu.
    private List<PdfPrintRenderer.Line> patientLines(Context ctx) {
        Patient p = ctx.patient();
        String ageText = p.getDateOfBirth() == null
                ? "—"
                : AgeCalculator.calculate(
                        p.getDateOfBirth(), p.getDobPrecision(), LocalDate.now()).format();

        return List.of(
                new PdfPrintRenderer.Line("Ho ten", p.getFullName()),
                new PdfPrintRenderer.Line("Ma benh nhan", p.getPatientCode()),
                new PdfPrintRenderer.Line("Ngay sinh", p.getDateOfBirth() == null ? null : p.getDateOfBirth().toString()),
                new PdfPrintRenderer.Line("Tuoi", ageText),
                new PdfPrintRenderer.Line("Gioi tinh", p.getGender() == null ? null : p.getGender().name()),
                new PdfPrintRenderer.Line("Ma BHXH", p.getSocialInsuranceNo()),
                new PdfPrintRenderer.Line("Ma phieu", ctx.order().getOrderCode()),
                new PdfPrintRenderer.Line("Ma LSVV", ctx.order().getVisitCode()),
                new PdfPrintRenderer.Line("Bac si chi dinh", ctx.order().getOrderingDoctorName()),
                new PdfPrintRenderer.Line("Chi dinh", ctx.serviceItem().map(ServiceItem::getName).orElse(null))
        );
    }

    // Lấy một field (node) cụ thể trong payload JSON của clinical_snapshot, trả null nếu thiếu hoặc JSON không hợp lệ.
    private JsonNode snapshotNode(Context ctx, String field) {
        return ctx.snapshot().map(s -> {
            try {
                JsonNode root = objectMapper.readTree(s.getPayload());
                JsonNode node = root.get(field);
                return node == null || node.isNull() ? null : node;
            } catch (Exception e) {
                log.warn("clinical_snapshot cua chi dinh {} khong phai JSON hop le: {}", s.getOrderId(), e.getMessage());
                return null;
            }
        }).orElse(null);
    }

    // Chuyển một mảng JSON (ví dụ danh sách xét nghiệm/thuốc) thành bảng dữ liệu để in, theo danh sách field/tiêu đề chỉ định.
    private PdfPrintRenderer.Table arrayTable(JsonNode array, List<String> fields, List<String> headers) {
        List<List<String>> rows = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                rows.add(fields.stream()
                        .map(item::get)
                        .map(val -> val == null || val.isNull() ? "—" : val.asText())
                        .toList());
            }
        }
        return new PdfPrintRenderer.Table(headers, rows);
    }
}
