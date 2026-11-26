# Checklist UAT — kiểm thủ công kiểu người dùng

In tờ này ra (hoặc mở song song) và tick tay. Đây là phần **test tự động không thay thế được**:
bắt lỗi nghiệp vụ mà chưa ai nghĩ tới.

| | |
|---|---|
| Nguồn | `doc/12-seed-data-va-kich-ban-demo.md` mục 4 (kịch bản 10 bước) + mục 3.1 (22 TC) |
| Thời lượng | ~20 phút cho 10 bước chính |
| Người chạy | ______________ |
| Ngày chạy | ______________ |
| Phiên bản | nhánh ______ @ commit ______ |

## Chuẩn bị trước khi bắt đầu

- [ ] Chọn chế độ: `mock` (chỉ frontend, `npm run dev` trong `Frontend/`) **hoặc** `live` (cần
      backend + MySQL + Docker)
- [ ] Nếu `live`: `kubectl apply -k deploy/k8s` **trước ít nhất 10 phút** (lần đầu phải kéo image
      Orthanc/OHIF)
- [ ] Nếu `live`: đã đóng tiến trình chiếm cổng `8080` / `4242` / `8042` / `8043` / `3307`
- [ ] Biết trước: **dữ liệu seed nằm ở 29–31/10/2026 và 05/11/2026** → bộ lọc "Hôm nay"/"Hôm qua"
      sẽ rỗng. Đây là rủi ro `D12-02` đã biết, **không phải lỗi**.

Lệnh reset để chạy lại từ đầu (chế độ `live`):
```bash
kubectl delete -k deploy/k8s     # xóa cả PVC, mất ca vừa tạo — chủ ý
kubectl apply -k deploy/k8s      # dựng lại từ seed V1-V8 sạch
```

## Tài khoản (mật khẩu chung `123456`)

| Tài khoản | Vai trò |
|---|---|
| `admin` | ADMIN |
| `bslamsang` | CLINICIAN |
| `ktv` | TECHNICIAN |
| `bscdha` / `bscdha2` | RADIOLOGIST |
| `bsduyet` / `bsduyet2` | APPROVER |

---

## Phần A — Kịch bản 10 bước (`doc/12` mục 4)

### Bước 1 — Triển khai *(chỉ chế độ `live`)*
- [ ] Mọi pod `Running`/`Ready`
- [ ] Mở `http://localhost:8080` thấy trang đăng nhập

Ghi chú: ________________________________________________

### Bước 2 — `ktv` tạo bệnh nhân + chỉ định + ca chụp · **TC-02**
- [ ] Đăng nhập `ktv` thành công
- [ ] Bấm nút "Thêm bệnh nhân" trên thanh trên, modal **THÊM CA CHỤP** mở ra
- [ ] Điền: bệnh nhân mới, dịch vụ `CT-SN`, máy `CT-01`
- [ ] Bấm "Tạo mới" → ca mới xuất hiện ở tab **Chờ**, trạng thái "Chờ thực hiện"
- [ ] Ca có **mã phiếu tự sinh** (không phải ô trống)
- [ ] ⚠️ **Thử lỗi `L1`:** nhập ngày sinh `2030-01-01` → form có chặn không? (hiện tại **KHÔNG**)

Trạng thái sau: `SCHEDULED` · Ghi chú: ______________________

### Bước 3 — Đẩy ảnh DICOM + chuyển "Chưa đọc" (T2) · **TC-03, TC-15**
> 🚫 **Bị chặn bởi `D12-01`** — bộ ảnh DICOM mẫu chưa có (`doc/12` mục 3.3). Bỏ qua và ghi lý do.
- [ ] *(nếu có ảnh)* `storescu -c VRRIS_PACS@localhost:4242 --bind SIM_CT01 <thư-mục-study>` không lỗi
- [ ] Nhập tay `study_instance_uid` vào ca
- [ ] Ca chuyển tab **Đã thực hiện**, trạng thái "Chưa đọc"
- [ ] Nút **"Xem ảnh chụp"** xuất hiện và bấm được

Trạng thái sau: `UNREAD` · Bỏ qua vì: __________________

### Bước 4 — `bscdha` mở ca để đọc (T3) · **TC-04**
- [ ] Đăng nhập `bscdha` → vào **sẵn** tab "Đã thực hiện" (đúng yêu cầu T30)
- [ ] Chọn ca → panel bên phải hiện nút "Mở ca để đọc"
- [ ] Bấm → trạng thái thành "Đang đọc"
- [ ] *(nếu có ảnh)* Bấm "Xem ảnh chụp" → OHIF mở **đúng** study

