# VR-RIS PACS — Kế hoạch giao diện mở viewer (nút "Xem ảnh") và vị trí AI Panel

| Phiên bản | 0.1 (nháp — chưa được nhóm xác nhận) |
|---|---|
| Ngày | 2026-11-06 |
| Thuộc | IN-23 (nút "Xem ảnh", tuần 6–7), tham chiếu IN-19 (Orthanc + OHIF, tuần 7) |
| Nguồn ràng buộc | `07-pacs-dicom.md` mục 6 · `README` mục 2.7 · `01-scope.md` IN-19/IN-23 · `02-erd.md` mục 4.4, 4.11 · `06-rbac-matrix.md` |
| Trạng thái | 🟡 Kế hoạch, **chưa code**. Task chỉ yêu cầu bố cục + đối chiếu, không yêu cầu cài đặt |

## 0. Phạm vi đã điều chỉnh

Tiêu đề gốc của task là "thiết kế giao diện phần đọc file MRI, giao diện chạy AI". Ba chỗ lệch tài liệu, đã chỉnh:

1. **RIS không hiển thị hay xử lý ảnh DICOM** (nguyên tắc bất biến, R-02). Không có viewer thứ hai trong RIS. Ảnh xem trong OHIF qua deep-link bằng `study_instance_uid`.
2. **Modality demo mặc định là CR + CT**, MR chưa được chọn (`01-scope.md` mục 8 câu 9, ❓ chờ nhóm). Tài liệu này không nói riêng về MRI; muốn demo MR thì chốt ở câu 9.
3. **AI chỉ ở mức thiết kế** (câu 19, H-04 hoãn). Không có giao diện "chạy AI" trong RIS. AI Panel là deliverable Figma của KAN-19 (IN-21) — xem mục 5, không làm lại ở đây.

Còn lại trong phạm vi: **bố cục hai cửa vào viewer trong RIS**.

## 1. Hai cửa vào (theo `07-pacs-dicom.md` mục 6.2)

| Cửa vào | Vị trí | Ai dùng | Màn hình hiện có |
|---|---|---|---|
| (a) | **Hàng worklist** | Bác sĩ CĐHA, bác sĩ duyệt | `features/worklist/components/WorklistTable.tsx` |
| (b) | **Tab 3 modal lâm sàng**, theo từng dịch vụ CĐHA | Bác sĩ lâm sàng | `features/clinical/components/ClinicalModal.tsx` |

Cả hai sinh **cùng một dạng URL** và mở **cùng một OHIF**.

## 2. Bố cục đề xuất

### 2.1. Cửa vào (a) — hàng worklist

Thêm **một cột cố định bên phải cùng**, rộng 56px, tiêu đề để trống, không nằm trong popover "Cột hiển thị" (không tắt được — đây là thao tác chính của bác sĩ đọc phim).

```
… │ Kết luận         │ [👁 Xem ảnh] │
… │ Chưa phát hiện…  │   (icon)     │
```

- Nút dạng **icon-only** (`Eye`, 14px) để không đẩy bảng rộng thêm; `title` là "Xem ảnh trong OHIF".
- Click **không** chọn dòng (gọi `stopPropagation`) — mở viewer ở tab mới.
- Giữ nguyên double-click hiện tại (mở modal lâm sàng). Popover "Cấu hình hệ thống" có dòng `Double Click: Viewer / Chi tiết` — khi nhóm nối cấu hình đó vào hành vi thật thì double-click mới đổi thành mở viewer.

### 2.2. Cửa vào (b) — tab 3 modal lâm sàng

Tab **"KẾT QUẢ CHẨN ĐOÁN CHỨC NĂNG CHÍNH"** liệt kê từng dịch vụ CĐHA. Mỗi dòng dịch vụ có ảnh thì thêm nút `[👁 Xem ảnh]` ở cuối dòng, cùng kiểu `Button variant="outline"` như các nút khác trong panel.

### 2.3. Trạng thái nút

| Tình huống | Hiển thị |
|---|---|
| `study_instance_uid` có giá trị | Nút bật |
| `study_instance_uid` là `NULL` | ❓ **ẩn hay disable — chưa chốt** (P-10, `07-pacs-dicom.md` mục 6.2) |
| Vai trò không có quyền xem ảnh | Nút khóa, tooltip ghi rõ cần vai trò nào (giống cách các nút khác trong panel đang làm) |

**Đề xuất cho P-10: disable kèm tooltip "Ca chưa có ảnh trong PACS"**, không ẩn. Lý do: ẩn làm cột nhảy chỗ giữa các hàng và người dùng không biết nút có tồn tại hay không; disable giữ bố cục ổn định và nói rõ vì sao chưa bấm được. Cần nhóm xác nhận — đây là việc Figma ở README mục 6E.

