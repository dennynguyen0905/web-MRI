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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test tang DAO rieng (khong qua HTTP/MockMvc) tren MySQL 8 thuc qua Testcontainers, cung mau voi
 * PatientDaoTest/ImagingOrderDaoTest (doc/15-kien-truc-backend.md muc 1 - item 7).
 *
 * search(): cac truong hop loc theo tab/time_field/department (gom nhanh con) da duoc test o tang
 * cao hon qua HTTP (StudyControllerTest), vi do la logic cua service (chuan hoa/mo rong truoc khi
 * xuong DAO) - o day chi test truc tiep dieu kien loc + join cua DAO (status/deviceIds/modality/
 * patientClass/patientCode/fullName/department qua join ImagingOrder/Patient/ServiceItem/Device).
 *
 * countByStatus() (IN-09): dung chung commonPredicates() voi search() (xem Javadoc StudyDaoImpl) -
 * o day chi can test group-by dung va mot dieu kien loc tieu bieu (deviceIds), vi cac dieu kien loc
 * con lai (modality/patientClass/department/...) da duoc test ky qua search() o tren, cung logic.
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
        Long patientId = savePatient("TEST0001", "Benh nhan TEST0001").getId();
        Long departmentId = saveDepartment("K01", null).getId();
        orderAId = saveOrder("PH20261031001", patientId, departmentId, "OUTPATIENT").getId();
        orderBId = saveOrder("PH20261031002", patientId, departmentId, "OUTPATIENT").getId();
        serviceItemId = saveServiceItem("CT_SO_NAO", "CT").getId();
        deviceId = saveDevice("CT01", departmentId, "CT").getId();

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
    void search_loc_theo_status() {
        StudyDao.SearchResult byStatus = studyDao.search(criteria(List.of("SCHEDULED"), null, null, null));
        assertThat(byStatus.total()).isEqualTo(2);
        assertThat(byStatus.items()).extracting(Study::getAccessionNumber)
                .containsExactly("2026103100001", "2026103100003");
    }

    @Test
    void search_loc_theo_deviceIds() {
        StudyDao.SearchResult result = studyDao.search(criteria(null, List.of(deviceId), null, null));
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).extracting(Study::getAccessionNumber)
                .containsExactlyInAnyOrder("2026103100001", "2026103100003");
    }

    @Test
    void search_phan_trang() {
        StudyDao.SearchResult firstPage = studyDao.search(pagedCriteria(1, 2));
        assertThat(firstPage.total()).isEqualTo(3);
        assertThat(firstPage.items()).hasSize(2);
    }

    @Test
    void search_loc_theo_modality_qua_join_service_item() {
        Long mrServiceItemId = saveServiceItem("MR_SO_NAO", "MR").getId();
        saveStudy("2026110900001", orderAId, mrServiceItemId, null, "SCHEDULED");

        StudyDao.SearchResult result = studyDao.search(criteria(null, null, "MR", null));
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).extracting(Study::getAccessionNumber).containsExactly("2026110900001");
    }

    @Test
    void search_loc_theo_patient_class_qua_join_order() {
        Long patientId = savePatient("TEST0002", "Benh nhan noi tru").getId();
        Long departmentId = saveDepartment("K02", null).getId();
        Long inpatientOrderId = saveOrder("PH20261109001", patientId, departmentId, "INPATIENT").getId();
        saveStudy("2026110900002", inpatientOrderId, serviceItemId, null, "SCHEDULED");

        StudyDao.SearchResult result = studyDao.search(criteria(null, null, null, "INPATIENT"));
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).extracting(Study::getAccessionNumber).containsExactly("2026110900002");
    }

    @Test
    void search_loc_theo_full_name_khong_phan_biet_hoa_thuong_qua_join_patient() {
        Long patientId = savePatient("TEST0003", "Nguyen Van An").getId();
        Long departmentId = saveDepartment("K03", null).getId();
        Long orderId = saveOrder("PH20261109002", patientId, departmentId, "OUTPATIENT").getId();
        saveStudy("2026110900003", orderId, serviceItemId, null, "SCHEDULED");

        StudyDao.SearchCriteria c = new StudyDao.SearchCriteria(null, Set.of(), null, null, null,
                null, "nguyen van", null, null, "scheduledAt", null, null, 1, 20, "accessionNumber", true);
        StudyDao.SearchResult result = studyDao.search(c);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).extracting(Study::getAccessionNumber).containsExactly("2026110900003");
    }

    @Test
    void search_loc_theo_department_khop_ca_nhanh_ordering_department_va_device() {
        Long patientId = savePatient("TEST0004", "Benh nhan K04").getId();
        Long khoaRiengId = saveDepartment("K04", null).getId();
        Long orderRiengId = saveOrder("PH20261109003", patientId, khoaRiengId, "OUTPATIENT").getId();
        // Ca nay dat o khoa K04 qua chi dinh (order.ordering_department_id), khong gan may.
        saveStudy("2026110900004", orderRiengId, serviceItemId, null, "SCHEDULED");

        Long khoaMayId = saveDepartment("K05", null).getId();
        Long deviceRiengId = saveDevice("CT02", khoaMayId, "CT").getId();
        // Ca nay chi dinh o K01 (departmentId mac dinh cua seed()) nhung may lai thuoc K05.
        saveStudy("2026110900005", orderAId, serviceItemId, deviceRiengId, "SCHEDULED");

        StudyDao.SearchResult matchKhoaRieng = studyDao.search(criteriaWithDepartments(Set.of(khoaRiengId)));
        assertThat(matchKhoaRieng.items()).extracting(Study::getAccessionNumber).containsExactly("2026110900004");

        StudyDao.SearchResult matchKhoaMay = studyDao.search(criteriaWithDepartments(Set.of(khoaMayId)));
        assertThat(matchKhoaMay.items()).extracting(Study::getAccessionNumber).containsExactly("2026110900005");
    }

    @Test
    void countByStatus_dem_dung_theo_tung_trang_thai() {
        Map<String, Long> counts = studyDao.countByStatus(statusCountCriteria(null, null, null));
        assertThat(counts).containsEntry("SCHEDULED", 2L).containsEntry("UNREAD", 1L);
        assertThat(counts).doesNotContainKey("READING");
    }

    @Test
    void countByStatus_ap_dung_bo_loc_deviceIds_giong_search() {
        Map<String, Long> counts = studyDao.countByStatus(statusCountCriteria(List.of(deviceId), null, null));
        assertThat(counts).containsEntry("SCHEDULED", 2L);
        assertThat(counts).doesNotContainKey("UNREAD");
    }

    @Test
    void countByStatus_tra_rong_khi_khong_co_ca_khop_bo_loc() {
        Map<String, Long> counts = studyDao.countByStatus(statusCountCriteria(null, "MR", null));
        assertThat(counts).isEmpty();
    }

    private StudyDao.StatusCountCriteria statusCountCriteria(List<Long> deviceIds, String modality, String patientClass) {
        return new StudyDao.StatusCountCriteria(Set.of(), deviceIds, modality, patientClass,
                null, null, null, null, "scheduledAt", null, null);
    }

    private StudyDao.SearchCriteria criteria(List<String> statuses, List<Long> deviceIds, String modality, String patientClass) {
        return new StudyDao.SearchCriteria(statuses, Set.of(), deviceIds, modality, patientClass,
                null, null, null, null, "scheduledAt", null, null, 1, 20, "accessionNumber", true);
    }

    private StudyDao.SearchCriteria criteriaWithDepartments(Set<Long> departmentIds) {
        return new StudyDao.SearchCriteria(null, departmentIds, null, null, null,
                null, null, null, null, "scheduledAt", null, null, 1, 20, "accessionNumber", true);
    }

    private StudyDao.SearchCriteria pagedCriteria(int page, int pageSize) {
        return new StudyDao.SearchCriteria(null, Set.of(), null, null, null,
                null, null, null, null, "scheduledAt", null, null, page, pageSize, "accessionNumber", true);
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

    private ImagingOrder saveOrder(String orderCode, Long patientId, Long departmentId, String patientClass) {
        ImagingOrder order = new ImagingOrder();
        order.setOrderCode(orderCode);
        order.setPatientId(patientId);
        order.setSource("MANUAL");
        order.setSyncStatus("NOT_SYNCED");
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
        p.setDobPrecision("DAY");
        p.setGender("U");
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
        d.setType("KHOA");
        d.setSortOrder(1);
        d.setIsActive(true);
        return departmentDao.save(d);
    }

    private ServiceItem saveServiceItem(String code, String modality) {
        ServiceItem s = new ServiceItem();
        s.setCode(code);
        s.setName("Chup " + code);
        s.setModality(modality);
        s.setIsActive(true);
        return serviceItemDao.save(s);
    }

    private Device saveDevice(String code, Long departmentId, String modality) {
        Device device = new Device();
        device.setCode(code);
        device.setName("May " + code);
        device.setModality(modality);
        device.setDepartmentId(departmentId);
        device.setIsActive(true);
        return deviceDao.save(device);
    }
}
