# VR-RIS PACS — Frontend

Giao diện RIS. Stack: **React 19 · TypeScript · Vite 7 · Tailwind CSS 4**, không dùng thư viện UI ngoài —
mọi control (nút, input, select, modal, popover, tag, icon) đều tự viết trong `src/shared/ui`.

## Chạy

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # ra dist/, dùng cho image nginx ở deploy/docker/frontend.Dockerfile
```

Yêu cầu Node.js 20+.

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `VITE_API_MODE` | `mock` | `mock` chạy mock server trong trình duyệt; `live` gọi backend thật |
| `VITE_API_BASE_URL` | `/api/v1` | Đường dẫn tương đối **cùng origin** (`doc/14` mục 3.2) — nginx của `ris-frontend` proxy `/api/` sang backend nên không cần CORS |
| `VITE_DEV_API_TARGET` | `http://localhost:8080` | Khi `npm run dev`, proxy của Vite thay vai trò nginx |
| `VITE_PERSIST_SESSION` | `false` | `true` thì access token sống qua lần tải lại trang — **nợ kỹ thuật**, xem `src/shared/api/session.ts` |
| `VITE_OHIF_BASE_URL` | — | Base URL của OHIF để mở deep-link (IN-23, chưa dùng) |

Gọi backend thật:

```bash
VITE_API_MODE=live npm run dev
```

## Đăng nhập

Mock server dùng đúng 7 tài khoản seed của `database/migrations/V7` + `V8`, mật khẩu chung `123456`:
`admin`, `bslamsang`, `ktv`, `bscdha`, `bscdha2`, `bsduyet`, `bsduyet2`.

Chưa đăng nhập mà vào `/worklist` thì bị chuyển về `/login` và quay lại đúng chỗ sau khi đăng nhập.
Access token giữ trong **bộ nhớ** (không phải localStorage) theo đề xuất ở `doc/09` mục 1.2, nên tải lại
trang là mất phiên — đổi hành vi này bằng `VITE_PERSIST_SESSION`, nhưng phải ghi nợ kỹ thuật.

Xử lý lỗi theo `doc/06` mục 8: **401** → dọn phiên, về trang login; **403** → hiện dải cảnh báo
"không đủ quyền", **không** đăng xuất.

## Cấu trúc

Theo `doc/15-kien-truc-backend.md` mục 7: component **không** gọi thẳng `fetch`, chỉ đi qua hook →
tầng `api` của feature → `shared/api`.

```
src/
  app/App.tsx                  router + AuthProvider + RequireAuth
  shared/
    ui/                        Icon, controls (Button/Input/Select/Modal/Popover/Tag/Textarea), StatusChip
    lib/                       types (mã ERD), status, stateMachine, auth (RBAC), format
    api/
      http.ts                  HttpClient trên fetch + ApiError (401/403)
      dto.ts                   DTO snake_case đúng openapi.yaml
      mappers.ts               DTO → kiểu miền camelCase
      session.ts               nơi giữ access token
      client.ts                chọn mock hay live
      mock/{seed,mockClient}   mock server trong trình duyệt, chép đúng seed V7+V8
    hooks/useAsync.ts          hook gọi API dùng chung
  features/
    auth/                      api, useAuth, RequireAuth, LoginPage
    worklist/                  api, hooks, Sidebar/TopBar/WorklistTable/PatientPanel/RelatedOrders/Pagination, WorklistPage
    clinical/                  api + ClinicalModal (5 tab)
    reports/                   api, useStudyReport, ReportModal
    studies/                   AddStudyModal, UpdateStudyModal, UpdateFromHisModal
    printing/                  api, printSheet, PrintModal
```

## Những chỗ bám tài liệu

- **Mã trạng thái** dùng mã DB/API (`SCHEDULED/UNREAD/READING/READ/APPROVING/APPROVED`),
  nhãn tiếng Việt nằm trong `shared/lib/status.ts`. `CANCELLED` **chưa có** — còn ❓, khớp
  `enu.StudyStatus` của backend (nợ kỹ thuật, `doc/12` TC-16).
- **Tuổi** lấy `age` + `age_approximate` từ API: `~68` khi ngày sinh chỉ có năm/tháng, `—` khi
  không có ngày sinh. Không bao giờ ra `0` (`01-scope.md` mục 6 bước 10, TC-09).
- **Chuyển trạng thái** sinh từ `shared/lib/stateMachine.ts` (T2–T9 theo `doc/03`), lọc tiếp theo
  vai trò. Backend vẫn kiểm lại — 403/409 hiện ngay trong modal báo cáo.
