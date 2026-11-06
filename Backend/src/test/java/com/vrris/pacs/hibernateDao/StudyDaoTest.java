package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.Device;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test tang DAO rieng (khong qua HTTP/MockMvc) tren MySQL 8 thuc qua Testcontainers, cung mau voi
 * PatientDaoTest/ImagingOrderDaoTest (doc/15-kien-truc-backend.md muc 1 - item 7).
 */
@Transactional
class StudyDaoTest extends AbstractIntegrationTest {

    @Autowired
    private StudyDao studyDao;

    @Autowired
    private ImagingOrderDao imagingOrderDao;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ServiceItemDao serviceItemDao;

    @Autowired
    private DeviceDao deviceDao;

    private Long orderAId;
    private Long orderBId;
    private Long serviceItemId;
    private Long deviceId;

    @BeforeEach
    void seed() {
        Long patientId = savePatient("TEST0001").getId();
        Long departmentId = saveDepartment("K01").getId();
        orderAId = saveOrder("PH20261031001", patientId, departmentId).getId();
        orderBId = saveOrder("PH20261031002", patientId, departmentId).getId();
        serviceItemId = saveServiceItem("CT_SO_NAO").getId();
        deviceId = saveDevice("CT01", departmentId).getId();

        saveStudy("2026103100001", orderAId, serviceItemId, deviceId, "SCHEDULED");
        saveStudy("2026103100002", orderAId, serviceItemId, null, "UNREAD");
        saveStudy("2026103100003", orderBId, serviceItemId, deviceId, "SCHEDULED");
    }

    @Test
    void findByAccessionNumber_tra_dung_ban_ghi_va_rong_khi_khong_co() {
        assertThat(studyDao.findByAccessionNumber("2026103100001"))
                .get().extracting(Study::getOrderId).isEqualTo(orderAId);
        assertThat(studyDao.findByAccessionNumber("KHONG_TON_TAI")).isEmpty();
    }

    @Test
    void findByOrderId_tra_dung_danh_sach_sap_theo_scheduledAt() {
        assertThat(studyDao.findByOrderId(orderAId)).extracting(Study::getAccessionNumber)
                .containsExactly("2026103100001", "2026103100002");
        assertThat(studyDao.findByOrderId(orderBId)).extracting(Study::getAccessionNumber)
                .containsExactly("2026103100003");
    }

    @Test
    void countByAccessionNumberStartingWith_dem_dung_theo_tien_to_ngay() {
        assertThat(studyDao.countByAccessionNumberStartingWith("20261031")).isEqualTo(3);
        assertThat(studyDao.countByAccessionNumberStartingWith("20270101")).isEqualTo(0);
    }

    @Test
    void search_loc_theo_orderId_va_status() {
        StudyDao.SearchResult byOrder = studyDao.search(
                new StudyDao.SearchCriteria(orderAId, null, null, 1, 20, "accessionNumber", true));
        assertThat(byOrder.total()).isEqualTo(2);

        StudyDao.SearchResult byStatus = studyDao.search(
                new StudyDao.SearchCriteria(null, "SCHEDULED", null, 1, 20, "accessionNumber", true));
        assertThat(byStatus.total()).isEqualTo(2);
        assertThat(byStatus.items()).extracting(Study::getAccessionNumber)
                .containsExactly("2026103100001", "2026103100003");
    }

    @Test
    void search_phan_trang() {
        StudyDao.SearchResult firstPage = studyDao.search(
                new StudyDao.SearchCriteria(null, null, null, 1, 2, "accessionNumber", true));
        assertThat(firstPage.total()).isEqualTo(3);
        assertThat(firstPage.items()).hasSize(2);
    }

    private Study saveStudy(String accessionNumber, Long orderId, Long serviceItemId, Long deviceId, String status) {
        Study study = new Study();
        study.setAccessionNumber(accessionNumber);
        study.setOrderId(orderId);
        study.setServiceItemId(serviceItemId);
        study.setDeviceId(deviceId);
        study.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        study.setScheduledAt(now);
        study.setCreatedAt(now);
        study.setUpdatedAt(now);
        return studyDao.save(study);
    }

    private ImagingOrder saveOrder(String orderCode, Long patientId, Long departmentId) {
        ImagingOrder order = new ImagingOrder();
        order.setOrderCode(orderCode);
        order.setPatientId(patientId);
        order.setSource("MANUAL");
        order.setSyncStatus("NOT_SYNCED");
        order.setOrderingDepartmentId(departmentId);
        order.setOrderingDoctorName("BS Test");
        order.setPatientClass("OUTPATIENT");
        LocalDateTime now = LocalDateTime.now();
        order.setOrderedAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return imagingOrderDao.save(order);
    }

    private Patient savePatient(String code) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFullName("Benh nhan " + code);
        p.setDobPrecision("DAY");
        p.setGender("U");
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return patientDao.save(p);
    }

    private Department saveDepartment(String code) {
        Department d = new Department();
        d.setCode(code);
        d.setName("Khoa " + code);
        d.setType("KHOA");
        d.setSortOrder(1);
        d.setIsActive(true);
        return departmentDao.save(d);
    }

    private ServiceItem saveServiceItem(String code) {
        ServiceItem s = new ServiceItem();
        s.setCode(code);
        s.setName("Chup " + code);
        s.setModality("CT");
        s.setIsActive(true);
        return serviceItemDao.save(s);
    }

    private Device saveDevice(String code, Long departmentId) {
        Device device = new Device();
        device.setCode(code);
        device.setName("May " + code);
        device.setModality("CT");
        device.setDepartmentId(departmentId);
        device.setIsActive(true);
        return deviceDao.save(device);
    }
}
