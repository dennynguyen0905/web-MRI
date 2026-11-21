package com.vrris.pacs.rule;

import com.vrris.pacs.enu.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test luat vai tro/dieu kien T2..T10 (doc/03-state-machine.md muc 2.2, 2.4, 2.5; doc/06 muc 4). */
class TransitionPolicyTest {

    private static final long READER = 10L;
    private static final long OTHER_READER = 11L;
    private static final long APPROVER = 20L;
    private static final long OTHER_APPROVER = 21L;
    private static final long ADMIN = 1L;
    private static final long TECH = 2L;
    private static final String UID = "1.2.840.10008.1";

    private static TransitionPolicy.Actor actor(long id, UserRole... roles) {
        return new TransitionPolicy.Actor(id, roles.length == 0 ? Set.of() : EnumSet.of(roles[0], roles));
    }

    private static TransitionPolicy.StudyFacts study(String uid, Long reading, Long approving) {
        return new TransitionPolicy.StudyFacts(uid, reading, approving);
    }

    private static TransitionPolicy.ReportFacts report(String technique, String conclusion, Long createdBy) {
        return new TransitionPolicy.ReportFacts(technique, conclusion, createdBy);
    }

    private static void assertAllowed(Optional<TransitionPolicy.Violation> result) {
        assertThat(result).isEmpty();
    }

