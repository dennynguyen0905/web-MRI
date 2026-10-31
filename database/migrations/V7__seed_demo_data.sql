-- V7: DỮ LIỆU GIẢ để dev và demo. KHÔNG phải dữ liệu thật (nguyên tắc bất biến: không dùng dữ liệu bệnh nhân thật).
--   * Bệnh nhân mã TEST0001...; tên có hậu tố "Giả". Số BHXH dạng GIA0000000001 (không tồn tại thật).
--   * Mật khẩu của 5 tài khoản demo đều là: 123456 (chỉ để dev, lưu dạng BCrypt). KHÔNG dùng ngoài môi trường dev/demo.
--   * study_instance_uid là UID giả, không trỏ tới ảnh nào trong Orthanc.
-- Dữ liệu bám đúng state machine ở doc/03-state-machine.md: mỗi ca có đủ dòng study_status_history cho tới trạng thái hiện tại.
-- Thành viên được phép tự thêm/xóa dữ liệu trong DB của mình; schema thì phải giống nhau.

-- ---------------------------------------------------------------- Khoa / nhóm
INSERT INTO department (id, code, name, parent_id, `type`, sort_order) VALUES
    (1, 'CDHA',    'Chẩn đoán hình ảnh', NULL, 'KHOA', 1),
    (2, 'CDHA_XQ', 'X-quang',            1,    'NHOM', 1),
    (3, 'CDHA_CT', 'CT',                 1,    'NHOM', 2),
    (4, 'CDHA_MR', 'MRI',                1,    'NHOM', 3),
    (5, 'CDHA_SA', 'Siêu âm',            1,    'NHOM', 4),
    (6, 'CC',      'Cấp cứu',            NULL, 'KHOA', 2),
    (7, 'NOI',     'Nội tổng hợp',       NULL, 'KHOA', 3),
    (8, 'NGOAI',   'Ngoại tổng hợp',     NULL, 'KHOA', 4);

-- ---------------------------------------------------------------- Máy chụp
INSERT INTO device (id, code, name, modality, department_id) VALUES
    (1, 'CR-01', 'Máy X-quang số hóa 01', 'CR', 2),
    (2, 'CT-01', 'Máy CT 64 dãy 01',      'CT', 3),
    (3, 'MR-01', 'Máy MRI 1.5T 01',       'MR', 4),
    (4, 'US-01', 'Máy siêu âm 01',        'US', 5);

-- ---------------------------------------------------------------- Danh mục dịch vụ
INSERT INTO service_item (id, code, name, modality, body_part) VALUES
    (1, 'XQ-NGUC', 'Chụp X-quang ngực thẳng',                         'CR', 'Ngực'),
    (2, 'XQ-CSTL', 'Chụp X-quang cột sống thắt lưng',                 'CR', 'Cột sống'),
    (3, 'CT-SN',   'Chụp CT sọ não không tiêm thuốc cản quang',       'CT', 'Sọ não'),
    (4, 'CT-NGUC', 'Chụp CT ngực',                                    'CT', 'Ngực'),
    (5, 'MR-SN',   'Chụp MRI sọ não',                                 'MR', 'Sọ não'),
    (6, 'US-BUNG', 'Siêu âm ổ bụng',                                  'US', 'Bụng');

-- ---------------------------------------------------------------- Vai trò và tài khoản demo
INSERT INTO `role` (id, code, name) VALUES
    (1, 'ADMIN',       'Quản trị hệ thống'),
    (2, 'CLINICIAN',   'Bác sĩ lâm sàng'),
    (3, 'TECHNICIAN',  'Kỹ thuật viên'),
    (4, 'RADIOLOGIST', 'Bác sĩ chẩn đoán hình ảnh'),
    (5, 'APPROVER',    'Bác sĩ duyệt');

