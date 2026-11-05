package com.vrris.pacs.hibernateDao;

import com.vrris.pacs.dao.DepartmentDao;
import com.vrris.pacs.model.Department;
import com.vrris.pacs.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test tang DAO rieng (khong qua HTTP/MockMvc) tren MySQL 8 thuc qua Testcontainers - item 7 cua
 * yeu cau skeleton (doc/15-kien-truc-backend.md muc 1).
 */
@Transactional
class DepartmentDaoTest extends AbstractIntegrationTest {

    @Autowired
    private DepartmentDao departmentDao;

    private Long khoaCdhaId;

    @BeforeEach
    void seed() {
        khoaCdhaId = save("K01", "Khoa CDHA", null, "KHOA", 1).getId();
        save("N01", "May CT", khoaCdhaId, "NHOM", 1);
        save("K02", "Khoa Noi", null, "KHOA", 2);
        save("K99", "Khoa ngung hoat dong", null, "KHOA", 99, false);
    }

    @Test
    void findActiveOrderedByTree_bo_qua_khoa_khong_active_va_sap_cha_truoc_con_sau() {
        List<Department> result = departmentDao.findActiveOrderedByTree();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCode()).isEqualTo("K01");
        assertThat(result.get(0).getParentId()).isNull();
        assertThat(result.get(2).getCode()).isEqualTo("N01");
        assertThat(result.get(2).getParentId()).isEqualTo(khoaCdhaId);
    }

    @Test
    void findById_tra_optional_rong_khi_khong_co() {
        assertThat(departmentDao.findById(-1L)).isEmpty();
    }

    private Department save(String code, String name, Long parentId, String type, int sortOrder) {
        return save(code, name, parentId, type, sortOrder, true);
    }

    private Department save(String code, String name, Long parentId, String type, int sortOrder, boolean active) {
        Department d = new Department();
        d.setCode(code);
        d.setName(name);
        d.setParentId(parentId);
        d.setType(type);
        d.setSortOrder(sortOrder);
        d.setIsActive(active);
        return departmentDao.save(d);
    }
}
