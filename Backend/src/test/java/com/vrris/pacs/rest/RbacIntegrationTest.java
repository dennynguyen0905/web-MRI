package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.IdentityConflictDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.ConflictResolutionRequest;
import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.request.ReportWriteRequest;
import com.vrris.pacs.dto.request.StudyUpdateRequest;
import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.enu.ConflictResolution;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.IdentityConflict;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bo kiem thu RBAC 5 vai tro (IN-14, doc/06-rbac-matrix.md muc 8).
 * Phu toan bo test case RBAC-01 -> RBAC-15 va cac quy tac chuyen trang thai T1-T10, gioi han row-level.
 */
@Transactional
class RbacIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

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
    private ReportDao reportDao;

    @Autowired
    private IdentityConflictDao identityConflictDao;

    @Autowired
    private AppUserDao appUserDao;

    private String adminToken;
    private String clinicianToken;
    private String technicianToken;
    private String radiologist1Token;
    private String radiologist2Token;
    private String approverToken;
    private String dualRoleToken; // Co ca RADIOLOGIST va APPROVER de test "nguoi duyet != nguoi doc"

    private AppUser adminUser;
    private AppUser clinicianUser;
    private AppUser techUser;
    private AppUser rad1User;
    private AppUser rad2User;
    private AppUser approverUser;
    private AppUser dualUser;

    private Long patientId;
    private Long orderId;
    private Long serviceItemId;
    private Long departmentId;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setCode("K01_RBAC");
        dept.setName("Khoa Chan Doan Hinh Anh");
        dept.setType("KHOA");
        dept.setSortOrder(1);
        dept.setIsActive(true);
        departmentDao.save(dept);
        departmentId = dept.getId();

        adminUser = saveUser("admin_rbac", "Quan Tri Vien", departmentId);
        clinicianUser = saveUser("bsls_rbac", "BS Lam Sang", departmentId);
        techUser = saveUser("ktv_rbac", "Ky Thuat Vien", departmentId);
        rad1User = saveUser("rad1_rbac", "BS CDHA 1", departmentId);
        rad2User = saveUser("rad2_rbac", "BS CDHA 2", departmentId);
        approverUser = saveUser("appr_rbac", "BS Duyet", departmentId);
        dualUser = saveUser("dual_rbac", "BS Doc Va Duyet", departmentId);

        adminToken = jwtService.generateToken(adminUser.getId(), adminUser.getUsername(), List.of("ADMIN"));
        clinicianToken = jwtService.generateToken(clinicianUser.getId(), clinicianUser.getUsername(), List.of("CLINICIAN"));
        technicianToken = jwtService.generateToken(techUser.getId(), techUser.getUsername(), List.of("TECHNICIAN"));
        radiologist1Token = jwtService.generateToken(rad1User.getId(), rad1User.getUsername(), List.of("RADIOLOGIST"));
        radiologist2Token = jwtService.generateToken(rad2User.getId(), rad2User.getUsername(), List.of("RADIOLOGIST"));
        approverToken = jwtService.generateToken(approverUser.getId(), approverUser.getUsername(), List.of("APPROVER"));
        dualRoleToken = jwtService.generateToken(dualUser.getId(), dualUser.getUsername(), List.of("RADIOLOGIST", "APPROVER"));

        Patient patient = new Patient();
        patient.setPatientCode("BN_RBAC_01");
        patient.setFullName("Nguyen Van A");
        patient.setDateOfBirth(LocalDate.of(1985, 1, 1));
        patient.setDobPrecision("DAY");
        patient.setGender("M");
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patientDao.save(patient);
        patientId = patient.getId();

        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setCode("MRI_BRAIN_RBAC");
        serviceItem.setName("Chup MRI nao");
        serviceItem.setModality("MR");
        serviceItem.setIsActive(true);
        serviceItemDao.save(serviceItem);
        serviceItemId = serviceItem.getId();

        ImagingOrder order = new ImagingOrder();
        order.setOrderCode("ORD_RBAC_01");
        order.setPatientId(patientId);
        order.setOrderingDepartmentId(departmentId);
        order.setOrderingDoctorName("BS Lam Sang");
        order.setPatientClass("INPATIENT");
        order.setSource("MANUAL");
        order.setSyncStatus("NOT_SYNCED");
        order.setOrderedAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        imagingOrderDao.save(order);
        orderId = order.getId();
    }

    private AppUser saveUser(String username, String fullName, Long deptId) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz12");
        user.setFullName(fullName);
        user.setEmail(username + "@vrris.vn");
        user.setDepartmentId(deptId);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return appUserDao.save(user);
    }

    private Study createStudy(String accNo, String status, String uid, Long readingUserId, Long approvingUserId) {
        Study study = new Study();
        study.setAccessionNumber(accNo);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus(status);
        study.setStudyInstanceUid(uid);
        study.setReadingUserId(readingUserId);
        study.setApprovingUserId(approvingUserId);
        study.setScheduledAt(LocalDateTime.now());
        study.setCreatedAt(LocalDateTime.now());
        study.setUpdatedAt(LocalDateTime.now());
        return studyDao.save(study);
    }

    private Report createReport(Long studyId, int version, String status, String tech, String concl, Long createdBy) {
        Report r = new Report();
        r.setStudyId(studyId);
        r.setVersion(version);
        r.setStatus(status);
        r.setOrigin("MANUAL");
        r.setTechnique(tech);
        r.setConclusion(concl);
        r.setCreatedByUserId(createdBy);
        r.setCreatedAt(LocalDateTime.now());
        return reportDao.save(r);
    }

    @Test
    @DisplayName("RBAC-01: Chua dang nhap -> GET /studies tra 401")
    void rbac01_chua_dang_nhap_tra_401() throws Exception {
        mockMvc.perform(get("/studies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("RBAC-02: Token het han hoac sai chu ky -> GET /auth/me tra 401")
    void rbac02_token_sai_chu_ky_hoac_het_han_tra_401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.sai.chu.ky.khong.hop.le"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("RBAC-03: RADIOLOGIST tao benh nhan -> POST /patients tra 403")
    void rbac03_radiologist_tao_benh_nhan_tra_403() throws Exception {
        PatientCreateRequest req = new PatientCreateRequest(
                "BN_NEW_RAD", "Nguyen Benh Nhan", null, null, null, null, null, null
        );

        mockMvc.perform(post("/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologist1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("RBAC-04: APPROVER luu bao cao -> PUT /studies/{id}/reports/current tra 403")
    void rbac04_approver_luu_bao_cao_tra_403() throws Exception {
        Study study = createStudy("ACC_RBAC_04", "READING", "1.2.3.4", rad1User.getId(), null);
        createReport(study.getId(), 1, "DRAFT", "KT", "KL", rad1User.getId());

        ReportWriteRequest req = new ReportWriteRequest("KT moi", "Mo ta", "Chan doan", "Ket luan", "Khuyen nghi");

        mockMvc.perform(put("/studies/" + study.getId() + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("RBAC-05: ADMIN luu bao cao -> PUT /studies/{id}/reports/current tra 403")
    void rbac05_admin_luu_bao_cao_tra_403() throws Exception {
        Study study = createStudy("ACC_RBAC_05", "READING", "1.2.3.5", rad1User.getId(), null);
        createReport(study.getId(), 1, "DRAFT", "KT", "KL", rad1User.getId());

        ReportWriteRequest req = new ReportWriteRequest("KT moi", "Mo ta", "Chan doan", "Ket luan", "Khuyen nghi");

        mockMvc.perform(put("/studies/" + study.getId() + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("RBAC-06: ADMIN chuyen T7 (duyet) -> POST /studies/{id}/transitions tra 403")
    void rbac06_admin_chuyen_t7_duyet_tra_403() throws Exception {
        Study study = createStudy("ACC_RBAC_06", "APPROVING", "1.2.3.6", rad1User.getId(), approverUser.getId());
        createReport(study.getId(), 1, "FINAL", "KT", "KL", rad1User.getId());

        TransitionRequest req = new TransitionRequest(StudyStatus.APPROVED, null, null);

        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_CANNOT_APPROVE"));
    }

    @Test
    @DisplayName("RBAC-07: CLINICIAN xem audit log -> GET /audit-logs tra 403")
    void rbac07_clinician_xem_audit_log_tra_403() throws Exception {
        mockMvc.perform(get("/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clinicianToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("RBAC-08: ADMIN xem audit log -> GET /audit-logs tra 200")
    void rbac08_admin_xem_audit_log_tra_200() throws Exception {
        mockMvc.perform(get("/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("RBAC-09: RADIOLOGIST thu hai luu bao cao cua ca nguoi khac dang doc -> PUT tra 403")
    void rbac09_radiologist_thu_hai_luu_bao_cao_ca_nguoi_khac_dang_doc_tra_403() throws Exception {
        Study study = createStudy("ACC_RBAC_09", "READING", "1.2.3.9", rad1User.getId(), null);
        createReport(study.getId(), 1, "DRAFT", "KT", "KL", rad1User.getId());

        ReportWriteRequest req = new ReportWriteRequest("KT moi", "Mo ta", "Chan doan", "Ket luan", "Khuyen nghi");

        mockMvc.perform(put("/studies/" + study.getId() + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologist2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_HOLDER"));
    }

    @Test
    @DisplayName("RBAC-10: Nguoi vua doc vua co vai tro APPROVER duyet ca minh doc -> T6/T7 tra 403")
    void rbac10_nguoi_vua_doc_vua_co_vai_tro_approver_duyet_ca_minh_doc_tra_403() throws Exception {
        // study co readingUserId = dualUser.getId()
        Study study = createStudy("ACC_RBAC_10", "READ", "1.2.3.10", dualUser.getId(), null);
        createReport(study.getId(), 1, "FINAL", "KT", "KL", dualUser.getId());

        TransitionRequest req = new TransitionRequest(StudyStatus.APPROVING, null, null);

        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dualRoleToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVER_CANNOT_BE_READER"));
    }

    @Test
    @DisplayName("RBAC-11: RADIOLOGIST xu ly identity-conflict -> POST /resolution tra 403")
    void rbac11_radiologist_xu_ly_identity_conflict_tra_403() throws Exception {
        IdentityConflict conflict = new IdentityConflict();
        conflict.setResolution("PENDING");
        conflict.setSource("HIS");
        conflict.setFieldName("full_name");
        conflict.setPatientId(patientId);
        conflict.setRisValue("Nguyen Van A");
        conflict.setHisValue("Nguyen Van An");
        conflict.setDetectedAt(LocalDateTime.now());
        identityConflictDao.save(conflict);

        ConflictResolutionRequest req = new ConflictResolutionRequest(ConflictResolution.KEEP_RIS, null, null);

        mockMvc.perform(post("/identity-conflicts/" + conflict.getId() + "/resolution")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologist1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("RBAC-12: TECHNICIAN huy ca da co study_instance_uid -> T10 tra 422")
    void rbac12_technician_huy_ca_da_co_study_instance_uid_tra_422() throws Exception {
        Study study = createStudy("ACC_RBAC_12", "SCHEDULED", "1.2.3.12", null, null);

        TransitionRequest req = new TransitionRequest(StudyStatus.CANCELLED, "Huy vi benh nhan doi lich", null);

        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CANNOT_CANCEL_STUDY_WITH_IMAGE"));
    }

    @Test
    @DisplayName("RBAC-13: CLINICIAN dat study_instance_uid -> PATCH /studies/{id} tra 403")
    void rbac13_clinician_dat_study_instance_uid_tra_403() throws Exception {
        Study study = createStudy("ACC_RBAC_13", "SCHEDULED", null, null, null);

        StudyUpdateRequest req = new StudyUpdateRequest(
                null, null, null, null, null, "1.2.3.13.mri.new"
        );

        mockMvc.perform(patch("/studies/" + study.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clinicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("RBAC-14: Moi lan 403 deu tra ve cau truc ApiError chuẩn")
    void rbac14_dinh_dang_loi_403_dung_api_error() throws Exception {
        mockMvc.perform(get("/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clinicianToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("RBAC-15: Xuat danh sach yeu cau token hop le va tra CSV kem BOM")
    void rbac15_xuat_danh_sach_yeu_cau_auth_va_tra_csv() throws Exception {
        // Chua co token -> 401
        mockMvc.perform(get("/patients/export"))
                .andExpect(status().isUnauthorized());

        // Co token -> 200 CSV UTF-8
        mockMvc.perform(get("/patients/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));
    }

    @Test
    @DisplayName("CLINICIAN goi /transitions bi chan 403 (T1 qua tao ca, khong qua transitions)")
    void clinician_goi_transitions_bi_chan_403() throws Exception {
        Study study = createStudy("ACC_CLINICIAN_TRANS", "SCHEDULED", null, null, null);
        TransitionRequest req = new TransitionRequest(StudyStatus.UNREAD, null, null);

        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clinicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("CLINICIAN sua truong dinh danh benh nhan bi chan 403 (D24)")
    void clinician_sua_truong_dinh_danh_bi_chan_403() throws Exception {
        PatientUpdateRequest req = new PatientUpdateRequest(
                "Ten Moi Bi Chan", null, null, null, null, null, null
        );

        mockMvc.perform(patch("/patients/" + patientId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clinicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("CLINICIAN sua SDT hoac dia chi benh nhan thanh cong 200 (D24)")
    void clinician_sua_sdt_dia_chi_thanh_cong_200() throws Exception {
        PatientUpdateRequest req = new PatientUpdateRequest(
                null, null, null, null, null, "0912345678", "123 Pho Hue, Ha Noi"
        );

        mockMvc.perform(patch("/patients/" + patientId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clinicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0912345678"))
                .andExpect(jsonPath("$.address").value("123 Pho Hue, Ha Noi"));
    }

    @Test
    @DisplayName("TECHNICIAN dat study_instance_uid thanh cong 200")
    void technician_dat_study_instance_uid_thanh_cong_200() throws Exception {
        Study study = createStudy("ACC_TECH_SET_UID", "SCHEDULED", null, null, null);

        StudyUpdateRequest req = new StudyUpdateRequest(
                null, null, null, null, null, "1.2.3.4.5.6.789"
        );

        mockMvc.perform(patch("/studies/" + study.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study_instance_uid").value("1.2.3.4.5.6.789"));
    }

    @Test
    @DisplayName("Luong chuyen trang thai chinh T2 -> T3 -> T5 -> T6 -> T7 thanh cong voi dung vai tro")
    void luong_chuyen_trang_thai_chinh_thanh_cong() throws Exception {
        // 1. Tao ca SCHEDULED da co UID (T1 da xong)
        Study study = createStudy("ACC_FLOW_MAIN", "SCHEDULED", "1.2.840.10008.flow", null, null);

        // 2. T2: SCHEDULED -> UNREAD boi TECHNICIAN
        TransitionRequest t2Req = new TransitionRequest(StudyStatus.UNREAD, null, null);
        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t2Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("UNREAD"));

        // 3. T3: UNREAD -> READING boi RADIOLOGIST 1
        TransitionRequest t3Req = new TransitionRequest(StudyStatus.READING, null, null);
        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologist1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t3Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("READING"))
                .andExpect(jsonPath("$.study.reading_user_id").value(rad1User.getId()));

        // Luu noi dung bao cao truoc khi chot doc
        ReportWriteRequest reportReq = new ReportWriteRequest(
                "Ky thuat chup T1, T2", "Khong thay bat thuong", "Binh thuong", "Ket luan ro rang", null
        );
        mockMvc.perform(put("/studies/" + study.getId() + "/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologist1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isOk());

        // 4. T5: READING -> READ boi RADIOLOGIST 1
        TransitionRequest t5Req = new TransitionRequest(StudyStatus.READ, null, null);
        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + radiologist1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t5Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("READ"));

        // 5. T6: READ -> APPROVING boi APPROVER (khac rad1User)
        TransitionRequest t6Req = new TransitionRequest(StudyStatus.APPROVING, null, null);
        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t6Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("APPROVING"))
                .andExpect(jsonPath("$.study.approving_user_id").value(approverUser.getId()));

        // 6. T7: APPROVING -> APPROVED boi APPROVER
        TransitionRequest t7Req = new TransitionRequest(StudyStatus.APPROVED, null, null);
        mockMvc.perform(post("/studies/" + study.getId() + "/transitions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t7Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.status").value("APPROVED"));

        // Kiem tra lich su chuyen trang thai
        mockMvc.perform(get("/studies/" + study.getId() + "/status-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }
}
