# VR-RIS PACS — Đặc tả UI × permission theo component

Tài liệu con của `06-rbac-matrix.md` (không thay thế). Mục đích: dev Frontend tra cứu trực tiếp
component nào dùng permission nào, tab/trạng thái mặc định theo vai trò, trường bị khóa theo vai
trò — không phải đọc lại toàn bộ `06-rbac-matrix.md` mỗi lần code.

| Phiên bản | 0.1 (nháp — chưa được nhóm xác nhận) |
|---|---|
| Ngày | 2026-11-08 |
| Nguồn | `06-rbac-matrix.md` mục 6 (bảng đối chiếu `auth.ts`) · `03-state-machine.md` mục 2.5 · `13-user-stories.md` mục 5 (đối chiếu Frontend) · `Frontend/src/shared/lib/auth.ts`, `stateMachine.ts` sau khi đồng bộ T30 |
| Trạng thái tài liệu | Mọi điểm ❓ là đề xuất, chờ nhóm xác nhận — giống `06-rbac-matrix.md` |

## 1. Bảng quyền 5 vai trò (`auth.ts`, sau khi đồng bộ T30)

| Permission | ADMIN | CLINICIAN | TECHNICIAN | RADIOLOGIST | APPROVER |
|---|---|---|---|---|---|
| `patient.create` | ✔ | ✔ | ✔ | — | — |
| `patient.update` | ✔ | ✔ | ✔ | — | — |
| `order.sync` | ✔ | ✔ | ✔ | — | — |
| `clinical.view` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `report.write` | — | — | — | ✔ | — |
| `report.approve` | — | — | — | — | ✔ |
| `study.markReceived` | ✔ | — | ✔ | — | — |
| `study.reopen` | ✔ | — | — | — | ✔ |
| `study.cancel` **(mới)** | ✔ | — | ✔ | — | — |
| `study.release` **(mới)** | ✔ | — | — | — | — |
| `conflict.resolve` **(mới)** | ✔ | — | ✔ | — | — |
| `audit.view` **(mới)** | ✔ | — | — | — | — |
| `list.print` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `report.print` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `study.viewImage` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `config.system` | ✔ | — | — | — | — |

Đổi so với bản trước (T30): bỏ `report.write`/`report.approve` khỏi `ADMIN` (D15); thêm `APPROVER`
vào `study.reopen` (D16); thêm 4 permission mới `study.cancel`, `study.release`,
`conflict.resolve`, `audit.view` — chưa gắn vào bất kỳ nút nào (xem mục 3).

## 2. Component × permission × trạng thái code

| Component | File | Permission dùng | Tab/trạng thái mặc định theo vai trò | Trường khóa theo vai trò | Trạng thái |
|---|---|---|---|---|---|
| `WorklistPage` | `features/worklist/pages/WorklistPage.tsx` | `list.print` (nút "In/Tải danh sách"); gọi `openGated` với `clinical.view`, `report.print` | **Có (mới, T30):** `RADIOLOGIST` → tab "Đã thực hiện" lọc `UNREAD`; `APPROVER` → tab "Đã thực hiện" lọc `READ` hoặc `APPROVING` (tab có nhiều ca hơn theo `useStatusCounts`, mặc định `READ` nếu chưa tải xong số đếm); `CLINICIAN`/`TECHNICIAN`/`ADMIN` giữ tab "Chờ" (không đổi) | — | Đã code |
| `Sidebar` | `features/worklist/components/Sidebar.tsx` | Không kiểm permission (chỉ là bộ lọc hiển thị theo `SidebarFilters`, `WorklistPage` ghi đè `statuses` khi áp tab mặc định ở trên) | — | — | Đã code |
| `TopBar` | `features/worklist/components/TopBar.tsx` | `patient.create` (nút "Thêm bệnh nhân"), `config.system` (icon mở popover cấu hình, chỉ `ADMIN` mở được) | — | — | Đã code |
| `SystemConfigMenu` (trong `TopBar.tsx`) | `features/worklist/components/TopBar.tsx` | `list.print`, `report.approve` (nút "Duyệt toàn bộ danh sách" — H-05, [HOÃN], nút không có chức năng), `patient.create` | — | — | Giao diện có, nút chưa có chức năng (H-05) |
| `PatientPanel` | `features/worklist/components/PatientPanel.tsx` | `patient.update` ("Cập nhật"), `order.sync` ("Cập nhật từ HIS"), `patient.create` ("Thêm chỉ định"), `clinical.view` ("Lâm sàng"), `report.print` ("In") | — | — | Đã code |
| `AddStudyModal` | `features/studies/components/AddStudyModal.tsx` | Không tự kiểm (mở qua nút đã gated `patient.create` ở `TopBar`/`PatientPanel`) | — | — | **Chỉ giao diện tĩnh** — nút "Tạo mới" chỉ đóng modal, chưa gửi dữ liệu |
| `UpdateStudyModal` | `features/studies/components/UpdateStudyModal.tsx` | Không tự kiểm (mở qua nút đã gated `patient.update` ở `PatientPanel`) | — | **Có (mới, T30):** `CLINICIAN` — khóa (disable, không ẩn) 5 trường định danh `Mã` (`patient_code`), `Họ tên` (`full_name`), `Ngày sinh` (`date_of_birth`), `Giới tính` (`gender`), `BHXH` (`social_insurance_no`), tooltip "Chỉ KTV hoặc Admin được sửa trường này". `TECHNICIAN`/`ADMIN` sửa được tất cả. `Địa chỉ`, `Chẩn đoán ban đầu`, `StudyInstanceUID` không bị khóa thêm (ngoài phạm vi D24) | **Chỉ giao diện tĩnh** cho phần gửi dữ liệu — nút "Lưu" chỉ đóng modal; riêng phần khóa trường theo vai trò đã hoạt động thật (disable `<input>` thật) |
| `UpdateFromHisModal` | `features/studies/components/UpdateFromHisModal.tsx` | Không tự kiểm (mở qua nút đã gated `order.sync` ở `PatientPanel`) | — | — | **Chỉ giao diện tĩnh** — không phải màn "Danh sách lệch dữ liệu" (`conflict.resolve`), chỉ đồng bộ một chỉ định/bệnh nhân theo mã |
| `ClinicalModal` | `features/clinical/components/ClinicalModal.tsx` | `clinical.view` (mở qua `openGated` ở `WorklistPage`); `study.viewImage` (nút "Xem ảnh" tab 3, IN-23 cửa vào b) | — | — | Đã code (tab 1–3 nối dữ liệu thật; tab 4–5 bảng rỗng theo thiết kế) |
| `ReportModal` | `features/reports/components/ReportModal.tsx` | `report.write` (sửa nội dung báo cáo + nút "Lưu nháp"); mỗi nút chuyển trạng thái sinh từ `stateMachine.TRANSITIONS`, lọc theo `can(user, t.permission)`: T3/T4/T5 → `report.write`, T6/T7/T8 → `report.approve`, T9 → `study.reopen` | — | — | Đã code — **chưa kiểm** điều kiện bắt buộc (trường rỗng, khóa mềm, người duyệt ≠ người đọc); backend phải kiểm lại (`03-state-machine.md` mục 2.4) |
| `PrintModal` | `features/printing/components/PrintModal.tsx` | `report.print` (mở qua `openGated` ở `WorklistPage`) | — | — | Đã code |
| `WorklistTable` | `features/worklist/components/WorklistTable.tsx` | `study.viewImage` (icon "Xem ảnh" ở hàng worklist, IN-23 cửa vào a) | — | — | Đã code |

