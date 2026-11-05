package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.model.Patient;
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
 * 4 endpoint /patients (doc/api/openapi.yaml): danh sach/tim, xem, tao (chan trung patient_code -
 * IN-01), sua. Cung mau voi DepartmentControllerTest - MockMvc that qua filter chain JWT thuc,
 * MySQL thuc qua Testcontainers (AbstractIntegrationTest).
 */
@Transactional
class PatientControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String radiologistToken;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));
        radiologistToken = jwtService.generateToken("bs.cdha", List.of("RADIOLOGIST"));

        save("TEST0001", "Nguyen Van Gia A", "GIA0000000001");
        save("TEST0002", "Le Thi Gia B", "GIA0000000002");
    }

    @Test
    void list_tra_danh_sach_theo_q() throws Exception {
        mockMvc.perform(get("/patients").param("q", "Nguyen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].patient_code").value("TEST0001"));
    }

    @Test
    void list_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void get_tra_benh_nhan_dung_id() throws Exception {
        Long id = patientDao.findByPatientCode("TEST0001").orElseThrow().getId();

        mockMvc.perform(get("/patients/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_code").value("TEST0001"))
                .andExpect(jsonPath("$.full_name").value("Nguyen Van Gia A"));
    }

    @Test
    void get_tra_404_khi_khong_tim_thay() throws Exception {
        mockMvc.perform(get("/patients/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_NOT_FOUND"));
    }

    @Test
    void create_thanh_cong_voi_du_truong_bat_buoc() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_code", "TEST0010");
        body.put("full_name", "Tran Van Gia D");
        body.put("date_of_birth", "1990-05-20");
        body.put("dob_precision", "DAY");
        body.put("gender", "M");

        mockMvc.perform(post("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient_code").value("TEST0010"))
                .andExpect(jsonPath("$.age").value(org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.age_approximate").value(false));
    }

    @Test
    void create_tra_409_khi_trung_patient_code() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_code", "TEST0001");
        body.put("full_name", "Ten Khac");
        body.put("dob_precision", "YEAR");
        body.put("gender", "F");

        mockMvc.perform(post("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_DUPLICATE_CODE"));
    }

    @Test
    void create_tra_409_khi_nghi_trung_nguoi_qua_bhxh() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_code", "TEST0011");
        body.put("full_name", "Benh Nhan Moi");
        body.put("dob_precision", "YEAR");
        body.put("gender", "M");
        body.put("social_insurance_no", "GIA0000000001");

        mockMvc.perform(post("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_SUSPECTED_DUPLICATE"));
    }

    @Test
    void create_tra_400_khi_thieu_truong_bat_buoc() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("full_name", "Thieu Ma Benh Nhan");
        body.put("dob_precision", "YEAR");
        body.put("gender", "F");

        mockMvc.perform(post("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_tra_403_khi_sai_vai_tro() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_code", "TEST0012");
        body.put("full_name", "Benh Nhan Khac");
        body.put("dob_precision", "YEAR");
        body.put("gender", "M");

        mockMvc.perform(post("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void update_sua_tung_phan_khong_doi_truong_khong_gui() throws Exception {
        Long id = patientDao.findByPatientCode("TEST0002").orElseThrow().getId();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", "0911111111");

        mockMvc.perform(patch("/patients/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0911111111"))
                .andExpect(jsonPath("$.full_name").value("Le Thi Gia B"));
    }

    @Test
    void update_tra_404_khi_khong_tim_thay() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", "0911111111");

        mockMvc.perform(patch("/patients/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_NOT_FOUND"));
    }

    private Patient save(String code, String fullName, String socialInsuranceNo) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFullName(fullName);
        p.setDobPrecision("DAY");
        p.setGender("U");
        p.setSocialInsuranceNo(socialInsuranceNo);
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return patientDao.save(p);
    }
}
