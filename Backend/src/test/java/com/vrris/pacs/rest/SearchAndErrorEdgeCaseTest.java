package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cac truong hop bien (review 2026-11-21) tren context Spring + MySQL thuc:
 * <ul>
 *   <li>tab x status giao rong phai tra 0 dong (truoc day tra ca worklist);</li>
 *   <li>phan trang on dinh khi nhieu dong trung gia tri sap xep (co cot phu id);</li>
 *   <li>ky tu "_" / "%" trong o tim kiem la ky tu thuong, khong phai wildcard;</li>
 *   <li>loi request (JSON hong, enum sai, sai method, URL la) ra 4xx dung schema Error, khong 500.</li>
 * </ul>
 * Seed bang JDBC (khong qua model/DAO) de khong phu thuoc thay doi entity cua lop khac.
 */
@Transactional
class SearchAndErrorEdgeCaseTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private Long orderId;
    private Long serviceItemId;
    private Long patientId;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));
        Long departmentId = insert("INSERT INTO department (code, name, type) VALUES ('KEDGE', 'Khoa edge', 'KHOA')");
        serviceItemId = insert("INSERT INTO service_item (code, name, modality) VALUES ('CT_EDGE', 'Chup CT edge', 'CT')");
        patientId = insert("INSERT INTO patient (patient_code, full_name) VALUES ('EDGE0001', 'Benh nhan edge')");
        orderId = insert("INSERT INTO imaging_order (order_code, patient_id, ordering_department_id) VALUES ('PHEDGE001', "
                + patientId + ", " + departmentId + ")");
    }

    /* ------------------------------------------------------------ tab x status */

    @Test
    void tab_PENDING_ket_hop_status_READ_giao_rong_tra_0_dong() throws Exception {
        insertStudy("EDGE00000000001", "SCHEDULED", "2026-11-01 08:00:00");
        insertStudy("EDGE00000000002", "READ", "2026-11-01 08:00:00");

        mockMvc.perform(get("/studies").param("tab", "PENDING").param("status", "READ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    /* ------------------------------------------------------------ phan trang */

    @Test
    void phan_trang_on_dinh_khi_trung_gia_tri_sap_xep() throws Exception {
        List<Long> inserted = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            inserted.add(insertStudy("EDGE0000000010" + i, "SCHEDULED", "2026-11-02 09:00:00"));
        }

        List<Long> seen = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            String json = mockMvc.perform(get("/studies").param("order_code", "PHEDGE001")
                            .param("page", String.valueOf(page)).param("page_size", "2")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            for (JsonNode item : objectMapper.readTree(json).get("items")) {
                seen.add(item.get("id").asLong());
            }
        }

        // Moi dong xuat hien dung mot lan, theo thu tu id tang dan (cot phu cung chieu asc mac dinh).
        assertThat(seen).containsExactlyElementsOf(inserted);
    }

    /* ------------------------------------------------------------ LIKE escape */

    @Test
    void tim_ten_benh_nhan_voi_dau_gach_duoi_khong_phai_wildcard() throws Exception {
        insert("INSERT INTO patient (patient_code, full_name) VALUES ('EDGE0002', 'Ten_co_gach')");

        mockMvc.perform(get("/patients").param("full_name", "_")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].patient_code").value("EDGE0002"));

        mockMvc.perform(get("/patients").param("q", "%")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void worklist_tim_full_name_voi_phan_tram_khong_khop_moi_dong() throws Exception {
        insertStudy("EDGE00000000201", "SCHEDULED", "2026-11-03 09:00:00");

        mockMvc.perform(get("/studies").param("full_name", "%")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    /* ------------------------------------------------------------ loi request -> 4xx */

    @Test
    void json_hong_khi_chuyen_trang_thai_tra_400() throws Exception {
        Long studyId = insertStudy("EDGE00000000301", "SCHEDULED", "2026-11-04 09:00:00");

        mockMvc.perform(post("/studies/" + studyId + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to_status\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void to_status_khong_ton_tai_tra_400_kem_ten_truong() throws Exception {
        Long studyId = insertStudy("EDGE00000000302", "SCHEDULED", "2026-11-04 09:00:00");

        mockMvc.perform(post("/studies/" + studyId + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to_status\": \"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("to_status"));
    }

    @Test
    void sai_method_tra_405() throws Exception {
        mockMvc.perform(delete("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void url_khong_ton_tai_tra_404() throws Exception {
        mockMvc.perform(get("/duong-dan-khong-ton-tai")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    /* ------------------------------------------------------------ helpers */

    private Long insertStudy(String accession, String status, String scheduledAt) {
        return insert("INSERT INTO study (accession_number, order_id, service_item_id, status, scheduled_at) VALUES ('"
                + accession + "', " + orderId + ", " + serviceItemId + ", '" + status + "', '" + scheduledAt + "')");
    }

    private Long insert(String sql) {
        jdbcTemplate.update(sql);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