Kịch bản kiểm thử tương ứng: **TC-15** (`doc/12` mục 3.1) — `study.id ∈ {7, 8, 9}` đang `SCHEDULED`, `study_instance_uid IS NULL`.

## 3. Luồng khi bấm nút

```
Người dùng bấm "Xem ảnh"
  → FE gọi API RIS để lấy URL viewer (xem 3.1)
  → BE kiểm vai trò → ghi audit_log VIEW_IMAGE → trả URL
  → FE mở URL ở tab mới (window.open, noopener)
```

### 3.1. Endpoint còn thiếu — việc của backend

`doc/api/openapi.yaml` **chưa có** endpoint nào cho việc này. Hai hướng, cần nhóm chọn:

| Hướng | Mô tả | Ưu | Nhược |
|---|---|---|---|
| **A — FE tự ghép URL** | FE lấy `study_instance_uid` từ worklist, ghép với `VITE_OHIF_BASE_URL` | Không cần endpoint mới | **Không ghi được `audit_log` `VIEW_IMAGE`** → vi phạm nguyên tắc bất biến |
| **B — BE trả URL** (đề xuất) | `GET /studies/{id}/viewer-link` → `{ url }`, BE ghi audit trước khi trả | Đúng nguyên tắc; sau này gắn token ngắn hạn (mục 6.3 của `doc/07`) không phải sửa FE | Thêm một endpoint |

**Chọn B.** Hướng A chỉ nên dùng nếu nhóm chấp nhận ghi nợ kỹ thuật phần audit, và phải ghi vào `01-scope.md` mục 9.

### 3.2. Mâu thuẫn tài liệu cần sửa

`doc/13` mục 8 ghi: `GET /studies/{id}` đang khai `x-audit: READ`, trong khi mở viewer phải là `VIEW_IMAGE`. Hai thứ này là **hai hành vi khác nhau** — xem thông tin ca chụp ≠ xem ảnh bệnh nhân. Hướng B ở trên tự giải quyết: `VIEW_IMAGE` gắn vào endpoint viewer-link riêng, `READ` giữ nguyên ở `GET /studies/{id}`.

`enu.AuditAction` của backend **đã có** `VIEW_IMAGE`; `openapi.yaml` trước đây thiếu — đã bổ sung cùng đợt này.

## 4. Những gì frontend đã sẵn sàng

| Việc | Trạng thái |
|---|---|
| `study_instance_uid` có trong kiểu miền (`shared/lib/types.ts`) và trả về từ worklist | ✅ |
| Biến môi trường `VITE_OHIF_BASE_URL` đã khai trong `src/vite-env.d.ts` | ✅ |
| Icon `Eye` đã có trong `shared/ui/Icon.tsx` | ✅ |
| Cơ chế khóa nút theo vai trò kèm tooltip (`can` / `deniedReason`) | ✅ |
| Cột "Xem ảnh", nút trong tab 3, endpoint viewer-link | ⬜ Chưa làm — tuần 6–7 |

Ước lượng khi có endpoint: khoảng **nửa ngày** cho cả hai cửa vào, vì chỉ là thêm một cột và một nút trên nền đã có.

## 5. AI Panel — không thuộc tài liệu này

Wireframe AI Panel trong OHIF là **KAN-19 / IN-21**, deliverable Figma, mô tả ở `doc/11-ai-design.md` mục 5. Ranh giới cần nhớ khi vẽ:

- AI Panel nằm **trong OHIF**, không phải trong RIS. RIS không có màn hình nào của AI.
- AI **không** tự trả kết quả chính thức — luôn qua bác sĩ CĐHA xác nhận (human-in-the-loop, nguyên tắc bất biến).
- Trong 8 tuần **không có code AI** (H-04).

Đối chiếu với KAN-19: tài liệu này chỉ mô tả **cửa vào** viewer phía RIS; mọi thứ bên trong viewer (overlay mask, panel kết quả, report nháp, nút xác nhận) thuộc KAN-19.

## 6. Câu hỏi còn mở

| Mã | Câu hỏi | Chặn việc gì |
|---|---|---|
| P-10 | Ca chưa có `study_instance_uid`: ẩn hay disable nút? Đề xuất **disable** (mục 2.3) | Vẽ Figma, code IN-23 |
| — | Chọn hướng A hay B ở mục 3.1? Đề xuất **B** | Backend phải thêm endpoint trước khi FE code |
| Câu 9 | Modality demo có thêm MR không? | Bộ DICOM mẫu tuần 4–5 |
| — | Cách chặn truy cập OHIF (3 hướng ở `doc/07` mục 6.3) | Tuần 7, không chặn IN-23 |