## 3. Permission chưa gắn UI nào (liệt kê, không tự tạo giao diện)

Theo đúng yêu cầu khi đồng bộ (T30): các permission dưới đây đã thêm vào `auth.ts` theo đề xuất
ở `06-rbac-matrix.md` mục 6, nhưng **không có component/nút/màn hình nào trong Frontend hiện tại
dùng tới** — không tự tạo giao diện mới ngoài phạm vi được giao.

| Permission | Vai trò | Màn hình còn thiếu | Story liên quan (`13-user-stories.md`) |
|---|---|---|---|
| `study.cancel` | `ADMIN`, `TECHNICIAN` | Nút "Hủy ca" (T10, `SCHEDULED → CANCELLED`) — chưa có ở `PatientPanel`/`ReportModal`; `CANCELLED` cũng **chưa có** trong `StudyStatus` Frontend (`shared/lib/types.ts`, còn ❓ chờ backend chốt `enu.StudyStatus`) nên `stateMachine.ts` chưa thể thêm `T10` | US-KTV-08 |
| `study.release` | `ADMIN` | Nút "Giải phóng khóa mềm" khi `ADMIN` muốn thực hiện T4 thay người đang đọc — Frontend hiện **không có khóa mềm** (`reading_user_id`/`approving_user_id`), nút T4 trong `ReportModal` hiện dùng chung `report.write` (chỉ `RADIOLOGIST`), không phân biệt được trường hợp `ADMIN` giải phóng khóa | US-ADMIN-06 |
| `conflict.resolve` | `ADMIN`, `TECHNICIAN` | Màn "Danh sách lệch dữ liệu" (`identity_conflict`, ba lựa chọn `KEEP_RIS`/`TAKE_HIS`/`MANUAL_EDIT`) — `UpdateFromHisModal` hiện có **không phải** màn này | US-KTV-07, US-ADMIN-02, US-ADMIN-03 |
| `audit.view` | `ADMIN` | Màn xem `audit_log` theo bệnh nhân/ca — chưa có route, chưa có component | US-ADMIN-04 |

## 4. Câu hỏi còn mở (❓ chưa chốt, không tự quyết)

1. Mã lỗi/cơ chế từ chối khi `CLINICIAN` sửa trường định danh ở `UpdateStudyModal` — Frontend đã
   chọn **disable kèm tooltip** (giữ nguyên giá trị hiển thị, không ẩn), nhưng backend trả `403`
   hay chỉ bỏ qua trường khi `CLINICIAN` cố gọi thẳng API vẫn **chưa chốt** (`06-rbac-matrix.md`
   mục 5, `13-user-stories.md` mục 7 câu 2).
2. Tab mặc định của `APPROVER` khi `useStatusCounts` chưa tải xong (lần đầu vào worklist, mạng
   chậm): hiện tại giữ tab "Chờ" cho tới khi có số đếm rồi mới đổi sang `READ`/`APPROVING` — có
   thể gây giật tab ngay sau khi đăng nhập. Chưa hỏi ý kiến nhóm UX, tạm chấp nhận vì tải worklist
   vốn đã thường dưới 1 giây ở môi trường mock/dev.
3. `study.release` qua T4: chưa rõ nên tách một permission riêng cho "Admin giải phóng khóa" hay
   dùng chung `report.write`/`study.release` một khi khóa mềm được cài đặt thật — để ngỏ tới khi
   khóa mềm (D20) được code.
