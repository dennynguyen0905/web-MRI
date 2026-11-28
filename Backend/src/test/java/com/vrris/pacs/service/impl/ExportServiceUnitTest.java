package com.vrris.pacs.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.audit.AuditLogAspect;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dao.StudyLookupDao;
import com.vrris.pacs.dto.request.ListExportEventRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.enu.DobPrecision;
import com.vrris.pacs.enu.Gender;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.model.AuditLog;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.Report;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.rest.PatientController;
import com.vrris.pacs.rest.StudyController;
import com.vrris.pacs.service.PatientService;
import com.vrris.pacs.service.StudyExportService;
import com.vrris.pacs.service.StudyService;
import com.vrris.pacs.service.StudyTransitionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test (khong can Docker) kiem tra luong xuat/tai danh sach benh nhan & worklist qua
 * Controller -> Service -> AuditLogAspect (AOP) -> PatientListCsvExporter (IN-25, IN-15, R-03).
 */
class ExportServiceUnitTest {

    private AuditLogDao auditLogDao;
    private PatientDao patientDao;
    private StudyDao studyDao;
    private StudyLookupDao studyLookupDao;

    private PatientService proxiedPatientService;
    private PatientController patientController;
    private StudyController studyController;

    @BeforeEach
    void setUp() {
        auditLogDao = mock(AuditLogDao.class);
        patientDao = mock(PatientDao.class);
        studyDao = mock(StudyDao.class);
        studyLookupDao = mock(StudyLookupDao.class);

        // Khong co transaction that trong unit test -> aspect ghi qua TransactionTemplate (REQUIRES_NEW)
        // tren transaction manager gia; auditLogDao.save van duoc goi dong bo nhu truoc.
        AuditLogAspect aspect = new AuditLogAspect(auditLogDao, new ObjectMapper(),
                mock(org.springframework.transaction.PlatformTransactionManager.class));

        PatientServiceImpl rawPatientService = new PatientServiceImpl(patientDao, mock(PatientMapper.class));
        AspectJProxyFactory patientProxyFactory = new AspectJProxyFactory(rawPatientService);
        patientProxyFactory.addAspect(aspect);
        proxiedPatientService = patientProxyFactory.getProxy();

        StudyExportServiceImpl rawStudyExportService = new StudyExportServiceImpl(studyDao, studyLookupDao);
        AspectJProxyFactory studyProxyFactory = new AspectJProxyFactory(rawStudyExportService);
        studyProxyFactory.addAspect(aspect);
        StudyExportService proxiedStudyExportService = studyProxyFactory.getProxy();

        patientController = new PatientController(proxiedPatientService);
        studyController = new StudyController(mock(StudyService.class), mock(StudyTransitionService.class),
                proxiedStudyExportService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ktv_test", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void export_danh_sach_nhieu_benh_nhan_ghi_audit_EXPORT_voi_patient_id_null() {
        Patient p1 = fakePatient(101L, "BN-FAKE-201", "Nguyễn Văn Giả Một", LocalDate.of(1958, 1, 1), DobPrecision.YEAR);
        Patient p2 = fakePatient(102L, "BN-FAKE-202", "Trần Thị Giả Hai", null, DobPrecision.DAY);
        when(patientDao.search(any())).thenReturn(new PatientDao.SearchResult(List.of(p1, p2), 2));

        ResponseEntity<org.springframework.core.io.Resource> response = patientController.exportPatients(null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("danh-sach-benh-nhan_");

        byte[] bytes = ((org.springframework.core.io.ByteArrayResource) response.getBody()).getByteArray();
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFSTT,Mã bệnh nhân,Mã HIS,Họ tên,");
        assertThat(csv).contains("BN-FAKE-201,,Nguyễn Văn Giả Một,1958,YEAR,~");
        assertThat(csv).contains("BN-FAKE-202,,Trần Thị Giả Hai,,DAY,—");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.EXPORT);
        assertThat(log.getEntityType()).isEqualTo(AuditEntityType.PATIENT);
        assertThat(log.getActorUsername()).isEqualTo("ktv_test");
        assertThat(log.getPatientId()).isNull();
    }

    @Test
    void export_danh_sach_1_benh_nhan_ghi_audit_EXPORT_kem_patient_id() {
        Patient p1 = fakePatient(101L, "BN-FAKE-201", "Nguyễn Văn Giả Một", LocalDate.of(1980, 5, 10), DobPrecision.DAY);
        when(patientDao.search(any())).thenReturn(new PatientDao.SearchResult(List.of(p1), 1));

        ExportDocumentResponse doc = proxiedPatientService.exportCsv(null, "BN-FAKE-201", null, null);
        assertThat(doc.rowCount()).isEqualTo(1);
        assertThat(doc.patientId()).isEqualTo(101L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.EXPORT);
        assertThat(log.getPatientId()).isEqualTo(101L);
    }

    @Test
    void export_events_ghi_audit_EXPORT_cho_in_danh_sach_trinh_duyet() {
        patientController.recordExportEvent(
                new ListExportEventRequest("STUDIES", "PRINT", 12, null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.EXPORT);
        assertThat(log.getPatientId()).isNull();
    }

    @Test
    void export_worklist_studies_loc_theo_ma_benh_nhan_va_ghi_audit_EXPORT() {
        Study s1 = new Study();
        s1.setId(501L);
        s1.setOrderId(301L);
        s1.setServiceItemId(10L);
        s1.setDeviceId(20L);
        s1.setAccessionNumber("ACC-20261107-0001");
        s1.setStatus(StudyStatus.SCHEDULED);
        s1.setScheduledAt(LocalDateTime.of(2026, 11, 7, 9, 0));

        ImagingOrder order = new ImagingOrder();
        order.setId(301L);
        order.setPatientId(101L);
        order.setOrderCode("ORD-20261107-0001");
        order.setVisitCode("LSVV-FAKE-01");
        order.setPatientClass(PatientClass.OUTPATIENT);
        order.setOrderingDoctorName("BS. Lê Anh Tuấn");
        order.setClinicalDiagnosis("Đau đầu kéo dài");

        Patient patient = fakePatient(101L, "BN-FAKE-201", "Nguyễn Văn Giả Một", LocalDate.of(1958, 1, 1), DobPrecision.YEAR);

        ServiceItem service = new ServiceItem();
        service.setId(10L);
        service.setName("Chụp cộng hưởng từ sọ não");
        service.setModality(Modality.MR);
        service.setBodyPart("Sọ não");

        Device device = new Device();
        device.setId(20L);
        device.setName("MRI 1.5T Phòng 1");

        Report report = new Report();
        report.setId(701L);
        report.setStudyId(501L);
        report.setConclusion("Bình thường");

        when(studyDao.search(any())).thenReturn(new StudyDao.SearchResult(List.of(s1), 1));
        when(studyLookupDao.findAllByIds(ImagingOrder.class, List.of(301L))).thenReturn(List.of(order));
        when(studyLookupDao.findAllByIds(Patient.class, List.of(101L))).thenReturn(List.of(patient));
        when(studyLookupDao.findAllByIds(ServiceItem.class, List.of(10L))).thenReturn(List.of(service));
        when(studyLookupDao.findAllByIds(Device.class, List.of(20L))).thenReturn(List.of(device));
        when(studyLookupDao.findCurrentReportsByStudyIds(List.of(501L))).thenReturn(List.of(report));

        ResponseEntity<org.springframework.core.io.Resource> response = studyController.exportStudies(
                null, "SCHEDULED", null, null, "BN-FAKE-201", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] bytes = ((org.springframework.core.io.ByteArrayResource) response.getBody()).getByteArray();
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFSTT,Mã hồ sơ,Trạng thái,Mã phiếu,Mã LSVV,Loại BN,Mã bệnh nhân");
        assertThat(csv).contains("ACC-20261107-0001,SCHEDULED,ORD-20261107-0001,LSVV-FAKE-01,OUTPATIENT,BN-FAKE-201");
        assertThat(csv).contains("Chụp cộng hưởng từ sọ não,MR,Sọ não,MRI 1.5T Phòng 1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.EXPORT);
        assertThat(log.getEntityType()).isEqualTo(AuditEntityType.STUDY);
        assertThat(log.getPatientId()).isEqualTo(101L);
    }

    @Test
    void export_worklist_vuot_gioi_han_tra_422_thay_vi_cat_bot_im_lang() {
        when(studyDao.search(any())).thenReturn(new StudyDao.SearchResult(List.of(), 5001));

        assertThatThrownBy(() -> studyController.exportStudies(null, null, null, null, null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(((ApiException) ex).getCode()).isEqualTo("EXPORT_TOO_MANY_ROWS");
                });
        verify(auditLogDao, never()).save(any());
    }

    @Test
    void export_worklist_theo_order_id_lay_moi_ca_cua_chi_dinh_khong_qua_search() {
        Study s1 = new Study();
        s1.setId(601L);
        s1.setOrderId(401L);
        s1.setServiceItemId(10L);
        s1.setAccessionNumber("2026110700001");
        s1.setStatus(StudyStatus.UNREAD);
        Study s2 = new Study();
        s2.setId(602L);
        s2.setOrderId(401L);
        s2.setServiceItemId(10L);
        s2.setAccessionNumber("2026110700002");
        s2.setStatus(StudyStatus.SCHEDULED);
        when(studyDao.findByOrderId(401L)).thenReturn(List.of(s1, s2));

        ResponseEntity<org.springframework.core.io.Resource> response = studyController.exportStudies(
                401L, "UNREAD", null, null, null, null, null);

        String csv = new String(((org.springframework.core.io.ByteArrayResource) response.getBody()).getByteArray(),
                StandardCharsets.UTF_8);
        assertThat(csv).contains("2026110700001").doesNotContain("2026110700002");
        verify(studyDao, never()).search(any());
    }

    private Patient fakePatient(Long id, String code, String name, LocalDate dob, DobPrecision precision) {
        Patient p = new Patient();
        p.setId(id);
        p.setPatientCode(code);
        p.setFullName(name);
        p.setDateOfBirth(dob);
        p.setDobPrecision(precision);
        p.setGender(Gender.M);
        p.setSocialInsuranceNo("DN4010000001234");
        return p;
    }
}