Trạng thái sau: `READING` · Ghi chú: ______________________

### Bước 5 — `bscdha` nhập và chốt báo cáo (T5) · **TC-05**
- [ ] Mở mục Báo cáo, nhập **Kỹ thuật** và **Kết luận**
- [ ] Thử bấm "Chốt đọc" khi **chưa** nhập đủ → nút bị khóa, tooltip giải thích
- [ ] Nhập đủ → "Chốt đọc" bấm được → trạng thái "Đọc xong"
- [ ] ⚠️ Lưu ý `UX-02`: nút ghi "Chốt đọc" nhưng hộp thoại ghi "Lưu báo cáo chính thức"

Trạng thái sau: `READ` · Ghi chú: ________________________

### Bước 6 — `bsduyet` mở duyệt (T6) rồi duyệt (T7) · **TC-06**
- [ ] Đăng nhập `bsduyet` → vào sẵn tab "Đã thực hiện"
- [ ] "Mở ca để duyệt" → trạng thái "Đang duyệt"
- [ ] "Duyệt" → trạng thái "Duyệt xong"
- [ ] ⚠️ **Thử vượt quyền:** đăng nhập `admin` ở ca "Đang duyệt" → **không** được thấy nút "Duyệt"
      (RBAC-06 / D15)

Trạng thái sau: `APPROVED` · Ghi chú: ____________________

### Bước 7 — `admin` xem audit log · **TC-07**
> ⬜ **Frontend chưa có màn này** (`doc/13` mục 5.2). Phương án thay thế: xem bảng `audit_log`
> trực tiếp bằng DBeaver.
- [ ] Thấy đủ dòng cho các bước 2–6, đúng `actor_username` và thời điểm

Bỏ qua vì: ______________________________________________

### Bước 8 — Xem danh sách lệch dữ liệu HIS↔RIS · **TC-08**
> ⬜ **Frontend chưa có màn này** (`doc/01-scope` mục 11). Thay thế: xem bảng `identity_conflict`
> bằng DBeaver.
- [ ] `TEST0002`: thấy dòng `PENDING`, năm sinh RIS `1958` ≠ HIS `1952`
- [ ] Hệ thống **không** tự động ghi đè

Bỏ qua vì: ______________________________________________

### Bước 9 — Kiểm cột Tuổi · **TC-09** ⭐ quan trọng
- [ ] Mở tab **Chờ** (cột "Tuổi" chỉ có ở tab này)
- [ ] `TEST0001` (đủ ngày sinh) → tuổi **chính xác**, không có dấu `~`
- [ ] `TEST0003` (chỉ tháng) → tuổi **xấp xỉ**, có dấu `~`
- [ ] `TEST0002`, `TEST0008` (chỉ năm) → tuổi **xấp xỉ**, có dấu `~`
- [ ] `TEST0005` (không có ngày sinh) → hiển thị `—`
- [ ] **KHÔNG ô nào hiển thị "0"**
- [ ] Trỏ chuột vào tuổi xấp xỉ → tooltip "Tuổi xấp xỉ vì ngày sinh chỉ có năm hoặc tháng"

Ghi chú: ________________________________________________

### Bước 10 — `ktv` hủy ca chưa có ảnh (T10) · **TC-12**
- [ ] Chọn ca `SCHEDULED` chưa có ảnh → nút "Hủy ca" bấm được
- [ ] Bấm → hộp thoại "Hủy ca" mở ra, có ô **Lý do \***
- [ ] Để trống lý do → nút xác nhận **bị khóa**
- [ ] ⚠️ **Thử `UX-01`:** để trống lý do — có **thông báo lỗi dạng chữ** nào trên màn hình không?
      (hiện tại **KHÔNG**, chỉ có tooltip khi trỏ chuột)
- [ ] Nhập chỉ **khoảng trắng** → vẫn bị khóa (đúng)
- [ ] Nhập lý do thật → xác nhận được → trạng thái "Đã hủy"
- [ ] Ca đã hủy **vẫn tra cứu được** ở tab "Đã thực hiện" (không biến mất)

Trạng thái sau: `CANCELLED` · Ghi chú: __________________

---

## Phần B — Ma trận quyền (bù cho phần tự động)

Phần này **đã được 36 test tự động phủ** (`e2e/08-rbac-ui.spec.ts`) và kết quả **sạch**. Chỉ cần
kiểm tay khi đổi bảng quyền trong `auth.ts`.

