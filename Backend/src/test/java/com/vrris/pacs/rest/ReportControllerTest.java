package com.vrris.pacs.rest;

import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /studies/{studyId}/reports (IN-10) - xem Javadoc rest.ReportController/service.ReportService.
 *
 * "Mo lai ca da duyet (T9) tao version bao cao moi": T9 (chuyen trang thai APPROVED -> READING) la
 * viec cua tinh nang chuyen trang thai (chua co, KAN-B9) - IN-10 KHONG tao version qua endpoint
 * nay (xem Javadoc saveCurrentReportDraft). multiVersion_* duoi day MO PHONG trang thai SAU KHI
 * T9 da xay ra (seed truc tiep 2 ban ghi report qua ReportDao, giong cach T8/T9 se tao) de kiem
 * GET/PUT xu ly dung nhieu version: ban APPROVED cu khong doi, ban DRAFT moi la "hien hanh" va chi
 * ban nay bi PUT sua (khop SM-04, doc/03-state-machine.md).
 */
@Transactional
class ReportControllerTest extends AbstractIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

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
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long patientId;
    private Long orderId;
    private Long studyId;
    private Long holderUserId;
    private String holderToken;

    @BeforeEach
    void seed() {
        patientId = savePatient("TEST0001", "Benh nhan TEST0001").getId();
        Long departmentId = saveDepartment("K01").getId();
        Long serviceItemId = saveServiceItem("CT_SO_NAO", "CT").getId();
        orderId = saveOrder("PH20261031001", patientId, departmentId).getId();
        studyId = saveStudy("2026103100001", orderId, serviceItemId, "UNREAD", null).getId();

        AppUser holder = saveAppUser("bscdha");
        holderUserId = holder.getId();
        holderToken = jwtService.generateToken("bscdha", List.of("RADIOLOGIST"));
    }

    @Test
    void listReports_tra_moi_version_version_lon_nhat_truoc() throws Exception {
        saveReport(studyId, 1, "APPROVED", "KT v1", "KL v1", null, null);
        saveReport(studyId, 2, "DRAFT", "KT v2", "KL v2", null, null);

        mockMvc.perform(get("/studies/" + studyId + "/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].status").value("APPROVED"));
    }

    @Test
    void listReports_tra_404_khi_khong_tim_thay_study() throws Exception {
        mockMvc.perform(get("/studies/999999/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    @Test
    void listReports_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/" + studyId + "/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentReport_tra_ban_version_lon_nhat() throws Exception {
        saveReport(studyId, 1, "DRAFT", "KT v1", "KL v1", null, null);
        saveReport(studyId, 2, "FINAL", "KT v2", "KL v2", null, null);

        mockMvc.perform(get("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.technique").value("KT v2"));
    }

    @Test
    void getCurrentReport_tra_404_khi_study_chua_co_bao_cao() throws Exception {
        mockMvc.perform(get("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void put_thanh_cong_khi_dung_nguoi_dang_giu_ca_va_ghi_audit_co_before_after() throws Exception {
        setStudyReadingBy(holderUserId);
        Report report = saveReport(studyId, 1, "DRAFT", "KT cu", "KL cu", null, null);

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"technique\":\"KT moi\",\"findings\":\"MT moi\",\"diagnosis\":\"CD moi\","
                                + "\"conclusion\":\"KL moi\",\"recommendation\":\"KN moi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.technique").value("KT moi"))
                .andExpect(jsonPath("$.conclusion").value("KL moi"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT before_value, after_value, entity_id, patient_id, actor_username FROM audit_log "
                        + "WHERE action = 'UPDATE' AND entity_type = 'REPORT' ORDER BY id DESC LIMIT 1");
        assertThat(row.get("entity_id")).isEqualTo(report.getId());
        assertThat(row.get("patient_id")).isEqualTo(patientId);
        assertThat(row.get("actor_username")).isEqualTo("bscdha");
        String before = (String) row.get("before_value");
        String after = (String) row.get("after_value");
        assertThat(before).contains("KT cu");
        assertThat(after).contains("KT moi");
        assertThat(before).isNotEqualTo(after);
    }

    @Test
    void put_tra_403_khi_APPROVER_goi() throws Exception {
        setStudyReadingBy(holderUserId);
        saveReport(studyId, 1, "DRAFT", null, null, null, null);
        String approverToken = jwtService.generateToken("bsduyet", List.of("APPROVER"));

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + approverToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void put_tra_403_khi_ADMIN_goi() throws Exception {
        setStudyReadingBy(holderUserId);
        saveReport(studyId, 1, "DRAFT", null, null, null, null);
        String adminToken = jwtService.generateToken("admin", List.of("ADMIN"));

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void put_tra_403_khi_RADIOLOGIST_khac_khong_giu_ca_goi() throws Exception {
        setStudyReadingBy(holderUserId);
        saveReport(studyId, 1, "DRAFT", null, null, null, null);
        saveAppUser("bscdha2");
        String otherToken = jwtService.generateToken("bscdha2", List.of("RADIOLOGIST"));

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_HOLDER"));
    }

    @Test
    void put_tra_409_khi_ca_khong_o_READING() throws Exception {
        // reading_user_id van la holder (vi du con lai tu truoc) nhung status khong con la READING.
        Study study = studyDao.findById(studyId).orElseThrow();
        study.setStatus("READ");
        study.setReadingUserId(holderUserId);
        studyDao.save(study);
        saveReport(studyId, 1, "DRAFT", null, null, null, null);

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_READING"));
    }

    @Test
    void put_tra_409_khi_bao_cao_hien_hanh_khong_con_la_DRAFT() throws Exception {
        setStudyReadingBy(holderUserId);
        Long approverId = saveAppUser("bsduyet").getId();
        saveReport(studyId, 1, "APPROVED", "KT", "KL", approverId, LocalDateTime.now());

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_DRAFT"));
    }

    @Test
    void put_tra_404_khi_study_chua_co_bao_cao() throws Exception {
        setStudyReadingBy(holderUserId);

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void put_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void multiVersion_mo_phong_T9_ban_APPROVED_cu_giu_nguyen_chi_ban_DRAFT_moi_bi_sua() throws Exception {
        setStudyReadingBy(holderUserId);
        Long approverId = saveAppUser("bsduyet").getId();
        Report approved = saveReport(studyId, 1, "APPROVED", "KT v1", "KL v1", approverId, LocalDateTime.now());
        Report draft = saveReport(studyId, 2, "DRAFT", null, null, null, null);

        mockMvc.perform(get("/studies/" + studyId + "/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].status").value("APPROVED"));

        mockMvc.perform(get("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(put("/studies/" + studyId + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + holderToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"technique\":\"KT v2 sua\",\"conclusion\":\"KL v2 sua\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        Report approvedAfter = reportDao.findById(approved.getId()).orElseThrow();
        assertThat(approvedAfter.getStatus()).isEqualTo("APPROVED");
        assertThat(approvedAfter.getTechnique()).isEqualTo("KT v1");
        assertThat(approvedAfter.getApprovedByUserId()).isEqualTo(approverId);

        Report draftAfter = reportDao.findById(draft.getId()).orElseThrow();
        assertThat(draftAfter.getTechnique()).isEqualTo("KT v2 sua");
        assertThat(draftAfter.getConclusion()).isEqualTo("KL v2 sua");
    }

    private void setStudyReadingBy(Long userId) {
        Study study = studyDao.findById(studyId).orElseThrow();
        study.setStatus("READING");
        study.setReadingUserId(userId);
        studyDao.save(study);
    }

    private AppUser saveAppUser(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz12");
        user.setFullName("Nguoi dung " + username);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return appUserDao.save(user);
    }

    private Report saveReport(Long studyId, int version, String status, String technique, String conclusion,
                               Long approvedByUserId, LocalDateTime approvedAt) {
        Report report = new Report();
        report.setStudyId(studyId);
        report.setVersion(version);
        report.setStatus(status);
        report.setOrigin("MANUAL");
        report.setTechnique(technique);
        report.setConclusion(conclusion);
        report.setCreatedByUserId(holderUserId);
        report.setCreatedAt(LocalDateTime.now());
        report.setApprovedByUserId(approvedByUserId);
        report.setApprovedAt(approvedAt);
        return reportDao.save(report);
    }

    private Study saveStudy(String accessionNumber, Long orderId, Long serviceItemId, String status,
                             Long readingUserId) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus(status);
        study.setReadingUserId(readingUserId);
        LocalDateTime now = LocalDateTime.now();
        study.setScheduledAt(now);
        study.setCreatedAt(now);
        study.setUpdatedAt(now);
        return studyDao.save(study);
    }

    private ImagingOrder saveOrder(String orderCode, Long patientId, Long departmentId) {
        ImagingOrder order = new ImagingOrder();
        order.setOrderCode(orderCode);
        order.setPatientId(patientId);
        order.setSource("MANUAL");
        order.setSyncStatus("NOT_SYNCED");
        order.setOrderingDepartmentId(departmentId);
        order.setOrderingDoctorName("BS Test");
        order.setPatientClass("OUTPATIENT");
        LocalDateTime now = LocalDateTime.now();
        order.setOrderedAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return imagingOrderDao.save(order);
    }

    private Patient savePatient(String code, String fullName) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFullName(fullName);
        p.setDobPrecision("DAY");
        p.setGender("U");
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return patientDao.save(p);
    }

    private Department saveDepartment(String code) {
        Department d = new Department();
        d.setCode(code);
        d.setName("Khoa " + code);
        d.setType("KHOA");
        d.setSortOrder(1);
        d.setIsActive(true);
        return departmentDao.save(d);
    }

    private ServiceItem saveServiceItem(String code, String modality) {
        ServiceItem s = new ServiceItem();
        s.setCode(code);
        s.setName("Chup " + code);
        s.setModality(modality);
        s.setIsActive(true);
        return serviceItemDao.save(s);
    }
}