    private static void assertViolation(Optional<TransitionPolicy.Violation> result,
                                        TransitionPolicy.Kind kind, String code) {
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.kind()).isEqualTo(kind);
            assertThat(v.code()).isEqualTo(code);
        });
    }

    @Nested
    class VaiTro {

        /** Bang 2.5 doc/03: vai tro nao duoc phep moi buoc (bo qua dieu kien - dung du lieu hop le). */
        @Test
        void CLINICIAN_khong_duoc_buoc_nao() {
            for (TransitionCode code : TransitionCode.values()) {
                assertViolation(TransitionPolicy.check(code, actor(99L, UserRole.CLINICIAN),
                                study(UID, 99L, 99L), report("k", "c", 50L), "ly do"),
                        TransitionPolicy.Kind.FORBIDDEN, "FORBIDDEN");
            }
        }

        @Test
        void ADMIN_khong_duoc_doc_va_duyet_T3_T5_T6_T8() {
            for (TransitionCode code : new TransitionCode[]{TransitionCode.T3, TransitionCode.T5,
                    TransitionCode.T6, TransitionCode.T8}) {
                assertThat(TransitionPolicy.check(code, actor(ADMIN, UserRole.ADMIN),
                        study(UID, ADMIN, ADMIN), report("k", "c", 50L), "ly do"))
                        .get().extracting(TransitionPolicy.Violation::kind).isEqualTo(TransitionPolicy.Kind.FORBIDDEN);
            }
        }

        @Test
        void ADMIN_khong_duoc_duyet_T7_ma_rieng() {
            assertViolation(TransitionPolicy.check(TransitionCode.T7, actor(ADMIN, UserRole.ADMIN),
                            study(UID, READER, ADMIN), report("k", "c", READER), null),
                    TransitionPolicy.Kind.FORBIDDEN, "ADMIN_CANNOT_APPROVE");
        }

        @Test
        void RADIOLOGIST_khong_duoc_T9_va_T10() {
            assertViolation(TransitionPolicy.check(TransitionCode.T9, actor(READER, UserRole.RADIOLOGIST),
                    study(UID, READER, null), null, "ly do"), TransitionPolicy.Kind.FORBIDDEN, "FORBIDDEN");
            assertViolation(TransitionPolicy.check(TransitionCode.T10, actor(READER, UserRole.RADIOLOGIST),
                    study(null, null, null), null, "ly do"), TransitionPolicy.Kind.FORBIDDEN, "FORBIDDEN");
        }
    }

    @Nested
    class T2 {
        @Test
        void technician_chuyen_duoc_khi_da_co_uid() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T2, actor(TECH, UserRole.TECHNICIAN),
                    study(UID, null, null), null, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void uid_rong_tra_422(String uid) {
            assertViolation(TransitionPolicy.check(TransitionCode.T2, actor(TECH, UserRole.TECHNICIAN),
                    study(uid, null, null), null, null), TransitionPolicy.Kind.UNPROCESSABLE, "STUDY_NO_DICOM_IMAGE");
            assertViolation(TransitionPolicy.check(TransitionCode.T2, actor(TECH, UserRole.TECHNICIAN),
                    study(null, null, null), null, null), TransitionPolicy.Kind.UNPROCESSABLE, "STUDY_NO_DICOM_IMAGE");
        }
    }

    @Nested
    class T3 {
        @Test
        void radiologist_mo_ca_chua_khoa() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T3, actor(READER, UserRole.RADIOLOGIST),
                    study(UID, null, null), null, null));
        }

        @Test
        void ca_dang_bi_nguoi_khac_khoa_tra_409() {
            assertViolation(TransitionPolicy.check(TransitionCode.T3, actor(OTHER_READER, UserRole.RADIOLOGIST),
                    study(UID, READER, null), null, null), TransitionPolicy.Kind.CONFLICT, "STUDY_LOCKED_BY_ANOTHER");
        }
    }

    @Nested
    class T4 {
        @Test
        void nguoi_dang_doc_bo_do_co_ly_do() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T4, actor(READER, UserRole.RADIOLOGIST),
                    study(UID, READER, null), null, "Bac si nghi dot xuat"));
        }

        @Test
        void admin_giai_phong_khoa_cua_nguoi_khac() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T4, actor(ADMIN, UserRole.ADMIN),
                    study(UID, READER, null), null, "Giai phong khoa"));
        }

        @Test
        void radiologist_khac_tra_403_REPORT_NOT_HOLDER() {
            assertViolation(TransitionPolicy.check(TransitionCode.T4, actor(OTHER_READER, UserRole.RADIOLOGIST),
                    study(UID, READER, null), null, "ly do"), TransitionPolicy.Kind.FORBIDDEN, "REPORT_NOT_HOLDER");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t\n"})
        void ly_do_rong_hoac_toan_khoang_trang_tra_422(String reason) {
            assertViolation(TransitionPolicy.check(TransitionCode.T4, actor(READER, UserRole.RADIOLOGIST),
                            study(UID, READER, null), null, reason),
                    TransitionPolicy.Kind.UNPROCESSABLE, "TRANSITION_REASON_REQUIRED");
        }

        @Test
        void ly_do_null_tra_422() {
            assertViolation(TransitionPolicy.check(TransitionCode.T4, actor(READER, UserRole.RADIOLOGIST),
                            study(UID, READER, null), null, null),
                    TransitionPolicy.Kind.UNPROCESSABLE, "TRANSITION_REASON_REQUIRED");
        }
    }

    @Nested
    class T5 {
        @Test
        void nguoi_dang_doc_chot_khi_du_ky_thuat_va_ket_luan() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T5, actor(READER, UserRole.RADIOLOGIST),
                    study(UID, READER, null), report("CT", "Binh thuong", READER), null));
        }

        @Test
        void nguoi_khac_tra_403() {
            assertViolation(TransitionPolicy.check(TransitionCode.T5, actor(OTHER_READER, UserRole.RADIOLOGIST),
                            study(UID, READER, null), report("CT", "Binh thuong", READER), null),
                    TransitionPolicy.Kind.FORBIDDEN, "REPORT_NOT_HOLDER");
        }

        @Test
        void thieu_bao_cao_hoac_truong_rong_tra_422() {
            TransitionPolicy.Actor reader = actor(READER, UserRole.RADIOLOGIST);
            TransitionPolicy.StudyFacts s = study(UID, READER, null);
            assertViolation(TransitionPolicy.check(TransitionCode.T5, reader, s, null, null),
                    TransitionPolicy.Kind.UNPROCESSABLE, "REPORT_INCOMPLETE");
            assertViolation(TransitionPolicy.check(TransitionCode.T5, reader, s, report(" ", "KL", READER), null),
                    TransitionPolicy.Kind.UNPROCESSABLE, "REPORT_INCOMPLETE");
            assertViolation(TransitionPolicy.check(TransitionCode.T5, reader, s, report("KT", null, READER), null),
                    TransitionPolicy.Kind.UNPROCESSABLE, "REPORT_INCOMPLETE");
        }
    }

    @Nested
    class T6T7 {
        @Test
        void approver_khac_nguoi_doc_mo_duyet_va_duyet() {
            TransitionPolicy.Actor approver = actor(APPROVER, UserRole.APPROVER);
            assertAllowed(TransitionPolicy.check(TransitionCode.T6, approver,
                    study(UID, READER, null), report("k", "c", READER), null));
            assertAllowed(TransitionPolicy.check(TransitionCode.T7, approver,
                    study(UID, READER, APPROVER), report("k", "c", READER), null));
        }

        @Test
        void nguoi_doc_co_ca_vai_tro_approver_khong_duyet_duoc_ca_minh() {
            TransitionPolicy.Actor dual = actor(READER, UserRole.RADIOLOGIST, UserRole.APPROVER);
            assertViolation(TransitionPolicy.check(TransitionCode.T6, dual,
                            study(UID, READER, null), report("k", "c", READER), null),
                    TransitionPolicy.Kind.FORBIDDEN, "APPROVER_CANNOT_BE_READER");
            // Nguoi tao bao cao (khac reading_user_id hien tai) cung khong duoc duyet.
            TransitionPolicy.Actor author = actor(OTHER_READER, UserRole.APPROVER);
            assertViolation(TransitionPolicy.check(TransitionCode.T6, author,
                            study(UID, READER, null), report("k", "c", OTHER_READER), null),
                    TransitionPolicy.Kind.FORBIDDEN, "APPROVER_CANNOT_BE_READER");
        }

        @Test
        void T7_approver_khong_giu_duyet_tra_403() {
            assertViolation(TransitionPolicy.check(TransitionCode.T7, actor(OTHER_APPROVER, UserRole.APPROVER),
                            study(UID, READER, APPROVER), report("k", "c", READER), null),
                    TransitionPolicy.Kind.FORBIDDEN, "NOT_APPROVING_HOLDER");
        }

        @Test
        void T7_khong_co_bao_cao_tra_422() {
            assertViolation(TransitionPolicy.check(TransitionCode.T7, actor(APPROVER, UserRole.APPROVER),
                            study(UID, READER, APPROVER), null, null),
                    TransitionPolicy.Kind.UNPROCESSABLE, "REPORT_INCOMPLETE");
        }
    }

    @Nested
    class T8 {
        @Test
        void nguoi_dang_giu_duyet_tra_lai_co_ly_do() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T8, actor(APPROVER, UserRole.APPROVER),
                    study(UID, READER, APPROVER), report("k", "c", READER), "Thieu mo ta"));
        }

        @Test
        void approver_khac_khong_giu_duyet_tra_403() {
            assertViolation(TransitionPolicy.check(TransitionCode.T8, actor(OTHER_APPROVER, UserRole.APPROVER),
                            study(UID, READER, APPROVER), report("k", "c", READER), "ly do"),
                    TransitionPolicy.Kind.FORBIDDEN, "NOT_APPROVING_HOLDER");
        }

        @Test
        void thieu_ly_do_tra_422() {
            assertViolation(TransitionPolicy.check(TransitionCode.T8, actor(APPROVER, UserRole.APPROVER),
                            study(UID, READER, APPROVER), report("k", "c", READER), "  "),
                    TransitionPolicy.Kind.UNPROCESSABLE, "TRANSITION_REASON_REQUIRED");
        }
    }

    @Nested
    class T9T10 {
        @Test
        void admin_va_approver_mo_lai_co_ly_do() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T9, actor(ADMIN, UserRole.ADMIN),
                    study(UID, READER, null), null, "Sai ket luan"));
            assertAllowed(TransitionPolicy.check(TransitionCode.T9, actor(APPROVER, UserRole.APPROVER),
                    study(UID, READER, null), null, "Sai ket luan"));
            assertViolation(TransitionPolicy.check(TransitionCode.T9, actor(ADMIN, UserRole.ADMIN),
                    study(UID, READER, null), null, null), TransitionPolicy.Kind.UNPROCESSABLE, "TRANSITION_REASON_REQUIRED");
        }

        @Test
        void T10_huy_ca_chua_co_anh() {
            assertAllowed(TransitionPolicy.check(TransitionCode.T10, actor(TECH, UserRole.TECHNICIAN),
                    study(null, null, null), null, "BN khong den"));
        }

        @Test
        void T10_ca_da_co_anh_tra_422_truoc_khi_xet_ly_do() {
            assertViolation(TransitionPolicy.check(TransitionCode.T10, actor(TECH, UserRole.TECHNICIAN),
                            study(UID, null, null), null, null),
                    TransitionPolicy.Kind.UNPROCESSABLE, "CANNOT_CANCEL_STUDY_WITH_IMAGE");
        }
    }
}
