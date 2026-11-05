# VR-RIS PACS — Ma trận phân quyền RBAC

Tài liệu con của `README.md`, mục 6A: [//] Ma trận phân quyền RBAC (T48 trong sheet Công việc). Gom quyền đang nằm rải ở `README.md` Phần 5, `03-state-machine.md` mục 2.5, `04-his-ris-reconciliation.md`, `api/openapi.yaml` (`x-allowed-roles`) và `Frontend/src/lib/auth.ts` về **một chỗ**, để backend (IN-14) viết theo và Frontend sửa cho khớp.

| Phiên bản | 0.1 (nháp — chưa được nhóm xác nhận) |
|---|---|
| Ngày | 2026-11-05 |
| Trạng thái tài liệu | Mọi điểm gắn ❓ là **đề xuất, chờ nhóm xác nhận**. Tài liệu này không đưa ra quyết định mới, chỉ gộp các đề xuất đã có |
| Nguồn ưu tiên khi các nguồn lệch nhau | `03-state-machine.md` mục 2.5 (chuyển trạng thái) > `openapi.yaml` (endpoint) > `04-…` (đối chiếu HIS/RIS) > `Frontend/auth.ts` (chỉ để đối chiếu, không phải nguồn quyết định) |
| Liên quan | IN-13, IN-14, IN-15; quyết định D15, D16, D22, D24, D34 (sheet Quyết định mở) |

Quy ước: mã vai trò là `ADMIN / CLINICIAN / TECHNICIAN / RADIOLOGIST / APPROVER` (bảng `role`, migration V2). Đây là vai trò **người dùng trong hệ thống**, không phải vai A–D của nhóm làm đồ án.

## 1. Nguyên tắc

1. **Chặn ở tầng API.** Frontend chỉ ẩn hoặc khóa nút; bỏ Frontend đi thì backend vẫn phải trả `403` đúng.
2. **`ADMIN` không đọc và không duyệt báo cáo.** `ADMIN` quản trị và xử lý ngoại lệ (giải phóng khóa, mở lại ca, hủy ca) ❓ D15.
3. **Người duyệt khác người đọc**, kể cả khi một người có cả hai vai trò ❓ D15.
4. **Mặc định từ chối.** Endpoint nào không có trong bảng 3 thì chỉ `ADMIN` gọi được cho tới khi nhóm bổ sung.
5. **Mọi lần bị từ chối vì sai vai trò hoặc sai người giữ ca đều ghi `audit_log`** (IN-15).

## 2. Tài khoản demo

Mật khẩu cả 5 tài khoản là `123456` (`database/README.md`). Chỉ dùng cho dev.

| Tài khoản | Vai trò | Khoa (`department_id`) |
|---|---|---|
| `admin` | `ADMIN` | 1 |
| `bslamsang` | `CLINICIAN` | 7 |
| `ktv` | `TECHNICIAN` | 1 |
| `bscdha` | `RADIOLOGIST` | 1 |
| `bsduyet` | `APPROVER` | 1 |

Mỗi tài khoản đang chỉ có một vai trò. Để thử quy tắc "người duyệt ≠ người đọc" và khóa mềm cần thêm một tài khoản `RADIOLOGIST` và một tài khoản `APPROVER` nữa (xem mục 7).

## 3. Endpoint × vai trò

Lấy từ `x-allowed-roles` trong `openapi.yaml` v0.1. ✔ = được gọi. Dấu `*` = còn điều kiện ở mục 4 hoặc 5.

| Endpoint | ADMIN | CLINICIAN | TECHNICIAN | RADIOLOGIST | APPROVER |
|---|---|---|---|---|---|
| `POST /auth/login` | công khai | công khai | công khai | công khai | công khai |
| `POST /auth/logout`, `GET /auth/me` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `GET /patients`, `GET /patients/{id}` | ✔ | ✔ * | ✔ | ✔ | ✔ |
| `POST /patients` | ✔ | ✔ | ✔ | — | — |
| `PATCH /patients/{id}` | ✔ * | ✔ * | ✔ * | — | — |
| `GET /imaging-orders`, `GET /imaging-orders/{id}` | ✔ | ✔ * | ✔ | ✔ | ✔ |
| `POST /imaging-orders`, `PATCH /imaging-orders/{id}` | ✔ | ✔ | ✔ | — | — |
| `POST /imaging-orders/{id}/studies` | ✔ | ✔ | ✔ | — | — |
| `GET /imaging-orders/{id}/clinical-snapshot` | ✔ | ✔ * | ✔ | ✔ | ✔ |
| `GET /studies`, `GET /studies/status-counts`, `GET /studies/{id}` | ✔ | ✔ * | ✔ | ✔ | ✔ |
| `PATCH /studies/{id}` | ✔ | ✔ * | ✔ | — | — |
| `POST /studies/{id}/transitions` | ✔ * | — | ✔ * | ✔ * | ✔ * |
| `GET /studies/{id}/status-history` | ✔ | ✔ * | ✔ | ✔ | ✔ |
| `GET /studies/{id}/reports`, `GET …/reports/current` | ✔ | ✔ * | ✔ | ✔ | ✔ |
| `PUT /studies/{id}/reports/current` | — | — | — | ✔ * | — |
| `GET /identity-conflicts`, `GET …/{id}` | ✔ | — | ✔ | — | — |
| `POST /identity-conflicts/{id}/resolution` | ✔ | — | ✔ | — | — |
| `GET /audit-logs` | ✔ | — | — | — | — |
| `GET /departments`, `/devices`, `/service-items` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `POST /integration/his/orders`, `POST /integration/pacs/study-notifications` | chỉ tài khoản hệ thống (`system`), không dùng JWT của người dùng | | | | |

`CLINICIAN` không gọi được `…/transitions` vì T1 (tạo ca) đi qua `POST /imaging-orders/{id}/studies`, không phải chuyển trạng thái.

## 4. Chuyển trạng thái (T1–T10)

Nguồn: `03-state-machine.md` mục 2.2 và 2.5. Backend kiểm theo thứ tự: đăng nhập → vai trò (`403`) → điều kiện nghiệp vụ (`422` hoặc `409`).

| Chuyển | ADMIN | CLINICIAN | TECHNICIAN | RADIOLOGIST | APPROVER | Điều kiện thêm |
|---|---|---|---|---|---|---|
| T1 Tạo ca → `SCHEDULED` | ✔ | ✔ | ✔ | — | — | Có bệnh nhân, chỉ định, dịch vụ |
| T2 `SCHEDULED → UNREAD` | ✔ | — | ✔ | — | — | Đã có `study_instance_uid` |
| T3 `UNREAD → READING` | — | — | — | ✔ | — | Ca chưa bị người khác khóa |
| T4 `READING → UNREAD` (bỏ dở) ❓ | ✔ (giải phóng khóa) | — | — | ✔ (người đang đọc) | — | Bắt buộc lý do |
| T5 `READING → READ` | — | — | — | ✔ (người đang đọc) | — | Kỹ thuật và kết luận không rỗng ❓ D14 |
| T6 `READ → APPROVING` | — | — | — | — | ✔ | Người duyệt ≠ người đọc ❓ |
| T7 `APPROVING → APPROVED` | — | — | — | — | ✔ (người đã mở duyệt) | Như T6 |
| T8 `APPROVING → READING` (trả lại) ❓ | — | — | — | — | ✔ | Bắt buộc lý do |
| T9 `APPROVED → READING` (mở lại) ❓ | ✔ | — | — | — | ✔ | Bắt buộc lý do, tạo version báo cáo mới |
| T10 `SCHEDULED → CANCELLED` ❓ | ✔ | — | ✔ | — | — | `study_instance_uid` còn NULL, bắt buộc lý do |

## 5. Giới hạn theo dữ liệu (row-level)

| Quy tắc | Áp dụng cho | Trạng thái | Ghi chú cài đặt |
|---|---|---|---|
| `CLINICIAN` chỉ thấy bệnh nhân/chỉ định/ca do mình chỉ định | Mọi `GET` ở mục 3 có dấu `*` cho `CLINICIAN` | ❓ D34, **chưa dùng được** | `ordering_doctor_name` là tên tự do (D10), không phải khóa tới `app_user`. Tạm thời lọc `ordering_doctor_name = app_user.full_name`. Dữ liệu seed hiện không khớp (user là "BS. Lâm Sàng (demo)", chỉ định là "BS. Chỉ Định A (giả)") nên `bslamsang` sẽ không thấy gì. Chọn một: sửa seed cho khớp, hoặc bỏ giới hạn này trong 8 tuần và ghi nợ kỹ thuật |
| Chỉ người đang giữ ca (`reading_user_id`) được lưu báo cáo và chốt đọc | T4, T5, `PUT …/reports/current` | ❓ (khóa mềm, D20) | Kiểm `reading_user_id = người gọi`, nếu không trả `403` |
| Chỉ người đã mở duyệt (`approving_user_id`) được duyệt hoặc trả lại | T7, T8 | ❓ | Như trên |
| Người duyệt ≠ người đọc | T6, T7 | ❓ D15 | So `approving_user_id` với `reading_user_id` của version báo cáo hiện hành; áp dụng cả khi người gọi có nhiều vai trò |
| Sửa trường định danh bệnh nhân (`patient_code`, họ tên, ngày sinh, giới, BHXH) | `PATCH /patients/{id}` | ❓ D24 | Đề xuất chỉ `TECHNICIAN`, `ADMIN`; `CLINICIAN` chỉ sửa các trường còn lại (SĐT, địa chỉ). Phải ghi `audit_log` với `before` và `after` |
| Chỉ `TECHNICIAN`, `ADMIN` được đặt `study_instance_uid` | `PATCH /studies/{id}` | Đã ghi trong `openapi.yaml` | Với `CLINICIAN`, trường này bị bỏ qua hoặc trả `403` (chọn một, ghi vào OpenAPI) |
| `ADMIN` được xem hết | Mọi `GET` | Theo đề xuất | |

## 6. Đối chiếu với Frontend (`auth.ts`)

Frontend mới chỉ ẩn nút. Cột "Đề xuất" là quyền mà backend sẽ áp dụng; Frontend cần sửa theo (gộp vào T30).

| Quyền trong Frontend | Frontend hiện tại | Đề xuất | Hành động |
|---|---|---|---|
| `patient.create` | ADMIN, CLINICIAN, TECHNICIAN | Giữ nguyên | — |
| `patient.update` | ADMIN, CLINICIAN, TECHNICIAN | Giữ nguyên, nhưng trường định danh chỉ TECHNICIAN, ADMIN ❓ D24 | Frontend khóa riêng các trường định danh với `CLINICIAN` |
| `order.sync` | ADMIN, CLINICIAN, TECHNICIAN | Chưa có endpoint tương ứng trong OpenAPI ❓ | Giữ nguyên cho tới khi có endpoint |
| `clinical.view` | Cả 5 | Giữ nguyên | — |
| `report.write` | ADMIN, RADIOLOGIST | **Chỉ RADIOLOGIST** | **Bỏ ADMIN** |
| `report.approve` | ADMIN, APPROVER | **Chỉ APPROVER** | **Bỏ ADMIN** |
| `study.markReceived` | ADMIN, TECHNICIAN | Giữ nguyên (tương ứng T2) | — |
| `study.reopen` | ADMIN | **ADMIN, APPROVER** ❓ D16 | **Thêm APPROVER** |
| `study.cancel` | (chưa có) | **ADMIN, TECHNICIAN** ❓ D07 | **Thêm mới** |
| `study.release` (giải phóng khóa) | (chưa có) | **ADMIN** ❓ D20 | **Thêm mới** |
| `conflict.resolve` | (chưa có) | **ADMIN, TECHNICIAN** ❓ D22 | **Thêm mới** (màn danh sách lệch dữ liệu, T34) |
| `audit.view` | (chưa có) | **ADMIN** | **Thêm mới** (T36) |
| `list.print` | Cả 5 | Giữ nguyên | — |
| `config.system` | ADMIN | ADMIN; chưa có endpoint quản lý người dùng/máy trong OpenAPI ❓ | Giữ nguyên, ghi nợ kỹ thuật |

Frontend còn **không kiểm** điều kiện "người giữ ca" và "người duyệt ≠ người đọc". Đó là việc của backend, không sửa ở Frontend.

## 7. Dữ liệu seed cần bổ sung để kiểm thử

Thêm bằng migration mới (không sửa V7 vì đã nằm trên `main`; xem `database/README.md` mục quy tắc):

- Một tài khoản `RADIOLOGIST` thứ hai (ví dụ `bscdha2`), để thử khóa mềm giữa hai bác sĩ.
- Một tài khoản `APPROVER` thứ hai, hoặc một tài khoản có **cả** `RADIOLOGIST` và `APPROVER`, để thử "người duyệt ≠ người đọc".
- Một chỉ định có `ordering_doctor_name` trùng `full_name` của `bslamsang`, nếu giữ giới hạn row-level cho `CLINICIAN` (D34).

Gộp chung với migration V8 nếu V8 được tạo (T16).

## 8. Test case RBAC (đề xuất cho IN-14)

| Mã | Tình huống | Gọi | Kết quả mong đợi |
|---|---|---|---|
| RBAC-01 | Chưa đăng nhập | `GET /studies` không có token | `401` |
| RBAC-02 | Token hết hạn hoặc sai chữ ký | `GET /auth/me` | `401` |
| RBAC-03 | `RADIOLOGIST` tạo bệnh nhân | `POST /patients` | `403` |
| RBAC-04 | `APPROVER` lưu báo cáo | `PUT …/reports/current` | `403` |
| RBAC-05 | `ADMIN` lưu báo cáo | `PUT …/reports/current` | `403` |
| RBAC-06 | `ADMIN` chuyển T7 (duyệt) | `POST …/transitions` | `403` |
| RBAC-07 | `CLINICIAN` xem audit log | `GET /audit-logs` | `403` |
| RBAC-08 | `ADMIN` xem audit log | `GET /audit-logs` | `200` |
| RBAC-09 | `RADIOLOGIST` thứ hai lưu báo cáo của ca người khác đang đọc | `PUT …/reports/current` | `403` |
| RBAC-10 | Người vừa đọc vừa có vai trò `APPROVER` duyệt ca mình đọc | T6 hoặc T7 | `403` (hoặc `422`, ghi rõ trong OpenAPI) |
| RBAC-11 | `RADIOLOGIST` xử lý identity-conflict | `POST …/resolution` | `403` (khớp RC-18 ở `04-…`) |
| RBAC-12 | `TECHNICIAN` hủy ca đã có `study_instance_uid` | T10 | `422` (đúng vai trò nhưng sai điều kiện) |
| RBAC-13 | `CLINICIAN` đặt `study_instance_uid` | `PATCH /studies/{id}` | Từ chối theo cách đã chọn ở mục 5 |
| RBAC-14 | Mỗi lần `403` ở trên | — | Có thêm dòng `audit_log` ghi người gọi, endpoint, lý do từ chối |

## 9. Điểm mở cần nhóm chốt trước khi viết RBAC

| ID | Câu hỏi | Đề xuất ở tài liệu này |
|---|---|---|
| D15 | `ADMIN` không đọc/duyệt; người duyệt ≠ người đọc kể cả `ADMIN` | Áp dụng |
| D16 | Ai được mở lại ca đã duyệt | `ADMIN`, `APPROVER`, bắt buộc lý do |
| D20 | Khóa mềm: thời hạn và ai giải phóng | Chưa chốt; tạm: `ADMIN` giải phóng được |
| D22 | Ai xử lý identity-conflict | `ADMIN`, `TECHNICIAN` |
| D24 | Ai được sửa tay trường định danh; cờ "đã sửa tay" lưu ở đâu | `TECHNICIAN`, `ADMIN`; nơi lưu chưa chọn |
| D34 | `CLINICIAN` chỉ thấy bệnh nhân mình chỉ định, dựa vào đâu | Chưa dùng được vì D10; cần chọn: sửa seed, hoặc bỏ giới hạn và ghi nợ kỹ thuật |
| (mới) | Một người có nhiều vai trò: quyền là hợp của các vai trò, nhưng "người duyệt ≠ người đọc" vẫn áp dụng | Đề xuất áp dụng |
| (mới) | Có cần endpoint quản lý người dùng, máy, phân quyền (`config.system`) trong 8 tuần không | Không; seed bằng migration, ghi nợ kỹ thuật |
