# VR-RIS PACS — Frontend

Giao diện RIS dựng theo bộ ảnh thiết kế trong `../vju_mri_web/img` (đã bỏ khung đen và chữ đánh số ở viền).
Chỉ có phần frontend; backend Spring Boot do nhóm khác làm.

## Chạy

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # ra thư mục dist/ (dùng cho image nginx trong manifest k8s)
```

Yêu cầu: Node.js 20+ (máy đang dùng Node 26 LTS).

## Stack

React 19 · TypeScript · Vite 7 · Tailwind CSS 4. Không dùng thư viện UI ngoài — mọi control
(nút, ô nhập, select, checkbox, popover, modal, tag, icon) đều tự viết theo trang
"DS / Icons", "DS / Controls", "DS / Layout" của bản thiết kế.

## Đăng nhập

Mật khẩu chung cho mọi tài khoản demo: `123456`.

| Tài khoản | Vai trò | Thấy được gì |
|---|---|---|
| `admin` | Quản trị hệ thống | Toàn bộ chức năng |
| `bslamsang` | Bác sĩ lâm sàng | Thêm/sửa bệnh nhân, đồng bộ HIS, lâm sàng |
| `ktv` | Kỹ thuật viên | Như trên + xác nhận đã nhận đủ ảnh |
| `bscdha` | Bác sĩ CĐHA | Mở ca, nhập và chốt báo cáo |
| `bsduyet` | Bác sĩ duyệt | Duyệt, trả lại để sửa |

## Cấu trúc

```
src/
  index.css                     design token: màu thương hiệu, màu 6 trạng thái, nền, viền
  lib/
    types.ts                    kiểu dữ liệu bám đúng ERD (diagrams/02-erd.mmd)
    status.ts                   6 trạng thái ca chụp + màu + hình chấm
    stateMachine.ts             các bước chuyển hợp lệ + quyền cần có cho từng bước
    auth.ts                     5 vai trò, bảng quyền, nhãn "cần vai trò gì"
    useWorklistStore.ts         state ca chụp, báo cáo, lịch sử trạng thái trong phiên
    format.ts                   ngày/giờ, ngày sinh theo dob_precision, tính tuổi, giới tính
  data/mock.ts                  dữ liệu giả + tài khoản demo + snapshot lâm sàng 5 tab
  pages/LoginPage.tsx           màn đăng nhập
  components/
    ui/                         Icon, controls (Button/Input/Select/Checkbox/Modal/Popover/Tag), StatusChip
    layout/TopBar.tsx           thanh trên + popover "Cấu hình hệ thống"  (ảnh 09)
    layout/Sidebar.tsx          cây máy, lọc trạng thái, các select lọc   (ảnh 01, 05–07)
    worklist/WorklistTable.tsx  bảng worklist, sắp xếp, lọc theo cột, popover cột hiển thị (ảnh 01–04, 08)
    worklist/PatientPanel.tsx   panel chi tiết bệnh nhân bên phải         (ảnh 02–03)
    worklist/RelatedOrders.tsx  bảng phiếu cùng bệnh nhân phía dưới       (ảnh 02–03)
    worklist/Pagination.tsx     phân trang
    modals/AddStudyModal        Thêm ca chụp                              (ảnh 10)
    modals/UpdateStudyModal     Cập nhật ca chụp — so sánh hiện tại ↔ mới (ảnh 11)
    modals/UpdateFromHisModal   Cập nhật chỉ định từ HIS                  (ảnh 12)
    modals/ClinicalModal        Thông tin lâm sàng 5 tab                  (ảnh 13–17)
    modals/ReportModal          Báo cáo kết quả + lịch sử trạng thái      (IN-10, không có ảnh mẫu)
  pages/WorklistPage.tsx        ghép tất cả: tab Chờ / Đã thực hiện, lọc, chọn dòng, modal
```

## Báo cáo kết quả và state machine

Nút "Báo cáo" ở panel phải mở modal nhập 5 trường theo IN-10: kỹ thuật, mô tả, chẩn đoán,
kết luận, khuyến nghị. Các nút chuyển trạng thái không viết cứng — chúng sinh ra từ
`TRANSITIONS` trong `stateMachine.ts` (dựng theo `05-state-machine.mmd`) rồi lọc tiếp theo
quyền của vai trò đang đăng nhập, nên một ca "Chưa đọc" chỉ hiện "Mở ca để đọc" cho bác sĩ CĐHA,
còn bác sĩ duyệt mở cùng ca đó thì chỉ xem được.

Vòng đời báo cáo: `DRAFT` → `FINAL` (lưu chính thức) → `APPROVED` (duyệt). Nhánh
"trả lại để sửa" đưa báo cáo về `RETURNED`, và lần chốt chính thức sau đó mới tăng `version`.
Ba nhánh ngoại lệ trong sơ đồ — bỏ dở, trả lại, mở lại — hiển thị kiểu cảnh báo và yêu cầu nhập lý do,
lý do được ghi vào dòng lịch sử trạng thái bên phải modal.

## Nối vào backend

Mọi dữ liệu đi qua `WORKLIST` trong `src/data/mock.ts`, kiểu `WorklistRow[]`
(`study` + `order` + `patient` + `service` + `device` + `department`) — đúng các bảng trong ERD.
Khi API sẵn sàng, thay import đó bằng một hàm fetch trả về cùng kiểu; các component không đổi.

Phần đã bám theo tài liệu:

- 6 trạng thái và thứ tự chuyển lấy từ `diagrams/05-state-machine.mmd`.
- Tên cột lấy từ `../README.md` mục 3.1; bộ lọc và 5 tab lâm sàng theo `../doc/01-scope.md` mục 3.2–3.3.
  *(Trước đây ghi là `01-scope.docx` — tài liệu đó đã chuyển sang Markdown ngày 2026-10-31; bản `.docx` trong `doc/archive/` không còn được cập nhật.)*
- Tuổi tính theo `dob_precision`, bệnh nhân chỉ có năm sinh vẫn ra tuổi đúng
  (bước 10 trong kịch bản nghiệm thu tuần 8).
- Dữ liệu bệnh nhân đều là giả (ràng buộc R-03).

## Chưa làm

- Deep-link sang OHIF Viewer (IN-19, tuần 7).
- Audit log (IN-15) — backend ghi, frontend chưa có màn xem.
- Màn quản trị người dùng / gán vai trò: hiện danh sách tài khoản nằm cứng trong `data/mock.ts`.

## Lưu ý khi nối backend

Màn đăng nhập và bảng quyền ở frontend chỉ để giao diện gọn mắt. Scope doc IN-14 yêu cầu
chặn ở tầng API chứ không chỉ ẩn nút, nên backend vẫn phải kiểm tra quyền cho mọi endpoint,
kể cả những thao tác mà giao diện đã khóa sẵn. Tương tự, điều kiện chuyển trạng thái trong
`stateMachine.ts` phải được backend kiểm lại trước khi ghi `study_status_history`.
