package com.vrris.pacs.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.model.AuditLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Unit test (khong can Docker) cho AuditLogAspect + AuditContext: pham vi long nhau, ro ThreadLocal, ghi that bai. */
class AuditLogAspectTest {

    private AuditLogDao auditLogDao;
    private FakeService service;

    /** Service gia - public, khong final de CGLIB proxy duoc. */
    public static class FakeService {

        FakeService self;

        @Audited(action = AuditAction.CREATE, entityType = "OUTER")
        public String outer() {
            AuditContext.recordPatientId(7L);
            self.inner();
            return "ok";
        }

        @Audited(action = AuditAction.CREATE, entityType = "INNER")
        public String inner() {
            AuditContext.recordPatientId(9L);
            return "ok";
        }

        /** Lay patient_id tu tham so NHUNG van ghi them vao AuditContext - truoc day key nay bi ro. */
        @Audited(action = AuditAction.READ, entityType = "PATIENT", patientIdArg = 0)
        public String readWithArg(Long patientId) {
            AuditContext.recordPatientId(999L);
            AuditContext.recordEntityId(888L);
            return "ok";
        }

        @Audited(action = AuditAction.UPDATE, entityType = "STUDY", entityIdArg = 0)
        public void forbidden(Long studyId) {
            AuditContext.recordPatientId(5L);
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_NOT_HOLDER", "khong giu ca");
        }

        @Audited(action = AuditAction.UPDATE, entityType = "STUDY", entityIdArg = 0)
        public void notFound(Long studyId) {
            AuditContext.recordPatientId(5L);
            throw new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND", "khong co");
        }

        @Audited(action = AuditAction.LOGIN, entityType = "APP_USER")
        public void badLogin(String username) {
            AuditContext.recordActorUsername(username);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "sai mat khau");
        }
    }

    @BeforeEach
    void setUp() {
        auditLogDao = mock(AuditLogDao.class);
        AuditLogAspect aspect = new AuditLogAspect(auditLogDao, new ObjectMapper(), mock(PlatformTransactionManager.class));
        FakeService target = new FakeService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);
        service = factory.getProxy();
        target.self = service;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ktv_test", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AuditContext.restore(new AuditContext.Scope(null, null, null, null, null));
    }

    @Test
    void method_long_nhau_moi_method_giu_patient_id_rieng() {
        service.outer();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao, times(2)).save(captor.capture());
        AuditLog inner = captor.getAllValues().get(0);
        AuditLog outer = captor.getAllValues().get(1);
        assertThat(inner.getEntityType()).isEqualTo("INNER");
        assertThat(inner.getPatientId()).isEqualTo(9L);
        assertThat(outer.getEntityType()).isEqualTo("OUTER");
        assertThat(outer.getPatientId()).isEqualTo(7L);
        assertThat(AuditContext.patientId()).isNull();
    }

    @Test
    void id_lay_tu_tham_so_van_xoa_sach_AuditContext_sau_khi_xong() {
        service.readWithArg(42L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        assertThat(captor.getValue().getPatientId()).isEqualTo(42L);
        assertThat(captor.getValue().getEntityId()).isEqualTo(888L);
        assertThat(AuditContext.patientId()).isNull();
        assertThat(AuditContext.entityId()).isNull();
    }

    @Test
    void bi_tu_choi_quyen_403_ghi_ACCESS_DENIED_va_xoa_context() {
        assertThatThrownBy(() -> service.forbidden(11L)).isInstanceOf(ApiException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        AuditLog entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(entry.getEntityId()).isEqualTo(11L);
        assertThat(entry.getPatientId()).isEqualTo(5L);
        assertThat(entry.getActorUsername()).isEqualTo("ktv_test");
        assertThat(entry.getAfterValue()).contains("\"attempted_action\":\"UPDATE\"").contains("REPORT_NOT_HOLDER");
        assertThat(AuditContext.patientId()).isNull();
    }

    @Test
    void that_bai_khong_lien_quan_bao_mat_khong_ghi_audit() {
        assertThatThrownBy(() -> service.notFound(11L)).isInstanceOf(ApiException.class);

        verify(auditLogDao, never()).save(any());
        assertThat(AuditContext.patientId()).isNull();
    }

    @Test
    void dang_nhap_sai_ghi_LOGIN_FAILED_voi_username_da_cat_100_ky_tu() {
        String longName = "u".repeat(150);
        assertThatThrownBy(() -> service.badLogin(longName)).isInstanceOf(ApiException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("LOGIN_FAILED");
        assertThat(captor.getValue().getActorUsername()).hasSize(100);
        assertThat(AuditContext.actorUsername()).isNull();
    }

    @Test
    void chi_tin_X_Forwarded_For_khi_den_tu_proxy_noi_bo() {
        assertThat(AuditLogAspect.isTrustedProxy("127.0.0.1")).isTrue();
        assertThat(AuditLogAspect.isTrustedProxy("10.1.2.3")).isTrue();
        assertThat(AuditLogAspect.isTrustedProxy("192.168.1.10")).isTrue();
        assertThat(AuditLogAspect.isTrustedProxy("::1")).isTrue();
        assertThat(AuditLogAspect.isTrustedProxy("fd00::1")).isTrue();
        assertThat(AuditLogAspect.isTrustedProxy("8.8.8.8")).isFalse();
        assertThat(AuditLogAspect.isTrustedProxy("2001:4860:4860::8888")).isFalse();
        assertThat(AuditLogAspect.isTrustedProxy("proxy.example.com")).isFalse();
        assertThat(AuditLogAspect.isTrustedProxy(null)).isFalse();
    }
}
