package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.enu.DepartmentType;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.ReportOrigin;
import com.vrris.pacs.enu.ReportStatus;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.enu.SyncStatus;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /studies/{id}/transitions - cac nhanh ngoai le (T4, T8, T9, T10) va cac ma loi, bo sung cho
 * RbacIntegrationTest (luong chinh T2 -> T7). Doi chieu doc/03-state-machine.md muc 2.2 va bang test
 * SM-xx muc 4.
 */
@Transactional
class StudyTransitionControllerTest extends AbstractIntegrationTest {

    private static final String UID = "1.2.840.10008.555.1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PatientDao patientDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private ServiceItemDao serviceItemDao;
    @Autowired
    private ImagingOrderDao imagingOrderDao;
    @Autowired
    private StudyDao studyDao;
    @Autowired
    private ReportDao reportDao;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private AppUser admin;
    private AppUser tech;
    private AppUser rad1;
    private AppUser rad2;
    private AppUser approver1;
    private AppUser approver2;
    private Long patientId;
    private Long orderId;
    private Long serviceItemId;
    private int accSeq;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setCode("K_TRANS");
        dept.setName("Khoa CDHA");
        dept.setType(DepartmentType.KHOA);
        dept.setSortOrder(1);
        dept.setIsActive(true);
        departmentDao.save(dept);

        admin = saveUser("admin_trans", dept.getId());
        tech = saveUser("ktv_trans", dept.getId());
        rad1 = saveUser("rad1_trans", dept.getId());
        rad2 = saveUser("rad2_trans", dept.getId());
        approver1 = saveUser("appr1_trans", dept.getId());
        approver2 = saveUser("appr2_trans", dept.getId());

        Patient patient = new Patient();
        patient.setPatientCode("BN_TRANS_01");
        patient.setFullName("Benh nhan gia lap");
        patient.setDobPrecision(DobPrecision.DAY);
        patient.setGender(Gender.U);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patientId = patientDao.save(patient).getId();

        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setCode("CT_TRANS");
        serviceItem.setName("CT so nao");
        serviceItem.setModality(Modality.CT);
        serviceItem.setIsActive(true);
        serviceItemId = serviceItemDao.save(serviceItem).getId();

