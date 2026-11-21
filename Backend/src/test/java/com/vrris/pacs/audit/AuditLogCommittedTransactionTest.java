package com.vrris.pacs.audit;

import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test audit KHONG dung @Transactional (khac moi test controller khac): moi request chay transaction
 * that cua service va COMMIT that, giong production. Muc dich:
 * <ul>
 *   <li>kiem audit_log van ghi duoc khi method @Audited dat @Transactional(readOnly = true)
 *       (MySQL + Connector/J dat connection read-only -> INSERT co the bi tu choi) - test
 *       @Transactional cua lop khac khong phat hien duoc vi transaction ngoai cua test la read-write;</li>
 *   <li>kiem dang nhap sai / bi tu choi quyen o tang service van de lai dong audit (transaction rieng,
 *       khong bi rollback cung exception).</li>
 * </ul>
 * Du lieu duoc commit that nen @AfterEach tu xoa, de khong lam ban cac lop test khac dung chung container.
 */
class AuditLogCommittedTransactionTest extends AbstractIntegrationTest {

    private static final String USERNAME = "audit_tx_user";
    private static final String PASSWORD = "Audit@123";
    private static final String PATIENT_CODE = "AUDITTX0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Long userId;
    private Long patientId;

    @BeforeEach
    void seed() {
        cleanup();
        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, full_name, is_active) VALUES (?, ?, ?, 1)",
                USERNAME, passwordEncoder.encode(PASSWORD), "Nguoi dung audit tx");
        userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, USERNAME);
        jdbcTemplate.update(
                "INSERT INTO patient (patient_code, full_name) VALUES (?, ?)", PATIENT_CODE, "Benh nhan audit tx");
        patientId = jdbcTemplate.queryForObject("SELECT id FROM patient WHERE patient_code = ?", Long.class, PATIENT_CODE);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM audit_log WHERE actor_username IN (?, ?, ?, ?)",
                USERNAME, "khong_ton_tai_audit_tx", "admin_audit_tx", "clinician_audit_tx");
        jdbcTemplate.update("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user WHERE username = ?)", USERNAME);
        jdbcTemplate.update("DELETE FROM app_user WHERE username = ?", USERNAME);
        jdbcTemplate.update("DELETE FROM patient WHERE patient_code = ?", PATIENT_CODE);
    }

    @Test
    void login_thanh_cong_ghi_audit_LOGIN_du_method_la_readOnly() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'LOGIN' AND actor_username = ? AND entity_id = ?",
                Integer.class, USERNAME, userId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void get_patient_readOnly_ghi_audit_READ_kem_patient_id() throws Exception {
        String token = jwtService.generateToken("admin_audit_tx", List.of("ADMIN"));
        mockMvc.perform(get("/patients/" + patientId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'READ' AND entity_type = 'PATIENT' "
                        + "AND actor_username = 'admin_audit_tx' AND patient_id = ? AND entity_id = ?",
                Integer.class, patientId, patientId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void login_sai_mat_khau_van_de_lai_audit_LOGIN_FAILED() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"sai-mat-khau\"}"))
                .andExpect(status().isUnauthorized());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'LOGIN_FAILED' AND actor_username = ? "
                        + "AND actor_user_id IS NULL",
                Integer.class, USERNAME);
        assertThat(count).isEqualTo(1);
    }

    /**
     * Service tu choi 403 (CLINICIAN sua truong dinh danh): transaction nghiep vu rollback, nhung dong
     * ACCESS_DENIED van con (ghi REQUIRES_NEW) - va du lieu benh nhan khong bi sua.
     */
    @Test
    void service_tu_choi_403_van_de_lai_audit_ACCESS_DENIED_va_khong_sua_du_lieu() throws Exception {
        String token = jwtService.generateToken("clinician_audit_tx", List.of("CLINICIAN"));
        mockMvc.perform(patch("/patients/" + patientId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Ten moi\"}"))
                .andExpect(status().isForbidden());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCESS_DENIED' AND entity_type = 'PATIENT' "
                        + "AND actor_username = 'clinician_audit_tx' AND patient_id = ? AND entity_id = ?",
                Integer.class, patientId, patientId);
        assertThat(count).isEqualTo(1);
        String fullName = jdbcTemplate.queryForObject("SELECT full_name FROM patient WHERE id = ?", String.class, patientId);
        assertThat(fullName).isEqualTo("Benh nhan audit tx");
    }

    /** Form FE gui lai ho ten KHONG doi kem SDT moi - CLINICIAN khong duoc bi chan 403 (chi chan khi doi that). */
    @Test
    void clinician_gui_lai_ho_ten_khong_doi_kem_sdt_moi_thanh_cong_va_ghi_audit_UPDATE_kem_patient_id() throws Exception {
        String token = jwtService.generateToken("clinician_audit_tx", List.of("CLINICIAN"));
        mockMvc.perform(patch("/patients/" + patientId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Benh nhan audit tx\",\"phone\":\"0900000123\"}"))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'UPDATE' AND entity_type = 'PATIENT' "
                        + "AND actor_username = 'clinician_audit_tx' AND patient_id = ?",
                Integer.class, patientId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void login_username_khong_ton_tai_van_de_lai_audit_LOGIN_FAILED() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"khong_ton_tai_audit_tx\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'LOGIN_FAILED' AND actor_username = ?",
                Integer.class, "khong_ton_tai_audit_tx");
        assertThat(count).isEqualTo(1);
    }
}
