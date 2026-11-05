-- V8: bổ sung dữ liệu giả còn THIẾU so với yêu cầu test case ở doc/06-rbac-matrix.md mục 7 và
-- doc/12-seed-data-va-kich-ban-demo.md. KHÔNG sửa V1-V7 đã vào main (quy tắc ở database/README.md).
-- Toàn bộ dữ liệu dưới đây là dữ liệu giả, không dùng ngoài dev/demo (giống V7).
--
-- Tóm tắt những gì V8 thêm (xem doc/12-seed-data-va-kich-ban-demo.md mục 3.2 để biết test case nào dùng):
--   1. Hai tài khoản demo thứ hai (RADIOLOGIST, APPROVER) để thử khóa mềm và "người duyệt khác người đọc"
--      (doc/06-rbac-matrix.md mục 7, RBAC-09/10/14 - tạm, chưa có code chạy test này).
--   2. Một chỉ định có ordering_doctor_name khớp đúng app_user.full_name của bslamsang, để minh họa
--      đề xuất row-level D34 (doc/06-rbac-matrix.md mục 5) - KHỚP DỮ LIỆU, chưa phải code chạy được.
--   3. Năm chỉ định được gắn his_order_id, khớp đúng 5/7 bản ghi trong
--      Backend/src/main/resources/his-mock/his-orders.json (schema + đường dẫn theo
--      doc/08-hl7-message-spec.md mục 9 — Agent 1 đặc tả, Agent 2 sinh file thật): order 1 (TEST0001)
--      khớp hoàn toàn; order 2 (TEST0002) khớp mã HIS của chính known issue README 2.5 (năm sinh
--      1958 RIS vs 1952 HIS — đã có identity_conflict từ V7, không tạo thêm); order 3 (TEST0003)
--      lệch họ tên có chủ ý (kiểu RC-01 ở doc/04-his-ris-reconciliation.md) kèm identity_conflict
--      mới; order 7 (TEST0006) khớp hoàn toàn; order 8 (TEST0007) lệch giới tính có chủ ý (kiểu
--      RC-12) kèm identity_conflict mới. Hai bản ghi còn lại trong his-orders.json (TEST0009 —
--      bệnh nhân mới; TEST0010 — trùng BHXH với TEST0004, kiểu RC-07) KHÔNG gắn vào order nào có
--      sẵn — đúng bản chất "chưa có trong RIS", dùng khi gọi JsonFileHisGateway trực tiếp hoặc khi
--      demo luồng tạo chỉ định mới.
--   4. Một ca CANCELLED (T10, doc/03-state-machine.md) - trạng thái này còn ❓ chưa được nhóm xác nhận
--      (doc/02-erd.md mục 9) và CHƯA có trong enum Backend (`enu.StudyStatus`, chỉ 6 giá trị). Ghi rõ:
--      đây là dữ liệu ở tầng DB (status là VARCHAR, không ràng buộc ENUM) để minh họa đề xuất, KHÔNG
--      có nghĩa là backend đã xử lý được CANCELLED.

-- ---------------------------------------------------------------- 1. Tài khoản demo thứ hai cho RADIOLOGIST/APPROVER
-- Cùng hash BCrypt với V7 (mật khẩu demo: 123456).
INSERT INTO app_user (id, username, password_hash, full_name, email, department_id) VALUES
    (6, 'bscdha2',  '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'BS. CĐHA Hai (demo)', 'bscdha2@vr-ris.local', 1),
    (7, 'bsduyet2', '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'BS. Duyệt Hai (demo)', 'bsduyet2@vr-ris.local', 1);

INSERT INTO user_role (user_id, role_id) VALUES (6, 4), (7, 5);

-- ---------------------------------------------------------------- 2. Khớp ordering_doctor_name với một app_user (D34)
-- Trước: 'BS. Chỉ Định C (giả)'. Chỉ định này (bệnh nhân TEST0004) đổi bác sĩ chỉ định thành đúng tên
-- đăng nhập 'bslamsang' để minh họa đề xuất "CLINICIAN chỉ thấy chỉ định do mình chỉ định" - đọc
-- doc/06-rbac-matrix.md mục 5 (D34) trước khi dùng dòng này, đề xuất đó CHƯA code chạy được.
UPDATE imaging_order SET ordering_doctor_name = 'BS. Lâm Sàng (demo)' WHERE id = 4;

-- ---------------------------------------------------------------- 3. his_order_id khớp Backend/src/main/resources/his-mock/his-orders.json
UPDATE imaging_order SET his_order_id = 'HISO2026110500007' WHERE id = 1; -- TEST0001, khớp hoàn toàn
UPDATE imaging_order SET his_order_id = 'HISO2026110500008' WHERE id = 2; -- TEST0002, DOB 1952 vs 1958 (README 2.5)
UPDATE imaging_order SET his_order_id = 'HISO2026110500011' WHERE id = 3; -- TEST0003, lệch họ tên (RC-01)
UPDATE imaging_order SET his_order_id = 'HISO2026110500014' WHERE id = 7; -- TEST0006, khớp hoàn toàn
UPDATE imaging_order SET his_order_id = 'HISO2026110500013' WHERE id = 8; -- TEST0007, lệch giới tính (RC-12)

-- order 2 (TEST0002) đã có identity_conflict từ V7__seed_demo_data.sql (field_name date_of_birth,
-- ris_value 1958, his_value 1952) - KHÔNG tạo thêm, chỉ nối his_order_id vào đúng ca đó ở trên.
INSERT INTO identity_conflict (patient_id, order_id, source, field_name, ris_value, his_value, detected_at, resolution) VALUES
    (3, 3, 'HIS', 'full_name', 'Trần Văn Giả C', 'Trần Văn Giả X', '2026-11-05 03:10:00', 'PENDING'),
    (7, 8, 'HIS', 'gender', 'M', 'F', '2026-11-05 03:15:00', 'PENDING');

-- ---------------------------------------------------------------- 4. Một ca CANCELLED (T10) - xem cảnh báo ở đầu file
-- Study thứ hai của order 8 (order 8 đã có study 9 SCHEDULED từ V7) - IN-02/02-erd.md mục 9 cho phép
-- một imaging_order có nhiều study. Hủy khi còn SCHEDULED và chưa có study_instance_uid (đúng điều kiện T10).
INSERT INTO study (id, accession_number, order_id, service_item_id, device_id, status, study_instance_uid, scheduled_at, started_at, completed_at, performed_by_user_id, reading_user_id, approving_user_id) VALUES
    (10, '2026110500001', 8, 2, NULL, 'CANCELLED', NULL, '2026-11-05 02:00:00', NULL, NULL, NULL, NULL, NULL);

INSERT INTO study_status_history (study_id, from_status, to_status, changed_by_user_id, changed_at, reason) VALUES
    (10, NULL,        'SCHEDULED', 3, '2026-11-05 01:50:00', NULL),
    (10, 'SCHEDULED', 'CANCELLED', 3, '2026-11-05 02:05:00', 'Bệnh nhân đã xuất viện, không còn nhu cầu chụp (dữ liệu giả, minh họa T10)');
