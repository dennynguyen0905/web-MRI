package com.vrris.pacs.rest;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /patients/{id}/print-documents va /print-events (Task 5). Cung mau voi StudyControllerTest:
 * MockMvc that qua filter chain JWT thuc, MySQL thuc qua Testcontainers.
 *
 * Cac test dung PDF chi chay khi may co font TTF Unicode (xem service.impl.PdfPrintRenderer);
 * khong co font thi bo qua, khong bao do.
 */
@Transactional
class PrintControllerTest extends AbstractIntegrationTest {

    private static final List<String> FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf");

    private static boolean hasFont() {
        return FONT_CANDIDATES.stream().anyMatch(p -> Files.isReadable(Path.of(p)));
    }

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
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String radiologistToken;
    private Long patientId;
    private Long otherPatientId;
    private Long studyId;

    @BeforeEach
    void seed() {
        radiologistToken = jwtService.generateToken("bscdha", List.of("RADIOLOGIST"));

        patientId = savePatient("TEST0001").getId();
        otherPatientId = savePatient("TEST0002").getId();
        Long departmentId = saveDepartment("K01").getId();
        Long serviceItemId = saveServiceItem("CT_SO_NAO").getId();
        Long orderId = saveOrder("PH20261031001", patientId, departmentId).getId();
        studyId = saveStudy("2026103100001", orderId, serviceItemId).getId();
        saveReport(studyId, saveAppUser("bscdha"));
    }

    /**
     * Flyway trong test chi chay toi V6 (xem src/test/resources/application.yml) nen bang app_user
     * rong - phai tu them mot nguoi dung truoc khi insert report, vi report.created_by_user_id co
     * khoa ngoai toi app_user. Chua co model.AppUser nen them bang SQL.
     */
    private Long saveAppUser(String username) {
        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, full_name, email) VALUES (?, ?, ?, ?)",
                username, "khong-dung-trong-test", "BS. CDHA (test)", username + "@vr-ris.local");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    @Test
    void tai_duoc_mot_phan_phieu_dang_pdf() throws Exception {
        assumeTrue(hasFont(), "May chay test chua co font TTF Unicode - bo qua");

        mockMvc.perform(get("/patients/" + patientId + "/print-documents")
                        .param("study_id", String.valueOf(studyId))
                        .param("section", "FUNCTIONAL")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("chan-doan-chuc-nang")));
    }

    @Test
    void phan_chua_co_du_lieu_van_tra_file_chu_khong_bao_loi() throws Exception {
        assumeTrue(hasFont(), "May chay test chua co font TTF Unicode - bo qua");

        mockMvc.perform(get("/patients/" + patientId + "/print-documents")
                        .param("study_id", String.valueOf(studyId))
                        .param("section", "DRUGS")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void khong_in_duoc_ca_chup_cua_benh_nhan_khac() throws Exception {
        mockMvc.perform(get("/patients/" + otherPatientId + "/print-documents")
                        .param("study_id", String.valueOf(studyId))
                        .param("section", "FUNCTIONAL")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDY_PATIENT_MISMATCH"));
    }

    @Test
    void tra_404_khi_khong_tim_thay_benh_nhan() throws Exception {
        mockMvc.perform(get("/patients/999999/print-documents")
                        .param("study_id", String.valueOf(studyId))
                        .param("section", "FUNCTIONAL")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_NOT_FOUND"));
    }

    @Test
    void tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/patients/" + patientId + "/print-documents")
                        .param("study_id", String.valueOf(studyId))
                        .param("section", "FUNCTIONAL"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ghi_nhan_lan_in_bang_trinh_duyet_tra_204() throws Exception {
        mockMvc.perform(post("/patients/" + patientId + "/print-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"study_id\":" + studyId + ",\"sections\":[\"FUNCTIONAL\",\"LABS\"]}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void moi_lan_in_deu_ghi_audit_log_PRINT_kem_patient_id() throws Exception {
        assumeTrue(hasFont(), "May chay test chua co font TTF Unicode - bo qua");

        mockMvc.perform(get("/patients/" + patientId + "/print-documents")
                        .param("study_id", String.valueOf(studyId))
                        .param("section", "FUNCTIONAL")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isOk());

        // In la mang du lieu benh nhan ra khoi he thong -> bat buoc co dong audit (IN-15,
        // doc/09 muc 2.2). Kiem ca patient_id vi do la khoa de truy vet theo benh nhan.
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'PRINT' AND patient_id = ? "
                        + "AND entity_type = 'study' AND entity_id = ? AND actor_username = 'bscdha'",
                Integer.class, patientId, studyId);

        assertThat(rows).isEqualTo(1);
    }

    @Test
    void ghi_nhan_lan_in_tra_400_khi_thieu_phan_in() throws Exception {
        mockMvc.perform(post("/patients/" + patientId + "/print-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"study_id\":" + studyId + ",\"sections\":[]}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isBadRequest());
    }

    /* ------------------------------------------------------------------ Seed */

    private Report saveReport(Long studyId, Long createdByUserId) {
        Report report = new Report();
        report.setStudyId(studyId);
        report.setVersion(1);
        report.setStatus("FINAL");
        report.setOrigin("MANUAL");
        report.setTechnique("CT so nao khong tiem thuoc (du lieu gia).");
        report.setConclusion("Chua phat hien bat thuong (du lieu gia).");
        report.setCreatedByUserId(createdByUserId);
        report.setCreatedAt(LocalDateTime.now());
        return reportDao.save(report);
    }

    private Study saveStudy(String accessionNumber, Long orderId, Long serviceItemId) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus("READ");
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

    private Patient savePatient(String code) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFullName("Benh nhan " + code);
        p.setDateOfBirth(LocalDate.of(1958, 3, 15));
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

    private ServiceItem saveServiceItem(String code) {
        ServiceItem s = new ServiceItem();
        s.setCode(code);
        s.setName("Chup " + code);
        s.setModality("CT");
        s.setIsActive(true);
        return serviceItemDao.save(s);
    }
}
