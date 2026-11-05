package com.vrris.pacs.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Lop goc cho test chay tren MySQL 8 thuc qua Testcontainers (R-05 quyet dinh 4: khong dung H2).
 *
 * "Singleton container" pattern (https://java.testcontainers.org/test_framework_integration/junit_5/#singleton-containers):
 * KHONG dung @Testcontainers/@Container - extension JUnit5 do quan ly lifecycle THEO TUNG TEST
 * CLASS (du field la static), nen se goi stop() sau khi het test cua class dau tien roi start()
 * lai (= container MOI, port MOI) cho class ke tiep, du cac class dung CHUNG field static nay.
 * Tu goi start() mot lan trong static initializer; KHONG goi stop() - container song het tien
 * trinh test, Testcontainers Ryuk don dep khi JVM ket thuc.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("vr_ris_test")
            .withUsername("ris")
            .withPassword("ris_test_pw");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
