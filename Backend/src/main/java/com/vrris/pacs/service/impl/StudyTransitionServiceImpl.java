package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dao.StudyLookupDao;
import com.vrris.pacs.dao.StudyStatusHistoryDao;
import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.dto.response.ReportResponse;
import com.vrris.pacs.dto.response.StatusHistoryEntry;
import com.vrris.pacs.dto.response.TransitionResponse;
import com.vrris.pacs.dto.response.UserRefResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.ReportOrigin;
import com.vrris.pacs.enu.ReportStatus;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.ReportMapper;
import com.vrris.pacs.mapper.StudyMapper;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.model.StudyStatusHistory;
import com.vrris.pacs.rule.StudyStateMachine;
import com.vrris.pacs.rule.TransitionCode;
import com.vrris.pacs.rule.TransitionPolicy;
import com.vrris.pacs.service.StudyTransitionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.vrris.pacs.service.impl.ServiceSupport.blankToNull;
import static com.vrris.pacs.service.impl.ServiceSupport.currentAuth;
import static com.vrris.pacs.service.impl.StudyServiceSupport.findStudyOrThrow;
import static com.vrris.pacs.service.impl.StudyServiceSupport.parseStoredStatus;
import static com.vrris.pacs.service.impl.StudyServiceSupport.resolvePatientId;
import static com.vrris.pacs.service.impl.StudyServiceSupport.rolesOf;

/**
 * Chuyen trang thai ca chup T2..T10 (doc/03-state-machine.md muc 2.2).
 *
 * Phan chia trach nhiem:
 * - rule.StudyStateMachine: cap (from, to) nao hop le -> TransitionCode (409 INVALID_TRANSITION neu khong).
 * - rule.TransitionPolicy: ai duoc lam, dieu kien gi (403 / 409 / 422).
 * - lop nay: doc du lieu, goi hai rule tren, ap dung thay doi (applyEffects), ghi lich su.
 *
 * Dong thoi: study/report co @Version (cot row_version, V9) - hai yeu cau cung luc tren mot ca
 * (vi du hai bac si cung T3) thi yeu cau commit sau that bai voi ObjectOptimisticLockingFailureException
 * (doc/03 muc 2.4 dieu 5, SM-16).
 */
@Service
public class StudyTransitionServiceImpl implements StudyTransitionService {

    private final StudyDao studyDao;
    private final ImagingOrderDao imagingOrderDao;
    private final ReportDao reportDao;
    private final AppUserDao appUserDao;
    private final StudyStatusHistoryDao studyStatusHistoryDao;
    private final StudyLookupDao studyLookupDao;
    private final StudyMapper studyMapper;
    private final ReportMapper reportMapper;

    // Khởi tạo service với các DAO/mapper cần cho việc chuyển trạng thái ca chụp và ghi lịch sử.
    public StudyTransitionServiceImpl(StudyDao studyDao, ImagingOrderDao imagingOrderDao, ReportDao reportDao,
                                      AppUserDao appUserDao, StudyStatusHistoryDao studyStatusHistoryDao,
                                      StudyLookupDao studyLookupDao, StudyMapper studyMapper,
                                      ReportMapper reportMapper) {
        this.studyDao = studyDao;
        this.imagingOrderDao = imagingOrderDao;
        this.reportDao = reportDao;
        this.appUserDao = appUserDao;
        this.studyStatusHistoryDao = studyStatusHistoryDao;
        this.studyLookupDao = studyLookupDao;
        this.studyMapper = studyMapper;
        this.reportMapper = reportMapper;
    }

    // Chuyển trạng thái một ca chụp (T2..T10): xác định bước chuyển hợp lệ qua StudyStateMachine, kiểm quyền/điều kiện qua TransitionPolicy, áp dụng thay đổi rồi ghi lịch sử chuyển trạng thái.
    @Override
    @Transactional
    @Audited(action = AuditAction.STATUS_CHANGE, entityType = AuditEntityType.STUDY, entityIdArg = 0)
    public TransitionResponse transition(Long id, TransitionRequest request) {
        Study study = findStudyOrThrow(studyDao, id);
        AuditContext.recordPatientId(resolvePatientId(imagingOrderDao, study));

        Authentication auth = currentAuth();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chua dang nhap");
        }
        AppUser caller = appUserDao.findByUsername(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Nguoi dung khong ton tai"));

        StudyStatus from = parseStoredStatus(study);
        StudyStatus to = request.toStatus();
        TransitionCode code = StudyStateMachine.resolveTransitionCode(from, to);
        if (code == null) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Khong cho phep chuyen trang thai tu " + from.name() + " sang " + to.name());
        }