- **Kỹ thuật và kết luận** lấy từ báo cáo hiện hành, không lấy từ `study` (`doc/02-erd.md` mục 10).
- **Lọc theo thời gian**: chỉ hai ô chọn ngày, đúng tham số `date_from`/`date_to` của `GET /studies`.
  Năm nút khoảng thời gian nhanh đã bỏ theo yêu cầu của nhóm.
- **Bộ lọc đã [HOÃN]** (đồng bộ HIS, BHYT, ca quá hạn) không dựng, vì backend không có tham số
  tương ứng — dựng ra sẽ là nút bấm không làm gì.

## In phiếu kết quả

Nút **In** ở panel bệnh nhân mở hộp thoại chọn nội dung. Mỗi mục được chọn in ra **một tờ riêng**.

- **In / Lưu PDF**: dựng HTML rồi gọi hộp thoại in của trình duyệt (in giấy hoặc Save as PDF).
  Chạy được cả ở chế độ mock. Trước khi mở hộp thoại có gọi `POST /patients/{id}/print-events`
  để ghi `audit_log` action `PRINT`.
- **Tải PDF từ máy chủ**: gọi `GET /patients/{id}/print-documents` cho từng mục, backend dựng PDF.
  Chỉ bật ở chế độ `live`.

Dữ liệu: chỉ **Kết quả chẩn đoán chức năng chính** là dữ liệu thật của RIS; tổng quan khám và
xét nghiệm là dữ liệu giả trong `clinical_snapshot`; đơn thuốc và phẫu thuật thủ thuật còn rỗng —
hộp thoại ghi rõ từng mục lấy dữ liệu từ đâu.

⚠️ In ấn phiếu lẻ vẫn là chức năng phụ `H-05` [HOÃN] ở `01-scope.md` mục 4. Muốn đưa hẳn vào IN thì phải
ghi bảng thay đổi phạm vi (mục 9) và chốt quyền `report.print` ở `doc/06` mục 3.

## Tải danh sách & In danh sách (`IN-25` ❓)

Các nút **"Tải danh sách"** và **"In danh sách"** trên thanh công cụ worklist (cùng **"Tải toàn bộ danh sách"** / **"In toàn bộ danh sách"** trong menu Cấu hình hệ thống):

- **Tải danh sách**: gọi `GET /studies/export` (hoặc `GET /patients/export`), trả về file CSV (`UTF-8` có BOM `\uFEFF` để mở trực tiếp bằng Excel giữ nguyên tiếng Việt, chống CSV Formula Injection). Backend tự động ghi `audit_log` với `action = 'EXPORT'` (kèm `patient_id` nếu danh sách xuất chỉ thuộc 1 bệnh nhân, `NULL` nếu nhiều bệnh nhân theo `doc/09` mục 2.2).
- **In danh sách**: gọi `POST /patients/export-events` để ghi `audit_log` với `action = 'EXPORT'`, sau đó dựng bảng HTML danh sách ca chụp và mở hộp thoại in của trình duyệt.
- Hoạt động ở cả hai chế độ `mock` và `live`, chỉ dùng dữ liệu giả (`R-03`), cột Tuổi tuân thủ `AgeCalculator` (`~68` khi ngày sinh chỉ biết năm/tháng, `—` khi không có ngày sinh, không bao giờ là `0`).

## Chưa làm

- Nút **"Xem ảnh"** mở OHIF (IN-23) — kế hoạch bố cục ở `doc/16-fe-viewer-entry-points.md`,
  chờ backend thêm endpoint trả URL viewer để ghi được `VIEW_IMAGE`.
- Màn **audit log** (IN-15) và **quản lý người dùng**.
- Gửi form **Thêm ca chụp** và **Cập nhật ca chụp** lên API (hiện mới có giao diện).
- Sửa `shared/lib/auth.ts` cho khớp `doc/06-rbac-matrix.md` mục 6 (bỏ `ADMIN` khỏi `report.write`
  và `report.approve`, thêm `APPROVER` vào `study.reopen`, thêm `study.cancel`/`study.release`/
  `conflict.resolve`/`audit.view`) — là task riêng, chưa làm ở đợt này.
- Lọc theo **Chỉ định** và **Bộ phận chụp** hiện lọc phía client trên trang đang xem, vì
  `GET /studies` chưa có tham số tương ứng trong `openapi.yaml` v0.1.