- [ ] `bslamsang` (CLINICIAN): ở **cả 7 trạng thái**, panel **không** có nút chuyển trạng thái nào
- [ ] `admin`: ở ca "Đang đọc" thấy "Giải phóng khóa", **không** thấy "Bỏ dở"/"Chốt đọc"
- [ ] `bscdha` (RADIOLOGIST): **không** thấy "Duyệt" ở ca "Đang duyệt"
- [ ] `bscdha`/`bsduyet`: nút "Thêm bệnh nhân" bị **khóa** kèm tooltip "Cần vai trò: …"
- [ ] `bscdha2` mở ca mà `bscdha` đang đọc → bị từ chối (khóa mềm, RBAC-09)
- [ ] Tài khoản vừa đọc vừa duyệt → **không** tự duyệt ca mình đọc (RBAC-10)

---

## Phần C — Lọc và tìm kiếm *(chưa có test tự động, cần kiểm tay)*

- [ ] **TC-17** Lọc Modality: `CT` / `MR` / `CR` / `US` → mỗi loại ra đúng số ca
- [ ] **TC-19** Lọc loại bệnh nhân: "Nội trú" / "Ngoại trú" ra đúng
- [ ] **TC-18** Lọc khoảng thời gian: chọn khoảng **29/10–05/11/2026** → có dữ liệu
- [ ] **TC-18** Chọn "Hôm nay" → **rỗng là ĐÚNG** (rủi ro `D12-02`, không phải lỗi)
- [ ] Tìm theo mã bệnh nhân / họ tên trên thanh trên → ra đúng
- [ ] Đổi bộ lọc → **số trang tự về trang 1** (không mắc ở trang 5 của kết quả cũ)
- [ ] **TC-16** Tick từng trạng thái → danh sách khớp; "Đã hủy" ở mock sẽ **rỗng** (lỗi `DATA-01`)
- [ ] Bật/tắt cột qua "Cột hiển thị" → cột xuất hiện/mất đúng
- [ ] Bấm "Xóa bộ lọc" → về mặc định

---

## Phần D — Nghịch lý dữ liệu *(nơi dễ ra lỗi mới nhất)*

Đây là phần đáng dành thời gian nhất — thử những thứ không ai nghĩ tới:

- [ ] Ngày sinh **tương lai** (`2030-01-01`) → lỗi `L1`, đã biết
- [ ] Ngày sinh **rất xa** (`1900-01-01`) → tuổi hiển thị hợp lý?
- [ ] Họ tên có **dấu tiếng Việt + ký tự đặc biệt** (`Nguyễn Thị <script>`) → hiển thị đúng,
      không chèn mã?
- [ ] Họ tên **rất dài** (200 ký tự) → không vỡ layout bảng?
- [ ] Mã bệnh nhân **trùng** mã đã có → báo lỗi rõ ("Mã bệnh nhân đã tồn tại…")?
- [ ] Mã BHXH trùng người khác → báo `PATIENT_SUSPECTED_DUPLICATE` rõ ràng?
- [ ] Lý do hủy **rất dài** (2000 ký tự) → có chặn hay cắt?
- [ ] Mở **2 tab trình duyệt**, cùng mở một ca để đọc → khóa mềm chặn đúng?
- [ ] Bấm "Chốt đọc" **2 lần liên tiếp rất nhanh** → có tạo 2 báo cáo?
- [ ] Ngắt mạng giữa lúc lưu báo cáo → báo "Không gọi được máy chủ, thử lại."?
- [ ] Để phiên hết hạn (chờ 15 phút) rồi bấm thao tác → về màn đăng nhập, không lỗi trắng trang?
- [ ] Thu nhỏ cửa sổ về **chiều ngang điện thoại** → layout còn dùng được?
- [ ] Dùng **chỉ bàn phím** (Tab + Enter) đi hết luồng bước 2–6 → làm được không?
      (liên quan `A11Y-01`, `A11Y-02`, `UX-01`)

---

## Kết luận phiên UAT

| | |
|---|---|
| Số bước đã tick | ______ / ______ |
| Số bước bỏ qua | ______ (lý do đã ghi ở từng bước) |
| Lỗi **mới** phát hiện | ______ → ghi vào `reports/bug-report.md` |
| Lỗi **Nặng** còn mở | ______ |
| **Kết luận: đủ điều kiện deploy?** | ☐ Có ☐ Không — vì: ____________________ |

Người chạy: ________________  Ngày: ________  Ký: ________