        Report currentReport = reportDao.findCurrentByStudyId(id).orElse(null);
        TransitionPolicy.Actor actor = new TransitionPolicy.Actor(caller.getId(), rolesOf(auth));
        TransitionPolicy.check(code, actor, studyFacts(study), reportFacts(currentReport), request.reason())
                .ifPresent(violation -> {
                    throw toApiException(violation);
                });

        LocalDateTime now = LocalDateTime.now();
        Report resultReport = applyEffects(code, study, currentReport, caller.getId(), now);
        study.setUpdatedAt(now);
        Study savedStudy = studyDao.save(study);

        StudyStatusHistory history = new StudyStatusHistory();
        history.setStudyId(id);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedByUserId(caller.getId());
        history.setChangedAt(now);
        history.setReason(blankToNull(request.reason()));
        history.setNote(blankToNull(request.note()));
        StudyStatusHistory savedHistory = studyStatusHistoryDao.save(history);

        StatusHistoryEntry entry = toStatusHistoryEntry(savedHistory, caller, code.name());
        ReportResponse reportRes = resultReport == null ? null : reportMapper.toResponse(resultReport);
        return new TransitionResponse(studyMapper.toResponse(savedStudy), entry, reportRes);
    }

    // Lấy lịch sử chuyển trạng thái của một ca chụp, kèm thông tin người thực hiện từng bước.
    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entityType = AuditEntityType.STUDY, entityIdArg = 0)
    public List<StatusHistoryEntry> listStatusHistory(Long studyId) {
        Study study = findStudyOrThrow(studyDao, studyId);
        AuditContext.recordPatientId(resolvePatientId(imagingOrderDao, study));

        List<StudyStatusHistory> rows = studyStatusHistoryDao.findByStudyIdOrderByChangedAtAsc(studyId);
        List<Long> userIds = rows.stream().map(StudyStatusHistory::getChangedByUserId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, AppUser> usersById = studyLookupDao.findAllByIds(AppUser.class, userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));

        return rows.stream()
                .map(h -> toStatusHistoryEntry(h, usersById.get(h.getChangedByUserId()), resolveHistoryTransitionCode(h)))
                .toList();
    }

    /* ----------------------------------------- Thay doi du lieu cua tung buoc T2..T10 */

    /**
     * Ap dung thay doi cua buoc chuyen (cot "Hành động kèm theo" doc/03 muc 2.2). Tra ve bao cao
     * hien hanh sau buoc chuyen (de tra kem response), co the null.
     */
    // Áp dụng thay đổi dữ liệu tương ứng với từng bước chuyển trạng thái T2..T10 (cập nhật ca chụp và báo cáo liên quan).
    private Report applyEffects(TransitionCode code, Study study, Report currentReport, Long callerId,
                                LocalDateTime now) {
        study.setStatus(code.to());
        return switch (code) {
            case T2 -> {
                // doc/03 muc 2.2 T2 + openapi Study.completed_at: "Ket thuc chup, dat khi chuyen
                // SCHEDULED -> UNREAD". completed_at/started_at la thoi gian CHUP, nen T3 (mo doc) khong
                // dat started_at, T7 (duyet) khong ghi de completed_at, T9 (mo lai) khong xoa no.
                if (study.getCompletedAt() == null) {
                    study.setCompletedAt(now);
                }
                yield currentReport;
            }
            case T3 -> {
                study.setReadingUserId(callerId);
                if (currentReport == null) {
                    yield reportDao.save(newDraft(study.getId(), 1, null, callerId, now));
                }
                if (ReportStatus.DRAFT == currentReport.getStatus()) {
                    currentReport.setCreatedByUserId(callerId);
                    yield reportDao.save(currentReport);
                }
                yield currentReport;
            }
            case T4 -> {
                study.setReadingUserId(null);
                yield currentReport;
            }
            case T5 -> {
                // TransitionPolicy da dam bao currentReport != null.
                currentReport.setStatus(ReportStatus.FINAL);
                yield reportDao.save(currentReport);
            }
            case T6 -> {
                study.setApprovingUserId(callerId);
                yield currentReport;
            }
            case T7 -> {
                // TransitionPolicy da dam bao currentReport != null.
                currentReport.setStatus(ReportStatus.APPROVED);
                currentReport.setApprovedByUserId(callerId);
                currentReport.setApprovedAt(now);
                yield reportDao.save(currentReport);
            }
            case T8 -> {
                study.setApprovingUserId(null);
                if (currentReport == null) {
                    yield null;
                }
                currentReport.setStatus(ReportStatus.RETURNED);
                yield reportDao.save(currentReport);
            }
            case T9 -> {
                study.setApprovingUserId(null);
                if (currentReport == null) {
                    yield null;
                }
                // doc/02-erd.md 4.10: ban APPROVED cu giu nguyen; ban moi version + 1, DRAFT, khong ke
                // thua approved_by/approved_at, noi dung sao chep tu ban cu lam diem xuat phat.
                Long authorId = study.getReadingUserId() != null ? study.getReadingUserId() : callerId;
                yield reportDao.save(newDraft(study.getId(), currentReport.getVersion() + 1, currentReport, authorId, now));
            }
            case T10 -> currentReport;
        };
    }

    // Tạo một phiên bản báo cáo DRAFT mới cho ca chụp, có thể sao chép nội dung từ một báo cáo mẫu (khi mở lại ca đã duyệt).
    private Report newDraft(Long studyId, int version, Report template, Long createdByUserId, LocalDateTime now) {
        Report r = new Report();
        r.setStudyId(studyId);
        r.setVersion(version);
        r.setStatus(ReportStatus.DRAFT);
        r.setOrigin(template == null ? ReportOrigin.MANUAL : template.getOrigin());
        if (template != null) {
            r.setTechnique(template.getTechnique());
            r.setFindings(template.getFindings());
            r.setDiagnosis(template.getDiagnosis());
            r.setConclusion(template.getConclusion());
            r.setRecommendation(template.getRecommendation());
        }
        r.setCreatedByUserId(createdByUserId);
        r.setCreatedAt(now);
        return r;
    }

    /* ------------------------------------------------------------ Tien ich noi bo */

    // Trích các thông tin cần thiết của ca chụp để TransitionPolicy kiểm điều kiện chuyển trạng thái.
    private static TransitionPolicy.StudyFacts studyFacts(Study study) {
        return new TransitionPolicy.StudyFacts(
                study.getStudyInstanceUid(), study.getReadingUserId(), study.getApprovingUserId());
    }

    // Trích các thông tin cần thiết của báo cáo hiện hành để TransitionPolicy kiểm điều kiện chuyển trạng thái; trả null nếu chưa có báo cáo.
    private static TransitionPolicy.ReportFacts reportFacts(Report report) {
        return report == null ? null
                : new TransitionPolicy.ReportFacts(report.getTechnique(), report.getConclusion(), report.getCreatedByUserId());
    }

    // Chuyển một vi phạm điều kiện chuyển trạng thái (TransitionPolicy.Violation) thành ApiException với mã lỗi HTTP tương ứng.
    private static ApiException toApiException(TransitionPolicy.Violation violation) {
        HttpStatus status = switch (violation.kind()) {
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return new ApiException(status, violation.code(), violation.message());
    }

    // Chuyển một dòng lịch sử chuyển trạng thái (entity) sang DTO, kèm thông tin người thực hiện.
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

    /** Ma buoc chuyen cho dong lich su; "T1" cho dong tao ca (from_status NULL); null neu du lieu la. */
    // Suy ra mã bước chuyển (T1..T10) cho một dòng lịch sử, dựa trên trạng thái trước/sau; trả null nếu dữ liệu lịch sử không hợp lệ.
    private String resolveHistoryTransitionCode(StudyStatusHistory h) {
        StudyStatus from = h.getFromStatus();
        StudyStatus to = h.getToStatus();
        if (from == null && to == StudyStatus.SCHEDULED) {
            return "T1";
        }
        TransitionCode code = StudyStateMachine.resolveTransitionCode(from, to);
        return code == null ? null : code.name();
    }

}
