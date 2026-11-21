package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.PatientDao;
import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Patient;
import com.vrris.pacs.model.ServiceItem;
import com.vrris.pacs.model.Study;
import com.vrris.pacs.service.StudyTransitionService;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Khoa lac quan (V9 row_version) cho luong ca chup - doc/03-state-machine.md muc 2.4 dieu 5, SM-16.
 *
 * KHONG dung @Transactional o muc test: can hai transaction that, commit that, de thay khoa lac
 * quan/UNIQUE hoat dong (test @Transactional chi co mot transaction va khong bao gio flush/commit).
 * Du lieu duoc tao trong transaction rieng va xoa o @AfterEach.
 */
class StudyTransitionConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private StudyTransitionService studyTransitionService;
    @Autowired
    private StudyDao studyDao;
    @Autowired
    private ImagingOrderDao imagingOrderDao;
    @Autowired
    private PatientDao patientDao;
    @Autowired
    private ServiceItemDao serviceItemDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private TransactionTemplate requiresNew;

    private Long departmentId;
    private Long patientId;
    private Long serviceItemId;
    private Long orderId;
    private Long studyId;
    private AppUser rad1;
    private AppUser rad2;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        tx.executeWithoutResult(status -> {
            Department dept = new Department();
            dept.setCode("K_CONC");
            dept.setName("Khoa CDHA");
            dept.setType("KHOA");
            dept.setSortOrder(1);
            dept.setIsActive(true);
            departmentId = departmentDao.save(dept).getId();

            rad1 = saveUser("rad1_conc");
            rad2 = saveUser("rad2_conc");

            Patient patient = new Patient();
            patient.setPatientCode("BN_CONC_01");
            patient.setFullName("Benh nhan gia lap");
            patient.setDobPrecision("DAY");
            patient.setGender("U");
            patient.setCreatedAt(LocalDateTime.now());
            patient.setUpdatedAt(LocalDateTime.now());
            patientId = patientDao.save(patient).getId();

            ServiceItem item = new ServiceItem();
            item.setCode("CT_CONC");
            item.setName("CT so nao");
            item.setModality("CT");
            item.setIsActive(true);
            serviceItemId = serviceItemDao.save(item).getId();

            ImagingOrder order = new ImagingOrder();
            order.setOrderCode("ORD_CONC_01");
            order.setPatientId(patientId);
            order.setOrderingDepartmentId(departmentId);
            order.setOrderingDoctorName("BS Lam Sang");
            order.setPatientClass("OUTPATIENT");
            order.setSource("MANUAL");
            order.setSyncStatus("NOT_SYNCED");
            order.setOrderedAt(LocalDateTime.now());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderId = imagingOrderDao.save(order).getId();

            Study study = new Study();
            study.setAccessionNumber("ACC_CONC_1");
            study.setOrderId(orderId);
            study.setServiceItemId(serviceItemId);
            study.setStatus("UNREAD");
            study.setStudyInstanceUid("1.2.840.10008.777.1");
            study.setScheduledAt(LocalDateTime.now());
            study.setCreatedAt(LocalDateTime.now());
            study.setUpdatedAt(LocalDateTime.now());
            studyId = studyDao.save(study).getId();
        });
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM audit_log WHERE actor_username IN ('rad1_conc', 'rad2_conc') OR patient_id = ?",
                patientId);
        jdbcTemplate.update("DELETE FROM study_status_history WHERE study_id = ?", studyId);
        jdbcTemplate.update("DELETE FROM report WHERE study_id = ?", studyId);
        jdbcTemplate.update("DELETE FROM study WHERE id = ?", studyId);
        jdbcTemplate.update("DELETE FROM imaging_order WHERE id = ?", orderId);
        jdbcTemplate.update("DELETE FROM patient WHERE id = ?", patientId);
        jdbcTemplate.update("DELETE FROM service_item WHERE id = ?", serviceItemId);
        jdbcTemplate.update("DELETE FROM app_user WHERE username IN ('rad1_conc', 'rad2_conc')");
        jdbcTemplate.update("DELETE FROM department WHERE id = ?", departmentId);
    }

    /**
     * SM-16: hai bac si cung mo doc (T3) gan nhu dong thoi. Bac si 1 doc ca (transaction 1 dang mo),
     * bac si 2 mo doc va commit truoc, roi bac si 1 moi chuyen - yeu cau cua bac si 1 phai that bai,
     * nguoi giu ca la bac si 2.
     */
    @Test
    void hai_bac_si_cung_mo_doc_chi_mot_nguoi_thanh_cong() {
        Throwable thrown = catchThrowable(() -> tx.executeWithoutResult(status -> {
            studyDao.findById(studyId).orElseThrow();

            runAs(rad2, () -> requiresNew.executeWithoutResult(inner ->
                    studyTransitionService.transition(studyId, new TransitionRequest(StudyStatus.READING, null, null))));

            runAs(rad1, () ->
                    studyTransitionService.transition(studyId, new TransitionRequest(StudyStatus.READING, null, null)));
        }));

        assertThat(thrown).isInstanceOfAny(ObjectOptimisticLockingFailureException.class,
                DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT reading_user_id FROM study WHERE id = ?", Long.class, studyId))
                .isEqualTo(rad2.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM report WHERE study_id = ?", Integer.class, studyId))
                .isEqualTo(1);
    }

    /** @Version tren Study: ghi de ban da cu (row_version da tang o transaction khac) bi tu choi khi commit. */
    @Test
    void ghi_ban_study_da_cu_bi_tu_choi_boi_row_version() {
        Throwable thrown = catchThrowable(() -> tx.executeWithoutResult(status -> {
            Study stale = studyDao.findById(studyId).orElseThrow();
            requiresNew.executeWithoutResult(inner -> jdbcTemplate.update(
                    "UPDATE study SET row_version = row_version + 1 WHERE id = ?", studyId));
            stale.setUpdatedAt(LocalDateTime.now());
        }));

        assertThat(thrown).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private void runAs(AppUser user, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_RADIOLOGIST"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private AppUser saveUser(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz12");
        user.setFullName("Nguoi dung " + username);
        user.setEmail(username + "@vrris.vn");
        user.setDepartmentId(departmentId);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return appUserDao.save(user);
    }
}
