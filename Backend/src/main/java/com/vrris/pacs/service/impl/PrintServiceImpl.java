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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    // KHONG readOnly: AuditLogAspect ghi mot dong audit_log cho moi lan in (IN-15).
    @Transactional
    @Audited(action = AuditAction.PRINT, entityType = "study", patientIdArg = 0, entityIdArg = 1)
    public PrintDocumentResponse render(Long patientId, Long studyId, PrintSection section) {
        Context ctx = load(patientId, studyId);

        List<PdfPrintRenderer.Line> patientLines = patientLines(ctx);
        List<PdfPrintRenderer.Line> body = new ArrayList<>();
        PdfPrintRenderer.Table table = null;
        String emptyNote = "Chua co du lieu — cho tich hop HIS (doc/01-scope.md muc 3.3).";

        switch (section) {
            case FUNCTIONAL -> {
                // Phan duy nhat la du lieu that cua RIS trong 8 tuan.
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

    @Override
    // KHONG readOnly: AuditLogAspect ghi mot dong audit_log cho moi lan in (IN-15).
    @Transactional
    @Audited(action = AuditAction.PRINT, entityType = "study", patientIdArg = 0, entityIdArg = 1)
    public void recordPrintEvent(Long patientId, Long studyId, List<PrintSection> sections) {
        Context ctx = load(patientId, studyId);
        log.info("Ghi su kien in: benh nhan {}, ca chup {}, cac phan {}",
                ctx.patient().getPatientCode(), ctx.study().getAccessionNumber(), sections);
    }

    /* ------------------------------------------------------------------ Noi bo */

    private record Context(Patient patient, Study study, ImagingOrder order,
                           Optional<ServiceItem> serviceItem, Optional<Device> device,
                           Optional<Report> report, Optional<ClinicalSnapshot> snapshot) {
    }

    private Context load(Long patientId, Long studyId) {
        Patient patient = patientDao.findById(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND",
                        "Khong tim thay benh nhan."));
        Study study = studyDao.findById(studyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND",
                        "Khong tim thay ca chup."));
        ImagingOrder order = imagingOrderDao.findById(study.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh cua ca chup."));

        // Chan in nham ho so nguoi khac: ca chup phai thuoc dung benh nhan duoc yeu cau.
        if (!order.getPatientId().equals(patientId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "STUDY_PATIENT_MISMATCH",
                    "Ca chup khong thuoc benh nhan nay.");
        }

        return new Context(
                patient,
                study,
                order,
                serviceItemDao.findById(study.getServiceItemId()),
                study.getDeviceId() == null ? Optional.empty() : deviceDao.findById(study.getDeviceId()),
                reportDao.findCurrentByStudyId(studyId),
                clinicalSnapshotDao.findByOrderId(order.getId()));
    }

    private List<PdfPrintRenderer.Line> patientLines(Context ctx) {
        Patient p = ctx.patient();
        // Khong co ngay sinh thi khong tinh tuoi - KHONG duoc ra "0" (doc/01-scope.md muc 6 buoc 10).
        String ageText = p.getDateOfBirth() == null
                ? "—"
                : AgeCalculator.calculate(
                        p.getDateOfBirth(), DobPrecision.valueOf(p.getDobPrecision()), LocalDate.now()).format();

        List<PdfPrintRenderer.Line> lines = new ArrayList<>();
        lines.add(new PdfPrintRenderer.Line("Ho ten", p.getFullName()));
        lines.add(new PdfPrintRenderer.Line("Ma benh nhan", p.getPatientCode()));
        lines.add(new PdfPrintRenderer.Line("Ngay sinh", p.getDateOfBirth() == null ? null : p.getDateOfBirth().toString()));
        lines.add(new PdfPrintRenderer.Line("Tuoi", ageText));
        lines.add(new PdfPrintRenderer.Line("Gioi tinh", p.getGender()));
        lines.add(new PdfPrintRenderer.Line("Ma BHXH", p.getSocialInsuranceNo()));
        lines.add(new PdfPrintRenderer.Line("Ma phieu", ctx.order().getOrderCode()));
        lines.add(new PdfPrintRenderer.Line("Ma LSVV", ctx.order().getVisitCode()));
        lines.add(new PdfPrintRenderer.Line("Bac si chi dinh", ctx.order().getOrderingDoctorName()));
        lines.add(new PdfPrintRenderer.Line("Chi dinh", ctx.serviceItem().map(ServiceItem::getName).orElse(null)));
        return lines;
    }

    private JsonNode snapshotNode(Context ctx, String field) {
        return ctx.snapshot().map(s -> {
            try {
                JsonNode root = objectMapper.readTree(s.getPayload());
                JsonNode node = root.get(field);
                return node == null || node.isNull() ? null : node;
            } catch (Exception e) {
                log.warn("clinical_snapshot cua chi dinh {} khong phai JSON hop le: {}",
                        s.getOrderId(), e.getMessage());
                return null;
            }
        }).orElse(null);
    }

    private PdfPrintRenderer.Table arrayTable(JsonNode array, List<String> fields, List<String> headers) {
        List<List<String>> rows = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                List<String> row = new ArrayList<>();
                for (String field : fields) {
                    JsonNode value = item.get(field);
                    row.add(value == null || value.isNull() ? "—" : value.asText());
                }
                rows.add(row);
            }
        }
        return new PdfPrintRenderer.Table(headers, rows);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
    }
}
