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
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.ReportMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IN-10 - xem Javadoc service.ReportService. Doc study.status/reading_user_id truc tiep qua
 * dao.StudyDao (KHONG qua service.StudyService) de tranh phu thuoc vong voi StudyServiceImpl dang
 * duoc sua o IN-08 cung luc (phoi hop qua tin nhan giua cac phien, xem lich su thay doi README).
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final String STATUS_READING = "READING";
    private static final String STATUS_DRAFT = "DRAFT";

    private final ReportDao reportDao;
    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final AppUserDao appUserDao;
    private final ReportMapper reportMapper;

    public ReportServiceImpl(ReportDao reportDao, StudyDao studyDao, ImagingOrderDao imagingOrderDao,
                              AppUserDao appUserDao, ReportMapper reportMapper) {
        this.reportDao = reportDao;
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.appUserDao = appUserDao;
        this.reportMapper = reportMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "REPORT", entityIdArg = 0)
    public List<ReportResponse> listVersions(Long studyId) {
        Study study = findStudyOrThrow(studyId);
        AuditContext.recordPatientId(resolvePatientId(study));
        return reportDao.findAllByStudyId(studyId).stream().map(reportMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = "REPORT", entityIdArg = 0)
    public ReportResponse getCurrent(Long studyId) {
        Study study = findStudyOrThrow(studyId);
        AuditContext.recordPatientId(resolvePatientId(study));
        Report current = findCurrentOrThrow(studyId);
        return reportMapper.toResponse(current);
    }

    @Override
    @Transactional
    @Audited(action = AuditAction.UPDATE, entityType = "REPORT")
    public ReportResponse updateCurrent(Long studyId, ReportWriteRequest request) {
        Study study = findStudyOrThrow(studyId);
        Report current = findCurrentOrThrow(studyId);

        Long callerId = resolveCallerId();
        if (callerId == null || study.getReadingUserId() == null || !study.getReadingUserId().equals(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_NOT_HOLDER",
                    "Chi nguoi dang giu ca (dang doc, da mo UNREAD -> READING) duoc luu bao cao");
        }
        if (!STATUS_READING.equals(study.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDY_NOT_READING",
                    "Ca chup khong o trang thai READING");
        }
        if (!STATUS_DRAFT.equals(current.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REPORT_NOT_DRAFT",
                    "Bao cao hien hanh khong con la DRAFT - version moi duoc tao khi chuyen trang thai, "
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

    private Study findStudyOrThrow(Long studyId) {
        return studyDao.findById(studyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND",
                        "Khong tim thay ca chup id=" + studyId));
    }

    private Report findCurrentOrThrow(Long studyId) {
        return reportDao.findCurrentByStudyId(studyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND",
                        "Ca chup id=" + studyId + " chua co bao cao"));
    }

    private Long resolveCallerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return appUserDao.findByUsername(String.valueOf(authentication.getPrincipal()))
                .map(AppUser::getId)
                .orElse(null);
    }

    private Long resolvePatientId(Study study) {
        return imagingOrderDao.findById(study.getOrderId())
                .map(ImagingOrder::getPatientId)
                .orElse(null);
    }

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
