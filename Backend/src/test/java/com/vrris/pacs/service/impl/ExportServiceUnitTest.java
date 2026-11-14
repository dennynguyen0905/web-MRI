package com.vrris.pacs.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.audit.AuditLogAspect;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ReportDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dao.StudyStatusHistoryDao;
import com.vrris.pacs.dto.request.ListExportEventRequest;
import com.vrris.pacs.dto.response.ExportDocumentResponse;
import com.vrris.pacs.mapper.DeviceMapper;
import com.vrris.pacs.mapper.ImagingOrderMapper;
import com.vrris.pacs.mapper.PatientMapper;
import com.vrris.pacs.mapper.ReportMapper;
import com.vrris.pacs.mapper.ServiceItemMapper;
import com.vrris.pacs.mapper.StudyMapper;
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
import com.vrris.pacs.service.StudyService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private ImagingOrderDao imagingOrderDao;
    private ServiceItemDao serviceItemDao;
    private DeviceDao deviceDao;
    private ReportDao reportDao;

    private PatientService proxiedPatientService;
    private StudyService proxiedStudyService;
    private PatientController patientController;
    private StudyController studyController;

    @BeforeEach
    void setUp() {
        auditLogDao = mock(AuditLogDao.class);
        patientDao = mock(PatientDao.class);
        studyDao = mock(StudyDao.class);
        imagingOrderDao = mock(ImagingOrderDao.class);
        serviceItemDao = mock(ServiceItemDao.class);
        deviceDao = mock(DeviceDao.class);
        reportDao = mock(ReportDao.class);

        AuditLogAspect aspect = new AuditLogAspect(auditLogDao, new ObjectMapper());

        PatientServiceImpl rawPatientService = new PatientServiceImpl(patientDao, mock(PatientMapper.class));
        AspectJProxyFactory patientProxyFactory = new AspectJProxyFactory(rawPatientService);
        patientProxyFactory.addAspect(aspect);
        proxiedPatientService = patientProxyFactory.getProxy();

        StudyServiceImpl rawStudyService = new StudyServiceImpl(
                studyDao, imagingOrderDao, serviceItemDao, deviceDao, mock(DepartmentDao.class),
                patientDao, reportDao, mock(AppUserDao.class), mock(StudyStatusHistoryDao.class),
                mock(StudyMapper.class), mock(ImagingOrderMapper.class),
                mock(ServiceItemMapper.class), mock(DeviceMapper.class), mock(PatientMapper.class),
                mock(ReportMapper.class));
        AspectJProxyFactory studyProxyFactory = new AspectJProxyFactory(rawStudyService);
        studyProxyFactory.addAspect(aspect);
        proxiedStudyService = studyProxyFactory.getProxy();

        patientController = new PatientController(proxiedPatientService);
        studyController = new StudyController(proxiedStudyService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ktv_test", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void export_danh_sach_nhieu_benh_nhan_ghi_audit_EXPORT_voi_patient_id_null() {
        Patient p1 = fakePatient(101L, "BN-FAKE-201", "Nguyễn Văn Giả Một", LocalDate.of(1958, 1, 1), "YEAR");
        Patient p2 = fakePatient(102L, "BN-FAKE-202", "Trần Thị Giả Hai", null, "DAY");
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
        assertThat(log.getAction()).isEqualTo("EXPORT");
        assertThat(log.getEntityType()).isEqualTo("PATIENT");
        assertThat(log.getActorUsername()).isEqualTo("ktv_test");
        assertThat(log.getPatientId()).isNull();
    }

    @Test
    void export_danh_sach_1_benh_nhan_ghi_audit_EXPORT_kem_patient_id() {
        Patient p1 = fakePatient(101L, "BN-FAKE-201", "Nguyễn Văn Giả Một", LocalDate.of(1980, 5, 10), "DAY");
        when(patientDao.search(any())).thenReturn(new PatientDao.SearchResult(List.of(p1), 1));

        ExportDocumentResponse doc = proxiedPatientService.exportCsv(null, "BN-FAKE-201", null, null);
        assertThat(doc.rowCount()).isEqualTo(1);
        assertThat(doc.patientId()).isEqualTo(101L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("EXPORT");
        assertThat(log.getPatientId()).isEqualTo(101L);
    }

    @Test
    void export_events_ghi_audit_EXPORT_cho_in_danh_sach_trinh_duyet() {
        patientController.recordExportEvent(
                new ListExportEventRequest("STUDIES", "PRINT", 12, null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("EXPORT");
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
        s1.setStatus("SCHEDULED");
        s1.setScheduledAt(LocalDateTime.of(2026, 11, 7, 9, 0));

        ImagingOrder order = new ImagingOrder();
        order.setId(301L);
        order.setPatientId(101L);
        order.setOrderCode("ORD-20261107-0001");
        order.setVisitCode("LSVV-FAKE-01");
        order.setPatientClass("OUTPATIENT");
        order.setOrderingDoctorName("BS. Lê Anh Tuấn");
        order.setClinicalDiagnosis("Đau đầu kéo dài");

        Patient patient = fakePatient(101L, "BN-FAKE-201", "Nguyễn Văn Giả Một", LocalDate.of(1958, 1, 1), "YEAR");

        ServiceItem service = new ServiceItem();
        service.setId(10L);
        service.setName("Chụp cộng hưởng từ sọ não");
        service.setModality("MR");
        service.setBodyPart("Sọ não");

        Device device = new Device();
        device.setId(20L);
        device.setName("MRI 1.5T Phòng 1");

        Report report = new Report();
        report.setId(701L);
        report.setStudyId(501L);
        report.setConclusion("Bình thường");

        when(studyDao.search(any())).thenReturn(new StudyDao.SearchResult(List.of(s1), 1));
        when(imagingOrderDao.findById(301L)).thenReturn(Optional.of(order));
        when(patientDao.findById(101L)).thenReturn(Optional.of(patient));
        when(serviceItemDao.findById(10L)).thenReturn(Optional.of(service));
        when(deviceDao.findById(20L)).thenReturn(Optional.of(device));
        when(reportDao.findCurrentByStudyId(501L)).thenReturn(Optional.of(report));

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
        assertThat(log.getAction()).isEqualTo("EXPORT");
        assertThat(log.getEntityType()).isEqualTo("STUDY");
        assertThat(log.getPatientId()).isEqualTo(101L);
    }

    private Patient fakePatient(Long id, String code, String name, LocalDate dob, String precision) {
        Patient p = new Patient();
        p.setId(id);
        p.setPatientCode(code);
        p.setFullName(name);
        p.setDateOfBirth(dob);
        p.setDobPrecision(precision);
        p.setGender("M");
        p.setSocialInsuranceNo("DN4010000001234");
        return p;
    }
}
