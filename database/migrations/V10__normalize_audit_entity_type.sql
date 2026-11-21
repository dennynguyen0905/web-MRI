-- V10: chuẩn hóa audit_log.entity_type về đúng tên hằng của enum (2026-11-21).
--
-- Từ 2026-11-21, model.AuditLog map entity_type/action sang enum Java (enu.AuditEntityType /
-- enu.AuditAction, @Enumerated(EnumType.STRING)). Hibernate so khớp tên enum PHÂN BIỆT hoa/thường,
-- nên dòng cũ có giá trị viết thường sẽ làm lỗi khi đọc (GET /audit-logs trả 500).
-- Trước bản sửa 2026-11-21, PrintServiceImpl ghi entity_type = 'study' (viết thường) cho sự kiện in
-- phiếu - đây là nguồn duy nhất đã biết của giá trị lệch. Chỉ đổi chữ hoa/thường, không đổi nghĩa.
--
-- Cột dùng collation utf8mb4_0900_ai_ci (không phân biệt hoa/thường) nên phải so sánh bằng BINARY.

UPDATE audit_log
SET entity_type = UPPER(entity_type)
WHERE BINARY entity_type <> BINARY UPPER(entity_type);

UPDATE audit_log
SET action = UPPER(action)
WHERE BINARY action <> BINARY UPPER(action);
