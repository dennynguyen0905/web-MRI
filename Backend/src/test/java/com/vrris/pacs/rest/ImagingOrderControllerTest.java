package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /imaging-orders (doc/api/openapi.yaml) - IN-02. Cung mau voi PatientControllerTest: MockMvc that
 * qua filter chain JWT thuc, MySQL thuc qua Testcontainers (AbstractIntegrationTest).
 */
@Transactional
class ImagingOrderControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ServiceItemDao serviceItemDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String radiologistToken;
    private Long patientId;
    private Long departmentId;
    private Long serviceItemId;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));
        radiologistToken = jwtService.generateToken("bs.cdha", List.of("RADIOLOGIST"));

        patientId = savePatient("TEST0001").getId();
        departmentId = saveDepartment("K01").getId();
        serviceItemId = saveServiceItem("CT_SO_NAO").getId();
    }

    @Test
    void create_thanh_cong_tao_chi_dinh_kem_study() throws Exception {
        Map<String, Object> study = new LinkedHashMap<>();
        study.put("service_item_id", serviceItemId);
        study.put("scheduled_at", "2026-11-10T08:00:00");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_id", patientId);
        body.put("ordering_department_id", departmentId);
        body.put("ordering_doctor_name", "BS Nguyen Van A");
        body.put("patient_class", "OUTPATIENT");
        body.put("studies", List.of(study));

        mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient_id").value(patientId))
                .andExpect(jsonPath("$.order_code").isNotEmpty())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.studies").isArray())
                .andExpect(jsonPath("$.studies[0].accession_number").isNotEmpty())
                .andExpect(jsonPath("$.studies[0].status").value("SCHEDULED"));
    }

    @Test
    void create_tra_422_khi_benh_nhan_khong_ton_tai() throws Exception {
        Map<String, Object> study = new LinkedHashMap<>();
        study.put("service_item_id", serviceItemId);
        study.put("scheduled_at", "2026-11-10T08:00:00");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_id", 999999);
        body.put("ordering_department_id", departmentId);
        body.put("ordering_doctor_name", "BS Nguyen Van A");
        body.put("patient_class", "OUTPATIENT");
        body.put("studies", List.of(study));

        mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMAGING_ORDER_PATIENT_NOT_FOUND"));
    }

    @Test
    void create_tra_400_khi_thieu_studies() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_id", patientId);
        body.put("ordering_department_id", departmentId);
        body.put("ordering_doctor_name", "BS Nguyen Van A");
        body.put("patient_class", "OUTPATIENT");
        body.put("studies", List.of());

        mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_tra_403_khi_sai_vai_tro() throws Exception {
        Map<String, Object> study = new LinkedHashMap<>();
        study.put("service_item_id", serviceItemId);
        study.put("scheduled_at", "2026-11-10T08:00:00");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_id", patientId);
        body.put("ordering_department_id", departmentId);
        body.put("ordering_doctor_name", "BS Nguyen Van A");
        body.put("patient_class", "OUTPATIENT");
        body.put("studies", List.of(study));

        mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void get_tra_404_khi_khong_tim_thay() throws Exception {
        mockMvc.perform(get("/imaging-orders/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMAGING_ORDER_NOT_FOUND"));
    }

    @Test
    void list_loc_theo_patient_id_va_get_tra_chi_tiet_kem_studies() throws Exception {
        Map<String, Object> study = new LinkedHashMap<>();
        study.put("service_item_id", serviceItemId);
        study.put("scheduled_at", "2026-11-10T08:00:00");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_id", patientId);
        body.put("ordering_department_id", departmentId);
        body.put("ordering_doctor_name", "BS Nguyen Van A");
        body.put("patient_class", "OUTPATIENT");
        body.put("studies", List.of(study));

        String response = mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        mockMvc.perform(get("/imaging-orders").param("patient_id", String.valueOf(patientId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/imaging-orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studies[0].order_id").value(orderId));
    }

    @Test
    void update_sua_tung_phan() throws Exception {
        Map<String, Object> study = new LinkedHashMap<>();
        study.put("service_item_id", serviceItemId);
        study.put("scheduled_at", "2026-11-10T08:00:00");
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("patient_id", patientId);
        createBody.put("ordering_department_id", departmentId);
        createBody.put("ordering_doctor_name", "BS Nguyen Van A");
        createBody.put("patient_class", "OUTPATIENT");
        createBody.put("studies", List.of(study));

        String response = mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("room", "P302");

        mockMvc.perform(patch("/imaging-orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("P302"))
                .andExpect(jsonPath("$.ordering_doctor_name").value("BS Nguyen Van A"));
    }

    @Test
    void addStudyToOrder_them_ca_chup_moi_vao_chi_dinh_da_co() throws Exception {
        Map<String, Object> firstStudy = new LinkedHashMap<>();
        firstStudy.put("service_item_id", serviceItemId);
        firstStudy.put("scheduled_at", "2026-11-10T08:00:00");
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("patient_id", patientId);
        createBody.put("ordering_department_id", departmentId);
        createBody.put("ordering_doctor_name", "BS Nguyen Van A");
        createBody.put("patient_class", "OUTPATIENT");
        createBody.put("studies", List.of(firstStudy));

        String response = mockMvc.perform(post("/imaging-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        Map<String, Object> secondStudy = new LinkedHashMap<>();
        secondStudy.put("service_item_id", serviceItemId);
        secondStudy.put("scheduled_at", "2026-11-12T08:00:00");

        mockMvc.perform(post("/imaging-orders/" + orderId + "/studies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondStudy)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value(orderId))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(get("/imaging-orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studies.length()").value(2));
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
