package com.vrris.pacs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.enu.DepartmentType;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.ReportOrigin;
import com.vrris.pacs.enu.ReportStatus;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.enu.SyncStatus;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /studies. listStudies (IN-07, worklist): xem Javadoc rest.StudyController/service.StudyService -
 * moi bo loc FE gui (tab, status[], department_id[] gom nhanh con, device_id[], modality,
 * patient_class, khoang thoi gian theo time_field, patient_code/full_name/visit_code/order_code,
 * phan trang, sap xep) co mot test tich hop rieng o day, tren MySQL thuc qua Testcontainers
 * (AbstractIntegrationTest) - khop tieu chi chap nhan cua phieu viec IN-07.
 *
 * getStatusCounts (IN-09): cac test statusCounts_* ben duoi - chi test lai dai dien mot vai bo loc
 * (device_id, department_id) vi logic chuan hoa/loc giong het listStudies (da test ky o tren); tap
 * trung vao phan rieng cua IN-09 la dien 0 cho nhom khong co ca va tong du 7 nhom (ke ca CANCELLED).
 */
@Transactional
class StudyControllerTest extends AbstractIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ServiceItemDao serviceItemDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private ImagingOrderDao imagingOrderDao;

    @Autowired
    private StudyDao studyDao;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private Long studyId;
    private Long patientId;
    private Long orderId;
    private Long serviceItemId;
    private Long deviceId;
    private Long departmentId;

    @BeforeEach
    void seed() {
        adminToken = jwtService.generateToken("admin", List.of("ADMIN"));

        patientId = savePatient("TEST0001", "Benh nhan TEST0001").getId();
        departmentId = saveDepartment("K01", null).getId();
        serviceItemId = saveServiceItem("CT_SO_NAO", Modality.CT).getId();
        deviceId = saveDevice("CT01", departmentId, Modality.CT).getId();
        orderId = saveOrder("PH20261031001", patientId, departmentId, PatientClass.OUTPATIENT, null).getId();
        studyId = saveStudy("2026103100001", orderId, serviceItemId, deviceId, StudyStatus.SCHEDULED, null, null).getId();
    }

    @Test
    void get_tra_dung_ca_chup_voi_day_du_patient_order_service_item_device() throws Exception {
        mockMvc.perform(get("/studies/" + studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accession_number").value("2026103100001"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.patient.id").value(patientId))
                .andExpect(jsonPath("$.patient.patient_code").value("TEST0001"))
                .andExpect(jsonPath("$.patient.full_name").value("Benh nhan TEST0001"))
                .andExpect(jsonPath("$.order.id").value(orderId))
                .andExpect(jsonPath("$.order.order_code").value("PH20261031001"))
                .andExpect(jsonPath("$.order.patient_id").value(patientId))
                .andExpect(jsonPath("$.order.studies.length()").value(1))
                .andExpect(jsonPath("$.order.studies[0].accession_number").value("2026103100001"))
                .andExpect(jsonPath("$.service_item.id").value(serviceItemId))
                .andExpect(jsonPath("$.service_item.code").value("CT_SO_NAO"))
                .andExpect(jsonPath("$.device.id").value(deviceId))
                .andExpect(jsonPath("$.device.code").value("CT01"))
                .andExpect(jsonPath("$.current_report").doesNotExist());
    }

    @Test
    void get_tra_device_null_khi_ca_chup_chua_gan_may() throws Exception {
        Study study = saveStudy("2026103100002", orderId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies/" + study.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device").doesNotExist());
    }

    @Test
    void get_tra_current_report_la_ban_co_version_lon_nhat() throws Exception {
        saveReport(studyId, 1, ReportStatus.DRAFT, "Ky thuat v1", "Ket luan v1");
        saveReport(studyId, 2, ReportStatus.FINAL, "Ky thuat v2", "Ket luan v2");

        mockMvc.perform(get("/studies/" + studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_report.version").value(2))
                .andExpect(jsonPath("$.current_report.status").value("FINAL"))
                .andExpect(jsonPath("$.current_report.technique").value("Ky thuat v2"))
                .andExpect(jsonPath("$.current_report.conclusion").value("Ket luan v2"));
    }

    @Test
    void get_tra_404_khi_khong_tim_thay() throws Exception {
        mockMvc.perform(get("/studies/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    @Test
    void get_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/" + studyId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_loc_theo_status() throws Exception {
        mockMvc.perform(get("/studies").param("status", "SCHEDULED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100001"));
    }

    @Test
    void list_loc_theo_nhieu_status() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, deviceId, StudyStatus.UNREAD, null, null);
        saveStudy("2026103100003", orderId, serviceItemId, deviceId, StudyStatus.READING, null, null);

        mockMvc.perform(get("/studies").param("status", "UNREAD", "READING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void list_tra_400_khi_status_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies").param("status", "KHONG_HOP_LE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void export_studies_tra_file_csv_va_ghi_audit_log_EXPORT() throws Exception {
        byte[] bytes = mockMvc.perform(get("/studies/export")
                        .param("status", "SCHEDULED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("danh-sach-ca-chup_")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFSTT,Mã hồ sơ,Trạng thái,");
        assertThat(csv).contains("2026103100001,SCHEDULED,PH20261031001");
        assertThat(csv).contains("TEST0001,Benh nhan TEST0001");
        // Chua co ngay sinh -> tuoi hien thi "—", khong phai "0"
        assertThat(csv).contains(",—,U,");

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'EXPORT' AND entity_type = 'STUDY' "
                        + "AND patient_id = ? AND actor_username = 'admin'",
                Integer.class, patientId);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void export_studies_tra_400_khi_status_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies/export")
                        .param("status", "SAI_TRANG_THAI")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void export_studies_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_tab_PENDING_chi_tra_SCHEDULED() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, deviceId, StudyStatus.UNREAD, null, null);

        mockMvc.perform(get("/studies").param("tab", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].status").value("SCHEDULED"));
    }

    @Test
    void list_tab_DONE_tra_cac_trang_thai_con_lai_theo_state_machine() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, deviceId, StudyStatus.UNREAD, null, null);
        saveStudy("2026103100003", orderId, serviceItemId, deviceId, StudyStatus.READ, null, null);

        mockMvc.perform(get("/studies").param("tab", "DONE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void list_tab_DONE_ket_hop_voi_status_bang_AND() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, deviceId, StudyStatus.UNREAD, null, null);
        saveStudy("2026103100003", orderId, serviceItemId, deviceId, StudyStatus.READ, null, null);

        mockMvc.perform(get("/studies").param("tab", "DONE").param("status", "UNREAD")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_tra_400_khi_tab_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies").param("tab", "KHONG_HOP_LE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_loc_theo_nhieu_device_id() throws Exception {
        Long device2Id = saveDevice("CT02", departmentId, Modality.CT).getId();
        saveStudy("2026103100002", orderId, serviceItemId, device2Id, StudyStatus.SCHEDULED, null, null);
        saveStudy("2026103100003", orderId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("device_id", deviceId.toString(), device2Id.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void list_loc_theo_modality_qua_join_service_item() throws Exception {
        Long mrServiceItemId = saveServiceItem("MR_SO_NAO", Modality.MR).getId();
        saveStudy("2026103100002", orderId, mrServiceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("modality", "MR")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_tra_400_khi_modality_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies").param("modality", "KHONG_HOP_LE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_loc_theo_patient_class_qua_join_order() throws Exception {
        Long inpatientOrderId = saveOrder("PH20261031002", patientId, departmentId, PatientClass.INPATIENT, null).getId();
        saveStudy("2026103100002", inpatientOrderId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("patient_class", "INPATIENT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_tim_theo_patient_code_khong_phan_biet_hoa_thuong_qua_join_patient() throws Exception {
        mockMvc.perform(get("/studies").param("patient_code", "test000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void list_tim_theo_full_name_khong_phan_biet_hoa_thuong_qua_join_patient() throws Exception {
        Long patient2Id = savePatient("TEST0002", "Nguyen Van An").getId();
        Long order2Id = saveOrder("PH20261031003", patient2Id, departmentId, PatientClass.OUTPATIENT, null).getId();
        saveStudy("2026103100002", order2Id, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("full_name", "nguyen van")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_loc_theo_visit_code_qua_join_order() throws Exception {
        Long order2Id = saveOrder("PH20261031004", patientId, departmentId, PatientClass.OUTPATIENT, "LSVV0099").getId();
        saveStudy("2026103100002", order2Id, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("visit_code", "0099")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_loc_theo_order_code_qua_join_order() throws Exception {
        mockMvc.perform(get("/studies").param("order_code", "PH20261031001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/studies").param("order_code", "KHONG_KHOP")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void list_loc_theo_department_id_gom_ca_nhanh_con() throws Exception {
        Long nhomId = saveDepartment("N01", departmentId).getId();
        Long orderDuoiNhomId = saveOrder("PH20261031005", patientId, nhomId, PatientClass.OUTPATIENT, null).getId();
        saveStudy("2026103100002", orderDuoiNhomId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        // Loc theo khoa cha (K01) phai lay luon ca chi dinh o nhom con (N01).
        mockMvc.perform(get("/studies").param("department_id", departmentId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void list_loc_theo_department_id_khop_qua_may_thuc_hien() throws Exception {
        Long khoaKhacId = saveDepartment("K02", null).getId();
        Long deviceKhacId = saveDevice("CT02", khoaKhacId, Modality.CT).getId();
        Long orderKhoaK01Id = saveOrder("PH20261031006", patientId, departmentId, PatientClass.OUTPATIENT, null).getId();
        // Chi dinh o K01 nhung may thuc hien lai thuoc K02.
        saveStudy("2026103100002", orderKhoaK01Id, serviceItemId, deviceKhacId, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("department_id", khoaKhacId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_loc_theo_khoang_thoi_gian_voi_time_field_mac_dinh_SCHEDULED() throws Exception {
        // Dat scheduled_at cua ca da seed ve mot ngay co dinh trong khoang loc - khong dua vao
        // LocalDateTime.now() (seed() dat scheduled_at = now) vi "hom nay" thay doi theo ngay chay test.
        jdbcTemplate.update("UPDATE study SET scheduled_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 10, 15, 8, 0), studyId);
        saveStudy("2026103100002", orderId, serviceItemId, null, StudyStatus.SCHEDULED,
                LocalDateTime.of(2026, 1, 1, 8, 0), null);

        mockMvc.perform(get("/studies")
                        .param("date_from", "2026-10-01").param("date_to", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100001"));
    }

    @Test
    void list_loc_theo_khoang_thoi_gian_voi_time_field_ORDERED() throws Exception {
        Long order2Id = saveOrder("PH20270101001", patientId, departmentId, PatientClass.OUTPATIENT, null).getId();
        jdbcTemplate.update("UPDATE imaging_order SET ordered_at = ? WHERE id = ?",
                LocalDateTime.of(2027, 1, 1, 8, 0), order2Id);
        saveStudy("2026103100002", order2Id, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("time_field", "ORDERED")
                        .param("date_from", "2027-01-01").param("date_to", "2027-01-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"));
    }

    @Test
    void list_tra_400_khi_time_field_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies").param("time_field", "KHONG_HOP_LE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_sap_xep_theo_full_name_qua_join_patient() throws Exception {
        Long patientBId = savePatient("TEST0002", "An Nguyen").getId();
        Long orderBId = saveOrder("PH20261031007", patientBId, departmentId, PatientClass.OUTPATIENT, null).getId();
        saveStudy("2026103100002", orderBId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("sort", "full_name,asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].accession_number").value("2026103100002"))
                .andExpect(jsonPath("$.items[1].accession_number").value("2026103100001"));
    }

    @Test
    void list_phan_trang_dung_page_size() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);
        saveStudy("2026103100003", orderId, serviceItemId, null, StudyStatus.SCHEDULED, null, null);

        mockMvc.perform(get("/studies").param("page", "2").param("page_size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void statusCounts_dem_dung_va_dien_0_cho_nhom_khong_co_ca() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, deviceId, StudyStatus.UNREAD, null, null);
        saveStudy("2026103100003", orderId, serviceItemId, deviceId, StudyStatus.UNREAD, null, null);

        mockMvc.perform(get("/studies/status-counts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.by_status.SCHEDULED").value(1))
                .andExpect(jsonPath("$.by_status.UNREAD").value(2))
                .andExpect(jsonPath("$.by_status.READING").value(0))
                .andExpect(jsonPath("$.by_status.READ").value(0))
                .andExpect(jsonPath("$.by_status.APPROVING").value(0))
                .andExpect(jsonPath("$.by_status.APPROVED").value(0))
                .andExpect(jsonPath("$.by_status.CANCELLED").value(0));
    }

    @Test
    void statusCounts_ap_dung_cung_bo_loc_voi_listStudies_theo_device_id() throws Exception {
        Long device2Id = saveDevice("CT02", departmentId, Modality.CT).getId();
        saveStudy("2026103100002", orderId, serviceItemId, device2Id, StudyStatus.UNREAD, null, null);

        mockMvc.perform(get("/studies/status-counts").param("device_id", deviceId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.by_status.SCHEDULED").value(1))
                .andExpect(jsonPath("$.by_status.UNREAD").value(0));
    }

    @Test
    void statusCounts_ap_dung_cung_bo_loc_theo_department_id_gom_nhanh_con() throws Exception {
        Long nhomId = saveDepartment("N01", departmentId).getId();
        Long orderDuoiNhomId = saveOrder("PH20261031005", patientId, nhomId, PatientClass.OUTPATIENT, null).getId();
        saveStudy("2026103100002", orderDuoiNhomId, serviceItemId, null, StudyStatus.UNREAD, null, null);

        mockMvc.perform(get("/studies/status-counts").param("department_id", departmentId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void statusCounts_tra_400_khi_modality_khong_hop_le() throws Exception {
        mockMvc.perform(get("/studies/status-counts").param("modality", "KHONG_HOP_LE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void statusCounts_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/status-counts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusCounts_tra_403_khi_vai_tro_khong_hop_le() throws Exception {
        String guestToken = jwtService.generateToken("guest", List.of("GUEST"));
        mockMvc.perform(get("/studies/status-counts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getViewerLink_thanh_cong_ghi_audit_log_VIEW_IMAGE() throws Exception {
        Study study = saveStudyWithUid("2026103100002", orderId, serviceItemId, StudyStatus.READ, "1.2.826.0.1.3680043.8.498.10000001");

        mockMvc.perform(get("/studies/" + study.getId() + "/viewer-link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study_id").value(study.getId()))
                .andExpect(jsonPath("$.patient_id").value(patientId))
                .andExpect(jsonPath("$.study_instance_uid").value("1.2.826.0.1.3680043.8.498.10000001"))
                .andExpect(jsonPath("$.viewer_url").value("http://localhost:8043/viewer/1.2.826.0.1.3680043.8.498.10000001"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'VIEW_IMAGE' AND entity_type = 'STUDY' "
                        + "AND entity_id = ? AND patient_id = ? AND actor_username = 'admin'",
                Integer.class, study.getId(), patientId);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void getViewerLink_tra_422_khi_chua_co_anh_pacs() throws Exception {
        mockMvc.perform(get("/studies/" + studyId + "/viewer-link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("STUDY_NO_DICOM_IMAGE"));
    }

    @Test
    void getViewerLink_tra_401_khi_khong_co_token() throws Exception {
        mockMvc.perform(get("/studies/" + studyId + "/viewer-link"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getViewerLink_tra_403_khi_vai_tro_khong_hop_le() throws Exception {
        String guestToken = jwtService.generateToken("guest", List.of("GUEST"));
        mockMvc.perform(get("/studies/" + studyId + "/viewer-link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getViewerLink_cac_vai_tro_duoc_phep_deu_mo_duoc_viewer() throws Exception {
        Study study = saveStudyWithUid("2026103100003", orderId, serviceItemId, StudyStatus.READ, "1.2.826.0.1.3680043.8.498.10000002");

        List<String> allowedRoles = List.of("CLINICIAN", "TECHNICIAN", "RADIOLOGIST", "APPROVER");
        for (String role : allowedRoles) {
            String token = jwtService.generateToken("user_" + role.toLowerCase(), List.of(role));
            mockMvc.perform(get("/studies/" + study.getId() + "/viewer-link")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.study_instance_uid").value("1.2.826.0.1.3680043.8.498.10000002"));
        }
    }

    @Test
    void list_tab_va_status_giao_nhau_rong_tra_0_ca_khong_phai_tat_ca() throws Exception {
        saveStudy("2026103100002", orderId, serviceItemId, deviceId, StudyStatus.READ, null, null);

        mockMvc.perform(get("/studies").param("tab", "PENDING").param("status", "READ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void get_chi_tiet_ghi_audit_READ_kem_patient_id_va_entity_id() throws Exception {
        mockMvc.perform(get("/studies/" + studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'READ' AND entity_type = 'STUDY' "
                        + "AND entity_id = ? AND patient_id = ?",
                Integer.class, studyId, patientId);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void patch_tra_400_khi_study_instance_uid_sai_dinh_dang() throws Exception {
        String tooLong = "1." + "2".repeat(63);
        for (String bad : List.of("", "abc", "1..2", "1.2.", tooLong)) {
            mockMvc.perform(patch("/studies/" + studyId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of("study_instance_uid", bad))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void patch_tra_409_khi_study_instance_uid_da_gan_cho_ca_khac() throws Exception {
        saveStudyWithUid("2026103100002", orderId, serviceItemId, StudyStatus.UNREAD, "1.2.840.10008.999");

        mockMvc.perform(patch("/studies/" + studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"study_instance_uid\":\"1.2.840.10008.999\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_INSTANCE_UID_DUPLICATE"));
    }

    @Test
    void patch_dat_lai_dung_uid_hien_tai_khong_bao_trung() throws Exception {
        Study study = saveStudyWithUid("2026103100002", orderId, serviceItemId, StudyStatus.SCHEDULED, "1.2.840.10008.777");

        mockMvc.perform(patch("/studies/" + study.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"study_instance_uid\":\"1.2.840.10008.777\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void patch_tra_409_khi_ca_da_APPROVED_hoac_CANCELLED() throws Exception {
        for (StudyStatus finalStatus : List.of(StudyStatus.APPROVED, StudyStatus.CANCELLED)) {
            Study study = saveStudy("20261031000" + (finalStatus == StudyStatus.APPROVED ? "11" : "12"),
                    orderId, serviceItemId, deviceId, finalStatus, null, null);
            mockMvc.perform(patch("/studies/" + study.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduled_at\":\"2026-11-20T08:00:00\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("STUDY_NOT_EDITABLE"));
        }
    }

    @Test
    void them_ca_sinh_accession_tuan_tu_va_bo_qua_so_da_dung() throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // Gia lap du lieu da co so 00001 cua hom nay (tao sau khi migration V9 khoi tao daily_sequence).
        saveStudy(today + "00001", orderId, serviceItemId, deviceId, StudyStatus.SCHEDULED, null, null);
        String body = "{\"service_item_id\":" + serviceItemId + ",\"scheduled_at\":\"2026-11-20T08:00:00\"}";

        mockMvc.perform(post("/imaging-orders/" + orderId + "/studies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.accession_number").value(today + "00002"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(post("/imaging-orders/" + orderId + "/studies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.accession_number").value(today + "00003"));
    }

    private Study saveStudyWithUid(String accessionNumber, Long orderId, Long serviceItemId, StudyStatus status, String studyInstanceUid) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setStatus(status);
        study.setStudyInstanceUid(studyInstanceUid);
        LocalDateTime now = LocalDateTime.now();
        study.setScheduledAt(now);
        study.setCreatedAt(now);
        study.setUpdatedAt(now);
        return studyDao.save(study);
    }

    private Study saveStudy(String accessionNumber, Long orderId, Long serviceItemId, Long deviceId, StudyStatus status,
                             LocalDateTime scheduledAt, LocalDateTime completedAt) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setDeviceId(deviceId);
        study.setStatus(status);
        study.setScheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now());
        study.setCompletedAt(completedAt);
        LocalDateTime now = LocalDateTime.now();
        study.setCreatedAt(now);
        study.setUpdatedAt(now);
        return studyDao.save(study);
    }

    private ImagingOrder saveOrder(String orderCode, Long patientId, Long departmentId, PatientClass patientClass, String visitCode) {
        ImagingOrder order = new ImagingOrder();
        order.setOrderCode(orderCode);
        order.setPatientId(patientId);
        order.setVisitCode(visitCode);
        order.setSource(OrderSource.MANUAL);
        order.setSyncStatus(SyncStatus.NOT_SYNCED);
        order.setOrderingDepartmentId(departmentId);
        order.setOrderingDoctorName("BS Test");
        order.setPatientClass(patientClass);
        LocalDateTime now = LocalDateTime.now();
        order.setOrderedAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return imagingOrderDao.save(order);
    }

    private Patient savePatient(String code, String fullName) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFullName(fullName);
        p.setDobPrecision(DobPrecision.DAY);
        p.setGender(Gender.U);
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return patientDao.save(p);
    }

    private Department saveDepartment(String code, Long parentId) {
        Department d = new Department();
        d.setCode(code);
        d.setName("Khoa " + code);
        d.setParentId(parentId);
        d.setType(DepartmentType.KHOA);
        d.setSortOrder(1);
        d.setIsActive(true);
        return departmentDao.save(d);
    }

    private ServiceItem saveServiceItem(String code, Modality modality) {
        ServiceItem s = new ServiceItem();
        s.setCode(code);
        s.setName("Chup " + code);
        s.setModality(modality);
        s.setIsActive(true);
        return serviceItemDao.save(s);
    }

    private Report saveReport(Long studyId, int version, ReportStatus status, String technique, String conclusion) {
        Report report = new Report();
        report.setStudyId(studyId);
        report.setVersion(version);
        report.setStatus(status);
        report.setOrigin(ReportOrigin.MANUAL);
        report.setTechnique(technique);
        report.setConclusion(conclusion);
        report.setCreatedByUserId(saveAppUser("bscdha_v" + version));
        report.setCreatedAt(LocalDateTime.now());
        return reportDao.save(report);
    }

    /**
     * Flyway trong test chi chay toi V6 (xem src/test/resources/application.yml) nen bang app_user
     * rong - phai tu them mot nguoi dung truoc khi insert report, vi report.created_by_user_id co
     * khoa ngoai toi app_user (giong cach rest.PrintControllerTest.saveAppUser dang lam).
     */
    private Long saveAppUser(String username) {
        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, full_name, email) VALUES (?, ?, ?, ?)",
                username, "khong-dung-trong-test", "BS. CDHA (test)", username + "@vr-ris.local");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    private Device saveDevice(String code, Long departmentId, Modality modality) {
        Device device = new Device();
        device.setCode(code);
        device.setName("May " + code);
        device.setModality(modality);
        device.setDepartmentId(departmentId);
        device.setIsActive(true);
        return deviceDao.save(device);
    }
}
