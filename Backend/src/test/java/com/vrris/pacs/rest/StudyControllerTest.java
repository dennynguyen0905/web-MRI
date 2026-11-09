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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private Long studyId;
    private Long patientId;
    private Long orderId;
    private Long serviceItemId;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));

        patientId = savePatient("TEST0001").getId();
        Long departmentId = saveDepartment("K01").getId();
        serviceItemId = saveServiceItem("CT_SO_NAO").getId();
        orderId = saveOrder("PH20261031001", patientId, departmentId).getId();
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
    void getViewerLink_thanh_cong_ghi_audit_log_VIEW_IMAGE() throws Exception {
        Study study = saveStudyWithUid("2026103100002", orderId, serviceItemId, "READ", "1.2.826.0.1.3680043.8.498.10000001");

        mockMvc.perform(get("/studies/" + study.getId() + "/viewer-link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study_id").value(study.getId()))
                .andExpect(jsonPath("$.patient_id").value(patientId))
                .andExpect(jsonPath("$.study_instance_uid").value("1.2.826.0.1.3680043.8.498.10000001"))
                .andExpect(jsonPath("$.viewer_url").value("http://localhost:8043/viewer/1.2.826.0.1.3680043.8.498.10000001"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'VIEW_IMAGE' AND entity_type = 'STUDY' "
                        + "AND entity_id = ? AND patient_id = ? AND actor_username = 'admin'",
                Integer.class, study.getId(), patientId);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void getViewerLink_tra_422_khi_chua_co_anh_pacs() throws Exception {
        mockMvc.perform(get("/studies/" + studyId + "/viewer-link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("STUDY_NO_DICOM_IMAGE"));
    }

    @Test
    void getViewerLink_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/" + studyId + "/viewer-link"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getViewerLink_tra_403_khi_vai_tro_khong_hop_le() throws Exception {
        String guestToken = jwtService.generateToken("guest", List.of("GUEST"));
        mockMvc.perform(get("/studies/" + studyId + "/viewer-link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getViewerLink_cac_vai_tro_duoc_phep_deu_mo_duoc_viewer() throws Exception {
        Study study = saveStudyWithUid("2026103100003", orderId, serviceItemId, "READ", "1.2.826.0.1.3680043.8.498.10000002");

        List<String> allowedRoles = List.of("CLINICIAN", "TECHNICIAN", "RADIOLOGIST", "APPROVER");
        for (String role : allowedRoles) {
            String token = jwtService.generateToken("user_" + role.toLowerCase(), List.of(role));
            mockMvc.perform(get("/studies/" + study.getId() + "/viewer-link")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.study_instance_uid").value("1.2.826.0.1.3680043.8.498.10000002"));
        }
    }

    private Study saveStudyWithUid(String accessionNumber, Long orderId, Long serviceItemId, String status, String studyInstanceUid) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus(status);
        study.setStudyInstanceUid(studyInstanceUid);
        LocalDateTime now = LocalDateTime.now();
        study.setScheduledAt(now);
        study.setCreatedAt(now);
        study.setUpdatedAt(now);
        return studyDao.save(study);
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
