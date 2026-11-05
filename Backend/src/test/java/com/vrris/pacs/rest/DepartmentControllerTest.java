package com.vrris.pacs.rest;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lat doc mau Department (doc/15-kien-truc-backend.md muc 9) - thanh cong, 401 (khong token),
 * 403 (token khong co vai tro hop le). Moi test chay trong 1 transaction, Spring tu rollback sau
 * khi test xong nen khong can xoa du lieu bang tay.
 */
@Transactional
class DepartmentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private JwtService jwtService;

    private Long khoaCdhaId;

    @BeforeEach
    void seed() {
        khoaCdhaId = save("K01", "Khoa CDHA", null, "KHOA", 1).getId();
        save("N01", "May CT", khoaCdhaId, "NHOM", 1);
        save("K02", "Khoa Noi", null, "KHOA", 2);
    }

    @Test
    void thanh_cong_voi_token_hop_le() throws Exception {
        String token = jwtService.generateToken("bs.cdha", List.of("RADIOLOGIST"));
        mockMvc.perform(get("/departments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("K01"))
                .andExpect(jsonPath("$[0].type").value("KHOA"))
                .andExpect(jsonPath("$[0].parent_id").doesNotExist())
                .andExpect(jsonPath("$[2].code").value("N01"))
                .andExpect(jsonPath("$[2].parent_id").value(khoaCdhaId));
    }

    @Test
    void tra_ve_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tra_ve_403_khi_token_khong_co_vai_tro_hop_le() throws Exception {
        String token = jwtService.generateToken("nguoi.la", List.of());
        mockMvc.perform(get("/departments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private Department save(String code, String name, Long parentId, String type, int sortOrder) {
        Department d = new Department();
        d.setCode(code);
        d.setName(name);
        d.setParentId(parentId);
        d.setType(type);
        d.setSortOrder(sortOrder);
        d.setIsActive(true);
        return departmentDao.save(d);
    }
}
