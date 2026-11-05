package com.vrris.pacs.rest;

import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.model.ServiceItem;
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
class ServiceItemControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceItemDao serviceItemDao;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void seed() {
        save("DV01", "Chup CT so nao", "CT", "So nao", true);
        save("DV02", "Chup MRI cot song", "MR", "Cot song", true);
        save("DV03", "Dich vu ngung dung", "CT", "Nguc", false);

        token = jwtService.generateToken("bs.cdha", List.of("RADIOLOGIST"));
    }

    @Test
    void loc_theo_modality_va_tra_body_part() throws Exception {
        mockMvc.perform(get("/service-items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("modality", "MR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("DV02"))
                .andExpect(jsonPath("$[0].body_part").value("Cot song"));
    }

    @Test
    void modality_khong_hop_le_tra_ve_400_dung_schema_error() throws Exception {
        mockMvc.perform(get("/service-items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("modality", "XQUANG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void tra_ve_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/service-items")).andExpect(status().isUnauthorized());
    }

    private void save(String code, String name, String modality, String bodyPart, boolean active) {
        ServiceItem s = new ServiceItem();
        s.setCode(code);
        s.setName(name);
        s.setModality(modality);
        s.setBodyPart(bodyPart);
        s.setIsActive(active);
        serviceItemDao.save(s);
    }
}
