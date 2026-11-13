package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.RoleDao;
import com.vrris.pacs.dto.request.LoginRequest;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Role;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserDao appUserDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seedUsersAndRoles() {
        Role adminRole = getOrCreateRole("ADMIN", "Quản trị viên");
        Role clinicianRole = getOrCreateRole("CLINICIAN", "BS Lâm sàng");
        Role technicianRole = getOrCreateRole("TECHNICIAN", "Kỹ thuật viên");
        Role radiologistRole = getOrCreateRole("RADIOLOGIST", "BS CĐHA");
        Role approverRole = getOrCreateRole("APPROVER", "BS Duyệt");

        createUserIfAbsent("admin", "Quản trị viên (demo)", "admin@vr-ris.local", 1L, Set.of(adminRole));
        createUserIfAbsent("bslamsang", "BS. Lâm Sàng (demo)", "bslamsang@vr-ris.local", 7L, Set.of(clinicianRole));
        createUserIfAbsent("ktv", "KTV. Kỹ Thuật (demo)", "ktv@vr-ris.local", 1L, Set.of(technicianRole));
        createUserIfAbsent("bscdha", "BS. CĐHA (demo)", "bscdha@vr-ris.local", 1L, Set.of(radiologistRole));
        createUserIfAbsent("bsduyet", "BS. Duyệt (demo)", "bsduyet@vr-ris.local", 1L, Set.of(approverRole));
        createUserIfAbsent("bscdha2", "BS. CĐHA Hai (demo)", "bscdha2@vr-ris.local", 1L, Set.of(radiologistRole));
        createUserIfAbsent("bsduyet2", "BS. Duyệt Hai (demo)", "bsduyet2@vr-ris.local", 1L, Set.of(approverRole));
    }

    private Role getOrCreateRole(String code, String name) {
        return roleDao.findByCode(code).orElseGet(() -> {
            Role r = new Role(code, name);
            return roleDao.save(r);
        });
    }

    private void createUserIfAbsent(String username, String fullName, String email, Long departmentId, Set<Role> roles) {
        if (appUserDao.findByUsername(username).isEmpty()) {
            AppUser u = new AppUser();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode("123456"));
            u.setFullName(fullName);
            u.setEmail(email);
            u.setDepartmentId(departmentId);
            u.setIsActive(true);
            u.setCreatedAt(LocalDateTime.now());
            u.setRoles(roles);
            appUserDao.save(u);
        }
    }

    @Test
    void dang_nhap_admin_thanh_cong() throws Exception {
        LoginRequest req = new LoginRequest("admin", "123456");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.full_name").value("Quản trị viên (demo)"))
                .andExpect(jsonPath("$.user.roles", hasItem("ADMIN")));
    }

    @Test
    void dang_nhap_7_tai_khoan_demo_thanh_cong_dung_vai_tro() throws Exception {
        Map<String, String> expectedRoles = Map.of(
                "admin", "ADMIN",
                "bslamsang", "CLINICIAN",
                "ktv", "TECHNICIAN",
                "bscdha", "RADIOLOGIST",
                "bsduyet", "APPROVER",
                "bscdha2", "RADIOLOGIST",
                "bsduyet2", "APPROVER"
        );

        for (Map.Entry<String, String> entry : expectedRoles.entrySet()) {
            LoginRequest req = new LoginRequest(entry.getKey(), "123456");
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value(entry.getKey()))
                    .andExpect(jsonPath("$.user.roles", hasItem(entry.getValue())));
        }
    }

    @Test
    void sai_mat_khau_tra_ve_401() throws Exception {
        LoginRequest req = new LoginRequest("admin", "wrong_pw");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    void tai_khoan_khong_ton_tai_tra_ve_cung_cau_401() throws Exception {
        LoginRequest req = new LoginRequest("user_khong_ton_tai", "123456");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    void khoa_tai_khoan_sau_5_lan_sai_lien_tiep() throws Exception {
        String testUser = "test_lockout_user";
        createUserIfAbsent(testUser, "Test Lockout", "lock@vr-ris.local", 1L, Set.of(getOrCreateRole("ADMIN", "Admin")));

        // 5 lần sai liên tiếp
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(testUser, "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
        }

        // Lần thứ 6 với mật khẩu ĐÚNG -> vẫn bị từ chối 401 do bị khóa 15 phút
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(testUser, "123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    void dang_nhap_thieu_truong_tra_ve_400() throws Exception {
        LoginRequest req = new LoginRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void auth_me_voi_token_hop_le() throws Exception {
        String token = jwtService.generateToken(1L, "admin", List.of("ADMIN"));

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")));
    }

    @Test
    void auth_me_khong_co_token_tra_ve_401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void auth_me_token_sai_chu_ky_tra_ve_401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_thanh_cong_tra_ve_204() throws Exception {
        String token = jwtService.generateToken(1L, "admin", List.of("ADMIN"));

        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_khong_token_tra_ve_401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
