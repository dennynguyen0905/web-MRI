package com.vrris.pacs.rest;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.Device;
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

@Transactional
class DeviceControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private JwtService jwtService;

    private Long khoaCdhaId;
    private String token;

    @BeforeEach
    void seed() {
        khoaCdhaId = saveDepartment("K01", "Khoa CDHA").getId();
        Long khoaNoiId = saveDepartment("K02", "Khoa Noi").getId();

        saveDevice("CT01", "May CT so 64 lat", "CT", khoaCdhaId, true);
        saveDevice("MR01", "May MRI 1.5T", "MR", khoaCdhaId, true);
        saveDevice("CR01", "May X-quang", "CR", khoaNoiId, true);
        saveDevice("CT02", "May CT cu (ngung dung)", "CT", khoaCdhaId, false);

        token = jwtService.generateToken("ktv.01", List.of("TECHNICIAN"));
    }

    @Test
    void khong_tham_so_tra_het_may_dang_active() throws Exception {
        mockMvc.perform(get("/devices").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void loc_theo_department_id_va_modality() throws Exception {
        mockMvc.perform(get("/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("department_id", String.valueOf(khoaCdhaId))
                        .param("modality", "CT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CT01"))
                .andExpect(jsonPath("$[0].department_id").value(khoaCdhaId))
                .andExpect(jsonPath("$[0].modality").value("CT"));
    }

    @Test
    void tra_ve_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/devices")).andExpect(status().isUnauthorized());
    }

    private Department saveDepartment(String code, String name) {
        Department d = new Department();
        d.setCode(code);
        d.setName(name);
        d.setParentId(null);
        d.setType("KHOA");
        d.setSortOrder(1);
        d.setIsActive(true);
        return departmentDao.save(d);
    }

    private void saveDevice(String code, String name, String modality, Long departmentId, boolean active) {
        Device d = new Device();
        d.setCode(code);
        d.setName(name);
        d.setModality(modality);
        d.setDepartmentId(departmentId);
        d.setIsActive(active);
        deviceDao.save(d);
    }
}
