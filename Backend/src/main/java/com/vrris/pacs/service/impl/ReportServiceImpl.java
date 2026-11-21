package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.ReportWriteRequest;
import com.vrris.pacs.dto.response.ReportResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.ReportStatus;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.ReportMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.vrris.pacs.service.impl.ServiceSupport.currentUser;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Set<ReportStatus> EDITABLE_REPORT_STATUSES =
            EnumSet.of(ReportStatus.DRAFT, ReportStatus.RETURNED);

    private final ReportDao reportDao;
    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final AppUserDao appUserDao;
    private final ReportMapper reportMapper;

    // Khởi tạo service với các DAO liên quan (báo cáo, ca chụp, chỉ định, người dùng) và mapper báo cáo.
    public ReportServiceImpl(ReportDao reportDao, StudyDao studyDao, ImagingOrderDao imagingOrderDao,
                              AppUserDao appUserDao, ReportMapper reportMapper) {
        this.reportDao = reportDao;
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.appUserDao = appUserDao;
        this.reportMapper = reportMapper;
    }

    /**
     * Doc NHIEU version bao cao cua mot ca - khong co mot report.id duy nhat de lam entity_id, nen
     * ghi entity_type = STUDY, entity_id = studyId (quy uoc: entity_id luon la id cua dung loai
     * entity_type; truoc day ghi entity_type = REPORT nhung entity_id lai la studyId).
     */
    // Lấy toàn bộ các phiên bản báo cáo của một ca chụp.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.STUDY, entityIdArg = 0)
    public List<ReportResponse> listVersions(Long studyId) {
        Study study = findStudyOrThrow(studyId);
        AuditContext.recordPatientId(resolvePatientId(study));
        return reportDao.findAllByStudyId(studyId).stream().map(reportMapper::toResponse).toList();
    }

    /** Doc bao cao hien hanh: entity_type = REPORT, entity_id = report.id (khong phai studyId). */
    // Lấy phiên bản báo cáo hiện hành (mới nhất) của một ca chụp.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.REPORT)
    public ReportResponse getCurrent(Long studyId) {
        Study study = findStudyOrThrow(studyId);
        Report current = findCurrentOrThrow(studyId);
        AuditContext.recordPatientId(resolvePatientId(study));
        AuditContext.recordEntityId(current.getId());
        return reportMapper.toResponse(current);
    }

    // Lưu nội dung báo cáo hiện hành; chỉ cho người đang giữ ca (readingUserId) khi ca ở READING và báo cáo đang DRAFT/RETURNED.
    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = AuditEntityType.REPORT)
    public ReportResponse updateCurrent(Long studyId, ReportWriteRequest request) {
        Study study = findStudyOrThrow(studyId);
        Report current = findCurrentOrThrow(studyId);

        Long callerId = currentUser(appUserDao).map(AppUser::getId).orElse(null);
        if (callerId == null || study.getReadingUserId() == null || !study.getReadingUserId().equals(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_NOT_HOLDER",
                    "Chi nguoi dang giu ca (dang doc, da mo UNREAD -> READING) duoc luu bao cao");
        }
        if (StudyStatus.READING != study.getStatus()) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDY_NOT_READING",
                    "Ca chup khong o trang thai READING");
        }
        if (!EDITABLE_REPORT_STATUSES.contains(current.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REPORT_NOT_DRAFT",
                    "Bao cao hien hanh khong con la DRAFT hoac RETURNED - version moi duoc tao khi chuyen trang thai, "
                            + "khong qua endpoint nay");
        }

        Map<String, Object> before = snapshot(current);

        current.setTechnique(request.technique());
        current.setFindings(request.findings());
        current.setDiagnosis(request.diagnosis());
        current.setConclusion(request.conclusion());
        current.setRecommendation(request.recommendation());
        Report saved = reportDao.save(current);

        AuditContext.recordEntityId(saved.getId());
        AuditContext.recordPatientId(resolvePatientId(study));
        AuditContext.recordBefore(before);
        AuditContext.recordAfter(snapshot(saved));

        return reportMapper.toResponse(saved);
    }

    // Tìm ca chụp theo id, báo lỗi 404 nếu không tồn tại.
    private Study findStudyOrThrow(Long studyId) {
        return studyDao.findById(studyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND",
                        "Khong tim thay ca chup id=" + studyId));
    }

    // Tìm báo cáo hiện hành của một ca chụp, báo lỗi 404 nếu ca chưa có báo cáo nào.
    private Report findCurrentOrThrow(Long studyId) {
        return reportDao.findCurrentByStudyId(studyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND",
                        "Ca chup id=" + studyId + " chua co bao cao"));
    }

    // Tra ra id bệnh nhân của một ca chụp thông qua chỉ định chụp liên quan.
    private Long resolvePatientId(Study study) {
        return imagingOrderDao.findById(study.getOrderId())
                .map(ImagingOrder::getPatientId)
                .orElse(null);
    }

    // Chụp lại (snapshot) các trường nội dung của báo cáo để dùng làm giá trị trước/sau khi ghi audit_log.
    private Map<String, Object> snapshot(Report report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", report.getStatus());
        map.put("technique", report.getTechnique());
        map.put("findings", report.getFindings());
        map.put("diagnosis", report.getDiagnosis());
        map.put("conclusion", report.getConclusion());
        map.put("recommendation", report.getRecommendation());
        return map;
    }
}