INSERT INTO app_user (id, username, password_hash, full_name, email, department_id) VALUES
    (1, 'admin',     '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'Quản trị viên (demo)',  'admin@vr-ris.local',     1),
    (2, 'bslamsang', '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'BS. Lâm Sàng (demo)',  'bslamsang@vr-ris.local', 7),
    (3, 'ktv',       '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'KTV. Kỹ Thuật (demo)', 'ktv@vr-ris.local',       1),
    (4, 'bscdha',    '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'BS. CĐHA (demo)',      'bscdha@vr-ris.local',    1),
    (5, 'bsduyet',   '$2a$10$y3edON7opgEvvrt/kieROOPZwA06JuIvax.kWuJOKcHYmGuMgdAKC', 'BS. Duyệt (demo)',     'bsduyet@vr-ris.local',   1);

INSERT INTO user_role (user_id, role_id) VALUES (1, 1), (2, 2), (3, 3), (4, 4), (5, 5);

-- ---------------------------------------------------------------- Bệnh nhân giả
-- Cố ý có đủ các trường hợp tuổi: đủ ngày (DAY), chỉ tháng (MONTH), chỉ năm (YEAR), và không có ngày sinh (NULL).
INSERT INTO patient (id, patient_code, full_name, date_of_birth, dob_precision, gender, social_insurance_no, phone, address, created_by) VALUES
    (1, 'TEST0001', 'Nguyễn Văn Giả A', '1958-03-15', 'DAY',   'M', 'GIA0000000001', '0900000001', 'Địa chỉ giả 1', 'ktv'),
    (2, 'TEST0002', 'Lê Thị Giả B',     '1958-01-01', 'YEAR',  'F', 'GIA0000000002', '0900000002', 'Địa chỉ giả 2', 'ktv'),
    (3, 'TEST0003', 'Trần Văn Giả C',   '1975-06-01', 'MONTH', 'M', 'GIA0000000003', '0900000003', 'Địa chỉ giả 3', 'ktv'),
    (4, 'TEST0004', 'Phạm Thị Giả D',   '1990-11-20', 'DAY',   'F', NULL,            '0900000004', 'Địa chỉ giả 4', 'ktv'),
    (5, 'TEST0005', 'Hoàng Văn Giả E',  NULL,         'DAY',   'M', NULL,            NULL,         NULL,            'ktv'),
    (6, 'TEST0006', 'Vũ Thị Giả F',     '1983-09-09', 'DAY',   'F', 'GIA0000000006', '0900000006', 'Địa chỉ giả 6', 'ktv'),
    (7, 'TEST0007', 'Đặng Văn Giả G',   '2001-02-28', 'DAY',   'M', 'GIA0000000007', '0900000007', 'Địa chỉ giả 7', 'ktv'),
    (8, 'TEST0008', 'Bùi Thị Giả H',    '1949-12-01', 'YEAR',  'F', 'GIA0000000008', '0900000008', 'Địa chỉ giả 8', 'ktv');

-- ---------------------------------------------------------------- Chỉ định
INSERT INTO imaging_order (id, order_code, patient_id, visit_code, source, sync_status, ordering_department_id, ordering_doctor_name, patient_class, room, bed, clinical_diagnosis, ordered_at) VALUES
    (1, 'PH20261029001', 1, 'VV0001', 'MANUAL', 'NOT_SYNCED', 6, 'BS. Chỉ Định A (giả)', 'OUTPATIENT', NULL,   NULL, 'Đau đầu đột ngột (dữ liệu giả)',    '2026-10-29 02:10:00'),
    (2, 'PH20261030001', 2, 'VV0002', 'MANUAL', 'NOT_SYNCED', 7, 'BS. Chỉ Định B (giả)', 'INPATIENT',  'P101', '3',  'Ho kéo dài (dữ liệu giả)',          '2026-10-30 00:40:00'),
    (3, 'PH20261030002', 3, 'VV0003', 'MANUAL', 'NOT_SYNCED', 7, 'BS. Chỉ Định B (giả)', 'INPATIENT',  'P102', '1',  'Chóng mặt (dữ liệu giả)',           '2026-10-30 01:30:00'),
    (4, 'PH20261030003', 4, 'VV0004', 'MANUAL', 'NOT_SYNCED', 8, 'BS. Chỉ Định C (giả)', 'OUTPATIENT', NULL,   NULL, 'Đau bụng (dữ liệu giả)',            '2026-10-30 02:40:00'),
    (5, 'PH20261031001', 8, 'VV0005', 'MANUAL', 'NOT_SYNCED', 6, 'BS. Chỉ Định A (giả)', 'OUTPATIENT', NULL,   NULL, 'Ngã, chấn thương đầu (dữ liệu giả)', '2026-10-31 01:10:00'),
    (6, 'PH20261031002', 5, 'VV0006', 'MANUAL', 'NOT_SYNCED', 8, 'BS. Chỉ Định C (giả)', 'OUTPATIENT', NULL,   NULL, 'Đau lưng (dữ liệu giả)',            '2026-10-31 04:30:00'),
    (7, 'PH20261031003', 6, 'VV0007', 'MANUAL', 'NOT_SYNCED', 7, 'BS. Chỉ Định B (giả)', 'OUTPATIENT', NULL,   NULL, 'Khó thở (dữ liệu giả)',             '2026-10-31 05:30:00'),
    (8, 'PH20261031004', 7, 'VV0008', 'MANUAL', 'NOT_SYNCED', 6, 'BS. Chỉ Định A (giả)', 'OUTPATIENT', NULL,   NULL, 'Kiểm tra sức khỏe (dữ liệu giả)',   '2026-10-31 06:30:00');

-- ---------------------------------------------------------------- Ca chụp (đủ 6 trạng thái chính; không seed CANCELLED vì còn ❓)
INSERT INTO study (id, accession_number, order_id, service_item_id, device_id, status, study_instance_uid, scheduled_at, started_at, completed_at, performed_by_user_id, reading_user_id, approving_user_id) VALUES
    (1, '2026102900001', 1, 3, 2, 'APPROVED',   '1.2.826.0.1.3680043.8.498.10000001', '2026-10-29 02:30:00', '2026-10-29 02:45:00', '2026-10-29 03:00:00', 3, 4, 5),
    (2, '2026102900002', 1, 1, 1, 'READ',       '1.2.826.0.1.3680043.8.498.10000002', '2026-10-29 02:30:00', '2026-10-29 02:50:00', '2026-10-29 02:55:00', 3, 4, NULL),
    (3, '2026103000001', 2, 1, 1, 'APPROVING',  '1.2.826.0.1.3680043.8.498.10000003', '2026-10-30 01:00:00', '2026-10-30 01:10:00', '2026-10-30 01:20:00', 3, 4, 5),
    (4, '2026103000002', 3, 5, 3, 'READING',    '1.2.826.0.1.3680043.8.498.10000004', '2026-10-30 02:00:00', '2026-10-30 02:10:00', '2026-10-30 02:40:00', 3, 4, NULL),
    (5, '2026103000003', 4, 6, 4, 'UNREAD',     '1.2.826.0.1.3680043.8.498.10000005', '2026-10-30 03:00:00', '2026-10-30 03:05:00', '2026-10-30 03:20:00', 3, NULL, NULL),
    (6, '2026103100001', 5, 3, 2, 'UNREAD',     '1.2.826.0.1.3680043.8.498.10000006', '2026-10-31 01:30:00', '2026-10-31 01:35:00', '2026-10-31 01:50:00', 3, NULL, NULL),
    (7, '2026103100002', 6, 2, 1, 'SCHEDULED',  NULL,                                 '2026-10-31 05:00:00', NULL, NULL, NULL, NULL, NULL),
    (8, '2026103100003', 7, 4, 2, 'SCHEDULED',  NULL,                                 '2026-10-31 06:00:00', NULL, NULL, NULL, NULL, NULL),
    (9, '2026103100004', 8, 1, NULL, 'SCHEDULED', NULL,                               '2026-10-31 07:00:00', NULL, NULL, NULL, NULL, NULL);

-- ---------------------------------------------------------------- Lịch sử trạng thái (append-only)
-- Người tạo: 2 = BS lâm sàng, 3 = KTV, 4 = BS CĐHA (đọc), 5 = BS duyệt.
INSERT INTO study_status_history (study_id, from_status, to_status, changed_by_user_id, changed_at) VALUES
    (1, NULL,        'SCHEDULED', 2, '2026-10-29 02:10:00'),
    (1, 'SCHEDULED', 'UNREAD',    3, '2026-10-29 03:00:00'),
    (1, 'UNREAD',    'READING',   4, '2026-10-29 03:30:00'),
    (1, 'READING',   'READ',      4, '2026-10-29 04:00:00'),
    (1, 'READ',      'APPROVING', 5, '2026-10-29 04:20:00'),
    (1, 'APPROVING', 'APPROVED',  5, '2026-10-29 04:40:00'),

    (2, NULL,        'SCHEDULED', 2, '2026-10-29 02:10:00'),
    (2, 'SCHEDULED', 'UNREAD',    3, '2026-10-29 02:55:00'),
    (2, 'UNREAD',    'READING',   4, '2026-10-29 05:00:00'),
    (2, 'READING',   'READ',      4, '2026-10-29 05:20:00'),

    (3, NULL,        'SCHEDULED', 2, '2026-10-30 00:40:00'),
    (3, 'SCHEDULED', 'UNREAD',    3, '2026-10-30 01:20:00'),
    (3, 'UNREAD',    'READING',   4, '2026-10-30 02:00:00'),
    (3, 'READING',   'READ',      4, '2026-10-30 02:30:00'),
    (3, 'READ',      'APPROVING', 5, '2026-10-30 03:00:00'),

    (4, NULL,        'SCHEDULED', 2, '2026-10-30 01:30:00'),
    (4, 'SCHEDULED', 'UNREAD',    3, '2026-10-30 02:40:00'),
    (4, 'UNREAD',    'READING',   4, '2026-10-30 04:00:00'),

    (5, NULL,        'SCHEDULED', 2, '2026-10-30 02:40:00'),
    (5, 'SCHEDULED', 'UNREAD',    3, '2026-10-30 03:20:00'),

    (6, NULL,        'SCHEDULED', 2, '2026-10-31 01:10:00'),
    (6, 'SCHEDULED', 'UNREAD',    3, '2026-10-31 01:50:00'),

    (7, NULL,        'SCHEDULED', 3, '2026-10-31 04:30:00'),
    (8, NULL,        'SCHEDULED', 3, '2026-10-31 05:30:00'),
    (9, NULL,        'SCHEDULED', 3, '2026-10-31 06:30:00');

-- ---------------------------------------------------------------- Báo cáo (nội dung giả)
INSERT INTO report (id, study_id, version, status, origin, technique, findings, diagnosis, conclusion, recommendation, created_by_user_id, created_at, approved_by_user_id, approved_at) VALUES
    (1, 1, 1, 'APPROVED', 'MANUAL', 'CT sọ não không tiêm thuốc cản quang (dữ liệu giả).', 'Không thấy tổn thương khu trú (dữ liệu giả).', 'Chưa phát hiện bất thường.', 'Chưa phát hiện bất thường trên CT sọ não (dữ liệu giả).', 'Theo dõi lâm sàng.', 4, '2026-10-29 03:30:00', 5, '2026-10-29 04:40:00'),
    (2, 2, 1, 'FINAL',    'MANUAL', 'X-quang ngực thẳng (dữ liệu giả).',                    'Hai trường phổi sáng, bóng tim không to (dữ liệu giả).', 'Chưa phát hiện bất thường.', 'X-quang ngực chưa phát hiện bất thường (dữ liệu giả).', NULL, 4, '2026-10-29 05:00:00', NULL, NULL),
    (3, 3, 1, 'FINAL',    'MANUAL', 'X-quang ngực thẳng (dữ liệu giả).',                    'Tăng đậm rốn phổi hai bên (dữ liệu giả).', 'Gợi ý viêm phế quản.', 'Hình ảnh gợi ý viêm phế quản (dữ liệu giả).', 'Đối chiếu lâm sàng.', 4, '2026-10-30 02:00:00', NULL, NULL),
    (4, 4, 1, 'DRAFT',    'MANUAL', 'MRI sọ não các chuỗi xung cơ bản (dữ liệu giả).',      NULL, NULL, NULL, NULL, 4, '2026-10-30 04:00:00', NULL, NULL);

-- ---------------------------------------------------------------- Một ca lệch dữ liệu HIS ↔ RIS đang chờ xử lý
-- Mô phỏng known issue ở README 2.5: năm sinh RIS 1958 nhưng HIS (giả) gửi 1952. Dữ liệu giả hoàn toàn.
INSERT INTO identity_conflict (patient_id, order_id, source, field_name, ris_value, his_value, detected_at, resolution) VALUES
    (2, NULL, 'HIS', 'date_of_birth', '1958', '1952', '2026-10-31 03:00:00', 'PENDING');

-- ---------------------------------------------------------------- Snapshot lâm sàng giả
-- Tab 1, 2 có nội dung; tab 3, 4, 5 để trống (chờ tích hợp HIS) theo doc/01-scope.md mục 3.3.
INSERT INTO clinical_snapshot (order_id, payload, source) VALUES
    (1, JSON_OBJECT(
            'overview', JSON_OBJECT('chan_doan_phieu_kham', 'Đau đầu đột ngột (dữ liệu giả)', 'khoa_kham', 'Cấp cứu', 'nhip_tim', '82', 'huyet_ap', '120/80 mmHg'),
            'labs', JSON_ARRAY(JSON_OBJECT('stt', 1, 'ten', 'Huyết học (giả)', 'trang_thai', 'Đã có kết quả')),
            'functional', JSON_ARRAY(),
            'drugs', JSON_ARRAY(),
            'surgery', JSON_ARRAY()), 'SEED'),
    (2, JSON_OBJECT(
            'overview', JSON_OBJECT('chan_doan_phieu_kham', 'Ho kéo dài (dữ liệu giả)', 'khoa_kham', 'Nội tổng hợp', 'nhip_tim', '76', 'huyet_ap', '130/85 mmHg'),
            'labs', JSON_ARRAY(JSON_OBJECT('stt', 1, 'ten', 'Sinh hóa máu (giả)', 'trang_thai', 'Đã có kết quả')),
            'functional', JSON_ARRAY(),
            'drugs', JSON_ARRAY(),
            'surgery', JSON_ARRAY()), 'SEED');
