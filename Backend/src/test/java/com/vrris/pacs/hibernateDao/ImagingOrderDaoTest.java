package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test tang DAO rieng (khong qua HTTP/MockMvc) tren MySQL 8 thuc qua Testcontainers, cung mau voi
 * PatientDaoTest/DepartmentDaoTest (doc/15-kien-truc-backend.md muc 1 - item 7).
 */
@Transactional
class ImagingOrderDaoTest extends AbstractIntegrationTest {

    @Autowired
    private ImagingOrderDao imagingOrderDao;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private DepartmentDao departmentDao;

    private Long patientAId;
    private Long patientBId;
    private Long departmentId;

    @BeforeEach
    void seed() {
        patientAId = savePatient("TEST0001").getId();
        patientBId = savePatient("TEST0002").getId();
        departmentId = saveDepartment("K01").getId();

        saveOrder("PH20261031001", patientAId, "LSVV001");
        saveOrder("PH20261031002", patientAId, "LSVV002");
        saveOrder("PH20261101001", patientBId, null);
    }

    @Test
    void findByOrderCode_tra_dung_ban_ghi_va_rong_khi_khong_co() {
        assertThat(imagingOrderDao.findByOrderCode("PH20261031001"))
                .get().extracting(ImagingOrder::getPatientId).isEqualTo(patientAId);
        assertThat(imagingOrderDao.findByOrderCode("KHONG_TON_TAI")).isEmpty();
    }

    @Test
    void countByOrderCodeStartingWith_dem_dung_theo_tien_to_ngay() {
        assertThat(imagingOrderDao.countByOrderCodeStartingWith("PH20261031")).isEqualTo(2);
        assertThat(imagingOrderDao.countByOrderCodeStartingWith("PH20261101")).isEqualTo(1);
        assertThat(imagingOrderDao.countByOrderCodeStartingWith("PH20270101")).isEqualTo(0);
    }

    @Test
    void search_loc_theo_patientId() {
        ImagingOrderDao.SearchResult result = imagingOrderDao.search(
                new ImagingOrderDao.SearchCriteria(patientAId, null, null, 1, 20, "orderCode", true));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).extracting(ImagingOrder::getOrderCode)
                .containsExactly("PH20261031001", "PH20261031002");
    }

    @Test
    void search_loc_theo_visitCode() {
        ImagingOrderDao.SearchResult result = imagingOrderDao.search(
                new ImagingOrderDao.SearchCriteria(null, null, "LSVV002", 1, 20, "orderCode", true));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().get(0).getOrderCode()).isEqualTo("PH20261031002");
    }

    @Test
    void search_phan_trang() {
        ImagingOrderDao.SearchResult firstPage = imagingOrderDao.search(
                new ImagingOrderDao.SearchCriteria(null, null, null, 1, 2, "orderCode", true));
        assertThat(firstPage.total()).isEqualTo(3);
        assertThat(firstPage.items()).hasSize(2);

        ImagingOrderDao.SearchResult secondPage = imagingOrderDao.search(
                new ImagingOrderDao.SearchCriteria(null, null, null, 2, 2, "orderCode", true));
        assertThat(secondPage.items()).hasSize(1);
    }

    private ImagingOrder saveOrder(String orderCode, Long patientId, String visitCode) {
        ImagingOrder order = new ImagingOrder();
        order.setOrderCode(orderCode);
        order.setPatientId(patientId);
        order.setVisitCode(visitCode);
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
}
