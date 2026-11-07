package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /studies (ban rut gon - xem rest.StudyController). Cung mau voi PatientControllerTest: MockMvc
 * that qua filter chain JWT thuc, MySQL thuc qua Testcontainers (AbstractIntegrationTest).
 */
@Transactional
class StudyControllerTest extends AbstractIntegrationTest {

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
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private Long patientId;
    private Long studyId;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));

        patientId = savePatient("TEST0001").getId();
        Long departmentId = saveDepartment("K01").getId();
        Long serviceItemId = saveServiceItem("CT_SO_NAO").getId();
        Long orderId = saveOrder("PH20261031001", patientId, departmentId).getId();
        studyId = saveStudy("2026103100001", orderId, serviceItemId, "SCHEDULED").getId();
    }

    @Test
    void get_tra_dung_ca_chup() throws Exception {
        mockMvc.perform(get("/studies/" + studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accession_number").value("2026103100001"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void get_tra_404_khi_khong_tim_thay() throws Exception {
        mockMvc.perform(get("/studies/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    @Test
    void list_loc_theo_status() throws Exception {
        mockMvc.perform(get("/studies").param("status", "SCHEDULED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100001"));
    }

    @Test
    void list_tra_400_khi_status_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies").param("status", "KHONG_HOP_LE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void export_studies_tra_file_csv_va_ghi_audit_log_EXPORT() throws Exception {
        byte[] bytes = mockMvc.perform(get("/studies/export")
                        .param("status", "SCHEDULED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("danh-sach-ca-chup_")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFSTT,Mã hồ sơ,Trạng thái,");
        assertThat(csv).contains("2026103100001,SCHEDULED,PH20261031001");
        assertThat(csv).contains("TEST0001,Benh nhan TEST0001");
        // Chua co ngay sinh -> tuoi hien thi "—", khong phai "0"
        assertThat(csv).contains(",—,U,");

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'EXPORT' AND entity_type = 'STUDY' "
                        + "AND patient_id = ? AND actor_username = 'admin'",
                Integer.class, patientId);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void export_studies_tra_400_khi_status_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies/export")
                        .param("status", "SAI_TRANG_THAI")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void export_studies_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/export"))
                .andExpect(status().isUnauthorized());
    }

    private Study saveStudy(String accessionNumber, Long orderId, Long serviceItemId, String status) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus(status);
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