        ImagingOrder order = new ImagingOrder();
        order.setOrderCode("ORD_TRANS_01");
        order.setPatientId(patientId);
        order.setOrderingDepartmentId(dept.getId());
        order.setOrderingDoctorName("BS Lam Sang");
        order.setPatientClass(PatientClass.OUTPATIENT);
        order.setSource(OrderSource.MANUAL);
        order.setSyncStatus(SyncStatus.NOT_SYNCED);
        order.setOrderedAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderId = imagingOrderDao.save(order).getId();
    }

    /* ------------------------------------------------------------------ T2, T3 */

    @Test
    void t2_dat_completed_at_ket_thuc_chup() throws Exception {
        Study study = createStudy(StudyStatus.SCHEDULED, UID, null, null);

        transition(study, tech, StudyStatus.UNREAD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("UNREAD"))
                .andExpect(jsonPath("$.study.completed_at").exists())
                .andExpect(jsonPath("$.history_entry.transition_code").value("T2"));
    }

    @Test
    void t3_tao_ban_nhap_v1_dat_nguoi_doc_va_khong_dat_started_at() throws Exception {
        Study study = createStudy(StudyStatus.UNREAD, UID, null, null);

        transition(study, rad1, StudyStatus.READING, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("READING"))
                .andExpect(jsonPath("$.study.reading_user_id").value(rad1.getId()))
                .andExpect(jsonPath("$.study.started_at").doesNotExist())
                .andExpect(jsonPath("$.report.version").value(1))
                .andExpect(jsonPath("$.report.status").value("DRAFT"));
    }

    @Test
    void t3_ca_dang_bi_bac_si_khac_khoa_tra_409() throws Exception {
        Study study = createStudy(StudyStatus.UNREAD, UID, rad1.getId(), null);

        transition(study, rad2, StudyStatus.READING, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_LOCKED_BY_ANOTHER"));
    }

    /* ------------------------------------------------------------------ T4 */

    @Test
    void t4_nguoi_dang_doc_bo_do_nha_khoa_va_giu_ban_nhap() throws Exception {
        Study study = createStudy(StudyStatus.READING, UID, rad1.getId(), null);
        createReport(study.getId(), 1, ReportStatus.DRAFT, "Ky thuat", null, rad1.getId());

        transition(study, rad1, StudyStatus.UNREAD, "Can hoi chan them")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("UNREAD"))
                .andExpect(jsonPath("$.study.reading_user_id").doesNotExist())
                .andExpect(jsonPath("$.report.version").value(1))
                .andExpect(jsonPath("$.report.status").value("DRAFT"))
                .andExpect(jsonPath("$.report.technique").value("Ky thuat"))
                .andExpect(jsonPath("$.history_entry.reason").value("Can hoi chan them"));
    }

    @Test
    void t4_admin_giai_phong_khoa_ca_bac_si_khac() throws Exception {
        Study study = createStudy(StudyStatus.READING, UID, rad1.getId(), null);

        transition(study, admin, StudyStatus.UNREAD, "Bac si vang mat")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.reading_user_id").doesNotExist());
    }

    @Test
    void t4_ly_do_toan_khoang_trang_tra_422() throws Exception {
        Study study = createStudy(StudyStatus.READING, UID, rad1.getId(), null);

        transition(study, rad1, StudyStatus.UNREAD, "   ")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSITION_REASON_REQUIRED"));
    }

    /* ------------------------------------------------------------------ T5 */

    @Test
    void t5_bao_cao_thieu_ket_luan_tra_422() throws Exception {
        Study study = createStudy(StudyStatus.READING, UID, rad1.getId(), null);
        createReport(study.getId(), 1, ReportStatus.DRAFT, "Ky thuat", "   ", rad1.getId());

        transition(study, rad1, StudyStatus.READ, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REPORT_INCOMPLETE"));
    }

    /* ------------------------------------------------------------------ T7 */

    @Test
    void t7_khong_co_bao_cao_tra_422() throws Exception {
        Study study = createStudy(StudyStatus.APPROVING, UID, rad1.getId(), approver1.getId());

        transition(study, approver1, StudyStatus.APPROVED, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REPORT_INCOMPLETE"));
    }

    @Test
    void t7_duyet_bao_cao_va_khong_ghi_de_completed_at() throws Exception {
        LocalDateTime imagedAt = LocalDateTime.of(2026, 11, 1, 9, 30);
        Study study = createStudy(StudyStatus.APPROVING, UID, rad1.getId(), approver1.getId());
        study.setCompletedAt(imagedAt);
        studyDao.save(study);
        createReport(study.getId(), 1, ReportStatus.FINAL, "Ky thuat", "Ket luan", rad1.getId());

        transition(study, approver1, StudyStatus.APPROVED, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("APPROVED"))
                .andExpect(jsonPath("$.study.completed_at").value("2026-11-01T09:30:00"))
                .andExpect(jsonPath("$.report.status").value("APPROVED"))
                .andExpect(jsonPath("$.report.approved_by_user_id").value(approver1.getId()));
    }

    /* ------------------------------------------------------------------ T8 */

    @Test
    void t8_nguoi_dang_duyet_tra_lai_bao_cao_RETURNED() throws Exception {
        Study study = createStudy(StudyStatus.APPROVING, UID, rad1.getId(), approver1.getId());
        createReport(study.getId(), 1, ReportStatus.FINAL, "Ky thuat", "Ket luan", rad1.getId());

        transition(study, approver1, StudyStatus.READING, "Thieu mo ta ton thuong")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("READING"))
                .andExpect(jsonPath("$.study.approving_user_id").doesNotExist())
                .andExpect(jsonPath("$.study.reading_user_id").value(rad1.getId()))
                .andExpect(jsonPath("$.report.status").value("RETURNED"));
    }

    @Test
    void t8_approver_khac_khong_giu_duyet_tra_403() throws Exception {
        Study study = createStudy(StudyStatus.APPROVING, UID, rad1.getId(), approver1.getId());
        createReport(study.getId(), 1, ReportStatus.FINAL, "Ky thuat", "Ket luan", rad1.getId());

        transition(study, approver2, StudyStatus.READING, "Tra lai")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_APPROVING_HOLDER"));
    }

    /* ------------------------------------------------------------------ T9 */

    @Test
    void t9_mo_lai_tao_version_moi_copy_noi_dung_giu_ban_APPROVED_va_completed_at() throws Exception {
        LocalDateTime imagedAt = LocalDateTime.of(2026, 11, 2, 10, 0);
        Study study = createStudy(StudyStatus.APPROVED, UID, rad1.getId(), approver1.getId());
        study.setCompletedAt(imagedAt);
        studyDao.save(study);
        Report approved = createReport(study.getId(), 1, ReportStatus.APPROVED, "Ky thuat v1", "Ket luan v1", rad1.getId());
        approved.setApprovedByUserId(approver1.getId());
        approved.setApprovedAt(LocalDateTime.now());
        reportDao.save(approved);

        transition(study, admin, StudyStatus.READING, "Phat hien sai ket luan")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("READING"))
                .andExpect(jsonPath("$.study.approving_user_id").doesNotExist())
                .andExpect(jsonPath("$.study.completed_at").value("2026-11-02T10:00:00"))
                .andExpect(jsonPath("$.report.version").value(2))
                .andExpect(jsonPath("$.report.status").value("DRAFT"))
                .andExpect(jsonPath("$.report.technique").value("Ky thuat v1"))
                .andExpect(jsonPath("$.report.conclusion").value("Ket luan v1"))
                .andExpect(jsonPath("$.report.created_by_user_id").value(rad1.getId()))
                .andExpect(jsonPath("$.report.approved_by_user_id").doesNotExist())
                .andExpect(jsonPath("$.report.approved_at").doesNotExist());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM report WHERE study_id = ? AND version = 1", String.class, study.getId()))
                .isEqualTo("APPROVED");
    }

    @Test
    void t9_thieu_ly_do_tra_422() throws Exception {
        Study study = createStudy(StudyStatus.APPROVED, UID, rad1.getId(), null);

        transition(study, approver1, StudyStatus.READING, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSITION_REASON_REQUIRED"));
    }

    /* ------------------------------------------------------------------ T10 + audit + lich su */

    @Test
    void t10_huy_ca_ghi_audit_STATUS_CHANGE_va_lich_su_dung_noi_dung() throws Exception {
        Study study = createStudy(StudyStatus.SCHEDULED, null, null, null);

        transition(study, tech, StudyStatus.CANCELLED, "Benh nhan khong den")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("CANCELLED"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'STATUS_CHANGE' AND entity_type = 'STUDY' "
                        + "AND entity_id = ? AND patient_id = ? AND actor_username = ?",
                Integer.class, study.getId(), patientId, tech.getUsername());
        assertThat(auditCount).isEqualTo(1);

        mockMvc.perform(get("/studies/" + study.getId() + "/status-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tech)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].from_status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].to_status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].transition_code").value("T10"))
                .andExpect(jsonPath("$[0].reason").value("Benh nhan khong den"))
                .andExpect(jsonPath("$[0].changed_by.id").value(tech.getId()))
                .andExpect(jsonPath("$[0].changed_by.username").value(tech.getUsername()));
    }

    @Test
    void cancelled_la_trang_thai_cuoi_moi_buoc_chuyen_tra_409() throws Exception {
        Study study = createStudy(StudyStatus.CANCELLED, null, null, null);

        for (StudyStatus to : StudyStatus.values()) {
            transition(study, admin, to, "thu")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        }
    }

    @Test
    void cap_trang_thai_khong_hop_le_tra_409_INVALID_TRANSITION() throws Exception {
        Study study = createStudy(StudyStatus.SCHEDULED, UID, null, null);

        transition(study, tech, StudyStatus.READ, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void trang_thai_luu_sai_trong_csdl_tra_loi_co_kiem_soat() throws Exception {
        // Model map study.status sang enum (@Enumerated) nen ung dung khong ghi duoc gia tri la; chi du
        // lieu sua tay trong CSDL moi lech. Ghi thang bang JDBC roi bo cache cap 1 de Hibernate doc lai.
        Study study = createStudy(StudyStatus.SCHEDULED, UID, null, null);
        jdbcTemplate.update("UPDATE study SET status = 'KHONG_HOP_LE' WHERE id = ?", study.getId());
        entityManager.flush();
        entityManager.clear();

        transition(study, tech, StudyStatus.UNREAD, null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void loi_dieu_kien_khong_ghi_audit_va_khong_ghi_lich_su() throws Exception {
        Study study = createStudy(StudyStatus.SCHEDULED, null, null, null);

        transition(study, tech, StudyStatus.UNREAD, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("STUDY_NO_DICOM_IMAGE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM study_status_history WHERE study_id = ?", Integer.class, study.getId()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'STATUS_CHANGE' AND entity_id = ?",
                Integer.class, study.getId()))
                .isZero();
    }

    /* ------------------------------------------------------------------ Tien ich */

    private ResultActions transition(Study study, AppUser user, StudyStatus to, String reason) throws Exception {
        return mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransitionRequest(to, reason, null))));
    }

    private String bearer(AppUser user) {
        String role = switch (user.getUsername()) {
            case "admin_trans" -> "ADMIN";
            case "ktv_trans" -> "TECHNICIAN";
            case "rad1_trans", "rad2_trans" -> "RADIOLOGIST";
            default -> "APPROVER";
        };
        return "Bearer " + jwtService.generateToken(user.getId(), user.getUsername(), List.of(role));
    }

    private AppUser saveUser(String username, Long deptId) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz12");
        user.setFullName("Nguoi dung " + username);
        user.setEmail(username + "@vrris.vn");
        user.setDepartmentId(deptId);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return appUserDao.save(user);
    }

    private Study createStudy(StudyStatus status, String uid, Long readingUserId, Long approvingUserId) {
        Study study = new Study();
        study.setAccessionNumber("ACC_TRANS_" + (++accSeq));
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus(status);
        study.setStudyInstanceUid(uid == null ? null : uid + "." + accSeq);
        study.setReadingUserId(readingUserId);
        study.setApprovingUserId(approvingUserId);
        study.setScheduledAt(LocalDateTime.now());
        study.setCreatedAt(LocalDateTime.now());
        study.setUpdatedAt(LocalDateTime.now());
        return studyDao.save(study);
    }

    private Report createReport(Long studyId, int version, ReportStatus status, String technique, String conclusion,
                                Long createdBy) {
        Report r = new Report();
        r.setStudyId(studyId);
        r.setVersion(version);
        r.setStatus(status);
        r.setOrigin(ReportOrigin.MANUAL);
        r.setTechnique(technique);
        r.setConclusion(conclusion);
        r.setCreatedByUserId(createdBy);
        r.setCreatedAt(LocalDateTime.now());
        return reportDao.save(r);
    }
}
