package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.dao.IdentityConflictDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.model.AuditLog;
import com.vrris.pacs.model.IdentityConflict;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /identity-conflicts (doc/api/openapi.yaml) - IN-05 (04-his-ris-reconciliation.md muc 2, 4;
 * doc/06-rbac-matrix.md RBAC-11; RC-15/RC-18 cua 04-his-ris-reconciliation.md muc 7). Cung mau voi
 * ImagingOrderControllerTest: MockMvc thuc qua filter chain JWT thuc, MySQL thuc qua Testcontainers.
 */
@Transactional
class IdentityConflictControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private IdentityConflictDao identityConflictDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String technicianToken;
    private String radiologistToken;
    private Long patientId;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));
        technicianToken = jwtService.generateToken("ky.thuat", List.of("TECHNICIAN"));
        radiologistToken = jwtService.generateToken("bs.cdha", List.of("RADIOLOGIST"));
        saveAppUser("admin", "Quan tri (test)");

        patientId = savePatient("TEST0001", "Nguyen Van Gia A").getId();
    }

    /* -------------------------------------------------- GET /identity-conflicts */

    @Test
    void list_tra_200_va_loc_theo_resolution() throws Exception {
        saveConflict(patientId, "full_name", "Nguyen Van Gia A", "Nguyen Van Gia B", "PENDING");
        IdentityConflict resolved = saveConflict(patientId, "address", "Dia chi A (gia)", "Dia chi B (gia)", "KEEP_RIS");
        resolved.setResolvedAt(LocalDateTime.now());
        identityConflictDao.save(resolved);

        mockMvc.perform(get("/identity-conflicts").param("resolution", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].field_name").value("full_name"))
                .andExpect(jsonPath("$.items[0].resolution").value("PENDING"));
    }

    @Test
    void list_tra_403_khi_sai_vai_tro() throws Exception {
        mockMvc.perform(get("/identity-conflicts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void list_ghi_audit_log_READ() throws Exception {
        mockMvc.perform(get("/identity-conflicts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        boolean audited = auditLogDao.findAll().stream().anyMatch(log ->
                "READ".equals(log.getAction()) && "IDENTITY_CONFLICT".equals(log.getEntityType())
                        && "admin".equals(log.getActorUsername()));
        assertThat(audited).isTrue();
    }

    /* -------------------------------------------------- GET /identity-conflicts/{id} */

    @Test
    void get_tra_404_khi_khong_tim_thay() throws Exception {
        mockMvc.perform(get("/identity-conflicts/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IDENTITY_CONFLICT_NOT_FOUND"));
    }

    @Test
    void get_tra_200_kem_du_lieu_benh_nhan() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "date_of_birth", "1958-03-15", "1952", "PENDING");

        mockMvc.perform(get("/identity-conflicts/" + conflict.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_id").value(patientId))
                .andExpect(jsonPath("$.ris_value").value("1958-03-15"))
                .andExpect(jsonPath("$.his_value").value("1952"))
                .andExpect(jsonPath("$.resolution").value("PENDING"));
    }

    @Test
    void get_tra_403_khi_sai_vai_tro() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "gender", "F", "M", "PENDING");

        mockMvc.perform(get("/identity-conflicts/" + conflict.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    /* -------------------------------------------------- POST /identity-conflicts/{id}/resolution */

    @Test
    void resolve_keepRis_khong_doi_patient_va_ghi_audit_log() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "full_name", "Nguyen Van Gia A", "Nguyen Van Gia B", "PENDING");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "KEEP_RIS");

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolution").value("KEEP_RIS"))
                .andExpect(jsonPath("$.resolved_by.username").value("admin"))
                .andExpect(jsonPath("$.resolved_at").isNotEmpty());

        Patient patient = patientDao.findById(patientId).orElseThrow();
        assertThat(patient.getFullName()).isEqualTo("Nguyen Van Gia A");
    }

    @Test
    void resolve_takeHis_doi_patient_va_ghi_before_after() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "full_name", "Nguyen Van Gia A", "Nguyen Van Gia B", "PENDING");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "TAKE_HIS");

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolution").value("TAKE_HIS"));

        Patient patient = patientDao.findById(patientId).orElseThrow();
        assertThat(patient.getFullName()).isEqualTo("Nguyen Van Gia B");

        AuditLog audit = auditLogDao.findAll().stream()
                .filter(log -> "UPDATE".equals(log.getAction()) && "IDENTITY_CONFLICT".equals(log.getEntityType())
                        && conflict.getId().equals(log.getEntityId()))
                .findFirst().orElseThrow();
        assertThat(audit.getPatientId()).isEqualTo(patientId);
        assertThat(audit.getBeforeValue()).contains("Nguyen Van Gia A");
        assertThat(audit.getAfterValue()).contains("Nguyen Van Gia B");
    }

    @Test
    void resolve_manualEdit_doi_patient_theo_gia_tri_nhap() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "gender", "U", "M", "PENDING");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "MANUAL_EDIT");
        body.put("manual_value", "F");

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolution").value("MANUAL_EDIT"));

        Patient patient = patientDao.findById(patientId).orElseThrow();
        assertThat(patient.getGender()).isEqualTo("F");
    }

    @Test
    void resolve_manualEdit_tra_400_khi_thieu_manual_value() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "full_name", "Nguyen Van Gia A", "Nguyen Van Gia B", "PENDING");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "MANUAL_EDIT");

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void resolve_tra_409_khi_khong_con_pending() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "full_name", "Nguyen Van Gia A", "Nguyen Van Gia B", "KEEP_RIS");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "TAKE_HIS");

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTITY_CONFLICT_ALREADY_RESOLVED"));
    }

    @Test
    void resolve_tra_404_khi_khong_tim_thay() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "KEEP_RIS");

        mockMvc.perform(post("/identity-conflicts/999999/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IDENTITY_CONFLICT_NOT_FOUND"));
    }

    @Test
    void resolve_tra_403_khi_radiologist_xu_ly_RBAC11_RC18() throws Exception {
        IdentityConflict conflict = saveConflict(patientId, "full_name", "Nguyen Van Gia A", "Nguyen Van Gia B", "PENDING");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", "KEEP_RIS");

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        IdentityConflict unchanged = identityConflictDao.findById(conflict.getId()).orElseThrow();
        assertThat(unchanged.getResolution()).isEqualTo("PENDING");
    }

    /* -------------------------------------------------- helpers */

    private IdentityConflict saveConflict(Long patientId, String fieldName, String risValue, String hisValue, String resolution) {
        IdentityConflict conflict = new IdentityConflict();
        conflict.setPatientId(patientId);
        conflict.setSource("HIS");
        conflict.setFieldName(fieldName);
        conflict.setRisValue(risValue);
        conflict.setHisValue(hisValue);
        conflict.setDetectedAt(LocalDateTime.now());
        conflict.setResolution(resolution);
        return identityConflictDao.save(conflict);
    }

    private void saveAppUser(String username, String fullName) {
        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, full_name, email) VALUES (?, ?, ?, ?)",
                username, "khong-dung-trong-test", fullName, username + "@vr-ris.local");
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
}
