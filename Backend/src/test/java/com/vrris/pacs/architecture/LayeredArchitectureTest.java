package com.vrris.pacs.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * R-05 (doc/15-kien-truc-backend.md muc 5): moi luat duoi day = 1 test. Vi pham lam `mvn verify`
 * do - day la co che thuc thi R-05, khong phai chi ghi trong tai lieu.
 */
class LayeredArchitectureTest {

    private static final String BASE = "com.vrris.pacs";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void l1_rest_khong_phu_thuoc_dao_hibernateDao_model() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".rest..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".dao..", BASE + ".hibernateDao..", BASE + ".model..");
        rule.check(classes);
    }

    @Test
    void l2_dto_khong_import_model() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".dto..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".model..");
        rule.check(classes);
    }

    @Test
    void l3_service_khong_dung_hibernateDao_hay_integration_impl() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".service..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".hibernateDao..", BASE + ".integration.impl..");
        rule.check(classes);
    }

    @Test
    void l4_chi_hibernateDao_va_config_duoc_phep_phu_thuoc_hibernateDao() {
        ArchRule rule = noClasses().that().resideOutsideOfPackages(BASE + ".hibernateDao..", BASE + ".config..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".hibernateDao..");
        rule.check(classes);
    }

    @Test
    void l5_rule_chi_phu_thuoc_enu_va_jdk() {
        ArchRule rule = classes().that().resideInAPackage(BASE + ".rule..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(BASE + ".rule..", BASE + ".enu..", "java..");
        rule.check(classes);
    }

    @Test
    void l6_transactional_chi_trong_service_impl() {
        // @Transactional cua Spring dat o METHOD (service.impl.*ServiceImpl), khong dat o class -
        // phai dung methods(), khong dung classes() (classes().areAnnotatedWith chi bat annotation
        // cap class va se bao "khong co class nao duoc kiem" neu dung nham o day).
        ArchRule rule = methods().that().areAnnotatedWith(Transactional.class)
                .should().beDeclaredInClassesThat().resideInAPackage(BASE + ".service.impl..");
        rule.check(classes);
    }

    @Test
    void l7_khong_co_cycle_giua_cac_package() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }

    @Test
    void l8a_lop_RestController_phai_ten_ket_thuc_Controller() {
        ArchRule rule = classes().that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller");
        rule.check(classes);
    }

    @Test
    void l8b_lop_ten_Controller_phai_nam_trong_rest() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage(BASE + ".rest..");
        rule.check(classes);
    }

    @Test
    void l8c_lop_ten_ServiceImpl_phai_nam_trong_service_impl() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("ServiceImpl")
                .should().resideInAPackage(BASE + ".service.impl..");
        rule.check(classes);
    }

    @Test
    void l8d_lop_ten_DaoImpl_phai_nam_trong_hibernateDao() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("DaoImpl")
                .should().resideInAPackage(BASE + ".hibernateDao..");
        rule.check(classes);
    }

    @Test
    void l9_khong_dung_interface_spring_data_jpa_repository() {
        // R-05 quyet dinh 2: DAO tu viet, khong dung JpaRepository/CrudRepository/... Dung ten lop
        // day du (khong import truc tiep) de ArchUnit khong bat buoc Spring Data phai co tren
        // classpath test module nay.
        ArchRule rule = noClasses().should()
                .implement("org.springframework.data.repository.Repository");
        rule.check(classes);
    }
}
