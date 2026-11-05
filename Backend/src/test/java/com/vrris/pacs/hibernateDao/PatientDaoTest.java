package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.PatientDao;
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
 * DepartmentDaoTest (doc/15-kien-truc-backend.md muc 1 - item 7).
 */
@Transactional
class PatientDaoTest extends AbstractIntegrationTest {

    @Autowired
    private PatientDao patientDao;

    @BeforeEach
    void seed() {
        save("TEST0001", "Nguyen Van Gia A", "GIA0000000001");
        save("TEST0002", "Le Thi Gia B", "GIA0000000002");
        save("TEST0003", "Nguyen Thi Gia C", null);
    }

    @Test
    void findByPatientCode_tra_dung_ban_ghi_va_rong_khi_khong_co() {
        assertThat(patientDao.findByPatientCode("TEST0001"))
                .get().extracting(Patient::getFullName).isEqualTo("Nguyen Van Gia A");
        assertThat(patientDao.findByPatientCode("KHONG_TON_TAI")).isEmpty();
    }

    @Test
    void findBySocialInsuranceNo_tra_dung_ban_ghi() {
        assertThat(patientDao.findBySocialInsuranceNo("GIA0000000002"))
                .get().extracting(Patient::getPatientCode).isEqualTo("TEST0002");
        assertThat(patientDao.findBySocialInsuranceNo("KHONG_TON_TAI")).isEmpty();
    }

    @Test
    void search_loc_theo_q_tim_tren_ca_ma_va_ho_ten() {
        PatientDao.SearchCriteria criteria =
                new PatientDao.SearchCriteria("Nguyen", null, null, 1, 20, "patientCode", true);

        PatientDao.SearchResult result = patientDao.search(criteria);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).extracting(Patient::getPatientCode)
                .containsExactly("TEST0001", "TEST0003");
    }

    @Test
    void search_phan_trang_va_sap_xep() {
        PatientDao.SearchResult firstPage = patientDao.search(
                new PatientDao.SearchCriteria(null, null, null, 1, 2, "patientCode", true));
        assertThat(firstPage.total()).isEqualTo(3);
        assertThat(firstPage.items()).extracting(Patient::getPatientCode)
                .containsExactly("TEST0001", "TEST0002");

        PatientDao.SearchResult secondPage = patientDao.search(
                new PatientDao.SearchCriteria(null, null, null, 2, 2, "patientCode", true));
        assertThat(secondPage.items()).extracting(Patient::getPatientCode)
                .containsExactly("TEST0003");
    }

    private Patient save(String code, String fullName, String socialInsuranceNo) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFullName(fullName);
        p.setDobPrecision("DAY");
        p.setGender("U");
        p.setSocialInsuranceNo(socialInsuranceNo);
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return patientDao.save(p);
    }
}
