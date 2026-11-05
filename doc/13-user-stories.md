# VR-RIS PACS — User story theo vai trò

Tài liệu con của `README.md`, mục 6A: [//] User story theo vai trò, **viết dần khi code**. Đợt viết đầu tiên này phủ **toàn bộ hạng mục IN** (`01-scope.md` mục 3, IN-01…IN-24), không chỉ phần đang code — theo đúng yêu cầu khi khởi tạo file này.

| Phiên bản | 0.1 (nháp — chưa được nhóm xác nhận) |
|---|---|
| Ngày | 2026-11-05 |
| Trạng thái tài liệu | Mọi story đều ở trạng thái **Draft** (chưa code, chưa review với bác sĩ/KTV thật). Mọi điểm gắn ❓ là đề xuất |
| Nguồn | README mục 5 (vai trò) · `01-scope.md` mục 3 (IN-01…IN-24) · `02-erd.md` · `03-state-machine.md` (mã T1–T10) · `06-rbac-matrix.md` (quyền) · `api/openapi.yaml` (endpoint) · README mục 7.2 (tuần) · `Frontend/` (đối chiếu prototype đã có) |
| Nguồn ưu tiên khi lệch nhau | `01-scope.md` (phạm vi) > `03-state-machine.md` (chuyển trạng thái) > `06-rbac-matrix.md` (quyền) > `api/openapi.yaml` (endpoint) |

---

## 0. Persona 5 vai trò

Lấy đúng tên và mô tả từ README mục 5 và `06-rbac-matrix.md` mục 2 (tài khoản demo). Không đặt vai trò mới.

### Bác sĩ lâm sàng (`CLINICIAN`, tài khoản demo `bslamsang`)

Làm việc ở một khoa lâm sàng (nội, ngoại, cấp cứu…), không thuộc khoa CĐHA — là nơi **phát sinh chỉ định**. Công việc hằng ngày: khám bệnh nhân, ra y lệnh, chỉ định cận lâm sàng (trong đó có CĐHA), rồi quay lại xem kết quả để ra quyết định điều trị. Màn hình dùng nhiều nhất: modal "Thông tin lâm sàng" (xem lại kết quả đã chỉ định cho bệnh nhân của mình). Điều làm họ mất thời gian: không biết ảnh/kết quả CĐHA đã có chưa nếu không hỏi trực tiếp khoa CĐHA. Điều họ sợ sai nhất: chỉ định hoặc xem nhầm kết quả của bệnh nhân khác khi tên/mã dễ gây nhầm lẫn.

### KTV chụp (`TECHNICIAN`, tài khoản demo `ktv`)

Làm việc tại phòng máy (CT/MR/CR/US). Công việc hằng ngày: tiếp nhận bệnh nhân theo lịch, kiểm tra/nhập thông tin hành chính, thực hiện chụp, cập nhật ca chụp sang "đã có ảnh". Màn hình dùng nhiều nhất: worklist tab **Chờ** và form "Thêm ca chụp" / "Cập nhật ca chụp". Điều làm họ mất thời gian: phải nhập tay lại thông tin lẽ ra đã có ở HIS, hoặc xử lý một ca bị báo lệch dữ liệu giữa chừng khi phòng chụp đang đông. Điều họ sợ sai nhất: chụp nhầm bệnh nhân — ghi sai thông tin vào header ảnh khiến ảnh và hồ sơ không khớp.

### Bác sĩ CĐHA — đọc (`RADIOLOGIST`, tài khoản demo `bscdha`)

Đọc ảnh, viết báo cáo kết quả. Công việc hằng ngày: mở lần lượt các ca ở tab **Chưa đọc**, xem ảnh trong OHIF, nhập 5 trường báo cáo (kỹ thuật, mô tả, chẩn đoán, kết luận, khuyến nghị), lưu và chuyển "Đọc xong". Màn hình dùng nhiều nhất: worklist (lọc theo trạng thái) + OHIF + modal báo cáo. Điều làm họ mất thời gian: ca thiếu thông tin lâm sàng nên phải tự suy đoán bối cảnh chỉ định, hoặc phải đọc lại từ đầu khi báo cáo bị người duyệt trả lại. Điều họ sợ sai nhất: bỏ sót tổn thương — đặc biệt khi có gợi ý AI mà lại tin tưởng quá mức (dùng nguyên gợi ý không kiểm tra) hoặc bỏ qua vùng AI không đánh dấu (xem `doc/11-ai-design.md` mục 7).

### Bác sĩ duyệt (`APPROVER`, tài khoản demo `bsduyet`)

Duyệt kết quả cuối trước khi trả về HIS. Công việc hằng ngày: xem lại báo cáo của bác sĩ CĐHA (không phải của chính mình — người duyệt luôn khác người đọc), đối chiếu ảnh gốc khi cần, duyệt hoặc trả lại kèm lý do. Màn hình dùng nhiều nhất: worklist tab trạng thái "Đang duyệt" + modal báo cáo (chế độ chỉ xem + nút duyệt/trả lại). Điều làm họ mất thời gian: phải mở lại ảnh gốc để kiểm chứng khi phần mô tả của báo cáo không đủ rõ. Điều họ sợ sai nhất: duyệt một báo cáo sai của đồng nghiệp mà không phát hiện kịp khi khối lượng ca lớn — đây là lớp kiểm soát cuối cùng trước khi kết quả về tay bác sĩ lâm sàng.

### Admin

Quản trị hệ thống. Công việc hằng ngày: cấu hình máy/khoa/người dùng, xử lý các ca bị báo lệch dữ liệu HIS/RIS mà KTV chưa xử lý, giải phóng khóa mềm khi bác sĩ bỏ ca giữa chừng không rõ lý do, tra `audit_log` khi có khiếu nại hoặc nghi vấn truy cập sai. Màn hình dùng nhiều nhất: màn "Danh sách lệch dữ liệu" và màn audit log (cả hai **chưa có trong Frontend hiện tại** — xem mục 5). Điều làm họ mất thời gian: trong 8 tuần chưa có màn quản lý người dùng/vai trò, phải sửa trực tiếp qua seed/migration. Điều họ sợ sai nhất: giải phóng khóa mềm hoặc mở lại một ca đã duyệt sai thời điểm, làm mất thao tác người khác đang làm dở hoặc che giấu một sai sót cần giữ lại để truy vết.

---

## 1. Quy ước

- **Mã story:** `US-<VAITRO>-<số 2 chữ số>`. Tiền tố vai trò: `KTV` (TECHNICIAN), `BSLS` (CLINICIAN), `BSCDHA` (RADIOLOGIST), `BSDUYET` (APPROVER), `ADMIN`.
- **Mẫu:** "Là [vai trò], tôi muốn [hành động] để [lợi ích]."
- **Tiêu chí chấp nhận:** dạng Given / When / Then, **ít nhất hai** tiêu chí mỗi story, trong đó **ít nhất một** là ca lỗi hoặc ca bị từ chối quyền (403/409/422 — đúng mã lỗi của `03-state-machine.md` mục 3 và `api/openapi.yaml`).
- **Trường bắt buộc** mỗi story:

  | Trường | Ý nghĩa |
  |---|---|
  | Mã IN | Hạng mục tương ứng ở `01-scope.md` mục 3 |
  | Endpoint | `METHOD /đường-dẫn` theo `api/openapi.yaml`; ❓ nếu chưa có endpoint |
  | Chuyển trạng thái | Mã `T1`–`T10` (`03-state-machine.md` mục 2.2) nếu story đổi trạng thái ca chụp; `—` nếu không |
  | Quyền | Vai trò được phép theo `06-rbac-matrix.md` |
  | Tuần dự kiến | Theo README mục 7.2 |
  | Trạng thái story | `Draft` / `Ready` / `Done` |

- **Trạng thái story** (khác trạng thái ca chụp): `Draft` = mới viết, chưa review nghiệp vụ; `Ready` = đã review, sẵn sàng đưa vào sprint; `Done` = đã code và có test khớp tiêu chí chấp nhận. **Toàn bộ story trong đợt viết đầu tiên này là `Draft`.**

## 2. Quy ước viết tiếp (khi code)

File này "viết dần khi code" (README mục 6A) — không viết hết một lần rồi đóng băng:

1. Khi bắt đầu code một tính năng, tìm story tương ứng ở mục 3. Nếu story đã có: đổi **Trạng thái story** sang `Ready` trước khi code, và sang `Done` kèm ngày khi test khớp tiêu chí chấp nhận đã pass.
2. Nếu phát sinh hành vi không có story nào khớp (ví dụ một nhánh lỗi mới phát hiện khi code): **thêm story mới**, đánh số tiếp theo cuối cùng của đúng vai trò đó (không chèn giữa, không đổi số story cũ).
3. Nếu một story đổi nội dung đáng kể khi code (khác với thiết kế ban đầu): sửa trực tiếp story đó, ghi một dòng vào mục "Phát hiện mâu thuẫn và đã sửa" ở cuối file nếu lý do là tài liệu khác (`01-scope.md`, `03-state-machine.md`…) đã đổi.
4. Người sửa: bất kỳ ai code tính năng đó cập nhật trực tiếp, không cần chờ người viết story gốc.
5. Không xóa story đã `Done` dù tính năng sau này bị cắt — chuyển trạng thái ca chụp sang ghi chú "đã cắt, xem `01-scope.md` mục 9 (bảng thay đổi phạm vi)" thay vì xóa, để giữ lịch sử quyết định.

---

## 3. Story theo vai trò

### 3.1. KTV chụp (`TECHNICIAN`)

#### US-KTV-01 — Tạo bệnh nhân mới khi tiếp nhận

**Là** KTV chụp, **tôi muốn** tạo hồ sơ bệnh nhân mới khi tiếp nhận một người chưa từng đến khoa, **để** có đối tượng gắn chỉ định và ca chụp.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-01 |
| Endpoint | `POST /patients` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given chưa có bệnh nhân nào mang `patient_code` X, When KTV nhập đủ thông tin bắt buộc (mã, họ tên, ngày sinh hoặc chỉ năm sinh, giới tính) và lưu, Then hệ thống tạo bệnh nhân mới, trả `201`, ghi `audit_log` (`action = CREATE`).
- Given đã tồn tại bệnh nhân mang `patient_code` X, When KTV tạo một bệnh nhân khác với cùng mã, Then hệ thống trả `409` (trùng mã), không tạo bản ghi mới (khớp IN-01 "chặn trùng mã").
- Given KTV chỉ nhập năm sinh (không có ngày/tháng), When lưu, Then `dob_precision = YEAR` và tuổi hiển thị về sau là xấp xỉ, không phải tuổi chính xác (`02-erd.md` 4.2).

#### US-KTV-02 — Tìm bệnh nhân để tránh tạo trùng

**Là** KTV chụp, **tôi muốn** tìm bệnh nhân theo mã hoặc họ tên trước khi tạo mới, **để** không tạo trùng hồ sơ cho cùng một người.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-01 |
| Endpoint | `GET /patients` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given có bệnh nhân với họ tên gần giống (ví dụ trùng họ tên, khác ngày sinh), When KTV tìm theo tên, Then kết quả hiển thị đủ cả hai để KTV tự phân biệt bằng ngày sinh/mã.
- Given từ khóa tìm không khớp bệnh nhân nào, When KTV tìm kiếm, Then trả danh sách rỗng (`200`, `[]`), không báo lỗi.

#### US-KTV-03 — Sửa thông tin một chỉ định

**Là** KTV chụp, **tôi muốn** sửa lại chỉ định đã nhập sai (ví dụ sai khoa chỉ định, sai bác sĩ chỉ định), **để** dữ liệu khớp phiếu giấy thực tế.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-02 |
| Endpoint | `PATCH /imaging-orders/{orderId}` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given một chỉ định đã tồn tại, When KTV sửa `ordering_doctor_name`, Then hệ thống lưu thay đổi và ghi `audit_log` (`UPDATE`, có `before_value`/`after_value`).
- Given một tài khoản `RADIOLOGIST` gọi thẳng API (không qua giao diện), When gọi `PATCH /imaging-orders/{orderId}`, Then trả `403` — vai trò này không có quyền sửa chỉ định (`06-rbac-matrix.md` mục 3).

#### US-KTV-04 — Thêm ca chụp vào một chỉ định

**Là** KTV chụp, **tôi muốn** thêm một ca chụp cho một chỉ định, gán máy và dịch vụ CĐHA, **để** ca xuất hiện ở worklist chờ thực hiện.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-03 |
| Endpoint | `POST /imaging-orders/{orderId}/studies` |
| Chuyển trạng thái | T1 (khởi tạo → `SCHEDULED`) |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given một chỉ định đã có bệnh nhân hợp lệ, When KTV chọn dịch vụ CĐHA và máy rồi lưu, Then hệ thống tạo `study` mới ở trạng thái `SCHEDULED`, sinh `accession_number` ngay (`02-erd.md` 4.3), ghi một dòng `study_status_history` (`from_status = NULL`) và một dòng `audit_log` (`STATUS_CHANGE`).
- Given chỉ định chưa gắn bệnh nhân hợp lệ, When KTV cố thêm ca chụp, Then hệ thống trả `422` (thiếu điều kiện), không tạo `study`.
- Given một chỉ định có nhu cầu chụp 2 dịch vụ khác nhau trong cùng một lần, When KTV thêm lần lượt hai ca chụp, Then hệ thống tạo **hai** `study` riêng (một `imaging_order` có nhiều `study` — `02-erd.md` mục 9, ❓ chưa xác nhận).

#### US-KTV-05 — Cập nhật ca chụp, so sánh hiện tại/mới

**Là** KTV chụp, **tôi muốn** thấy bảng so sánh "Thông tin hiện tại" và "Thông tin cập nhật" khi sửa một ca chụp, **để** tránh sửa nhầm hoặc bỏ sót trường cần đổi.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-04 |
| Endpoint | `PATCH /studies/{studyId}` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `TECHNICIAN` (và `CLINICIAN` với phần sửa chung — xem `06-rbac-matrix.md` mục 5) |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given một ca chụp đã có `device_id`, When KTV đổi sang máy khác và xem màn so sánh trước khi lưu, Then màn hình hiển thị đúng giá trị cũ bên trái, giá trị mới bên phải cho từng trường đã đổi (chỉ UI, không phải một API riêng).
- Given KTV là vai trò `CLINICIAN`, When gọi `PATCH /studies/{studyId}` để đặt `study_instance_uid`, Then hệ thống từ chối theo cách đã chọn ở `06-rbac-matrix.md` mục 5 (chỉ `TECHNICIAN`/`ADMIN` được đặt trường này) — ❓ mã lỗi cụ thể (`403` hay bỏ qua trường) chưa chốt.

#### US-KTV-06 — Đánh dấu ca đã có ảnh, chuyển "Chưa đọc"

**Là** KTV chụp, **tôi muốn** xác nhận một ca đã đẩy ảnh vào PACS, **để** ca chuyển sang trạng thái "Chưa đọc" và bác sĩ CĐHA thấy được trong worklist.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T2 (`SCHEDULED → UNREAD`) |
| Quyền | `ADMIN`, `TECHNICIAN` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given ca đang `SCHEDULED` và đã điền `study_instance_uid` (tay, trong 8 tuần), When KTV gọi chuyển T2, Then ca chuyển `UNREAD`, đặt `completed_at`, ghi `study_status_history` + `audit_log` (`STATUS_CHANGE`).
- Given ca đang `SCHEDULED` nhưng **chưa** có `study_instance_uid`, When KTV gọi chuyển T2, Then hệ thống trả `422` (SM-12, `03-state-machine.md` mục 3) — không cho chuyển khi chưa có ảnh để deep-link.
- Given một tài khoản `CLINICIAN` gọi T2, When gửi yêu cầu, Then trả `403` (SM-08) — vai trò này không có trong bảng 2.5.

#### US-KTV-07 — Xử lý một ca lệch dữ liệu HIS/RIS

**Là** KTV chụp, **tôi muốn** xem và xử lý một bản ghi lệch dữ liệu (ví dụ ngày sinh RIS khác ngày sinh HIS), **để** chọn giữ RIS, lấy theo HIS, hoặc sửa tay có kiểm soát.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-05 |
| Endpoint | `POST /identity-conflicts/{conflictId}/resolution` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `TECHNICIAN` (D22, `06-rbac-matrix.md` mục 9) |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given một `identity_conflict` ở trạng thái `PENDING` (năm sinh RIS 1958 khác HIS 1952 — đúng ca known issue README mục 2.5), When KTV chọn `KEEP_RIS` kèm ghi chú, Then `resolution = KEEP_RIS`, ghi `resolved_by_user_id`/`resolved_at`, dữ liệu RIS **không đổi**.
- Given một `identity_conflict` đã ở trạng thái `KEEP_RIS`/`TAKE_HIS`/`MANUAL_EDIT` (đã xử lý), When KTV cố xử lý lại, Then hệ thống từ chối hoặc yêu cầu xác nhận ghi đè — ❓ hành vi cụ thể chưa chốt (`04-his-ris-reconciliation.md` chưa định nghĩa).
- Given một tài khoản `RADIOLOGIST` gọi API này, When gửi yêu cầu, Then trả `403` (RBAC-11, `06-rbac-matrix.md` mục 8).

#### US-KTV-08 — Hủy một ca chưa chụp

**Là** KTV chụp, **tôi muốn** hủy một ca chụp vừa tạo nhầm hoặc không còn cần nữa (chưa chụp), **để** ca không nằm lại trong worklist chờ.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T10 (`SCHEDULED → CANCELLED`) |
| Quyền | `ADMIN`, `TECHNICIAN` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given ca đang `SCHEDULED`, chưa có `study_instance_uid`, When KTV hủy kèm lý do, Then ca chuyển `CANCELLED` (trạng thái cuối), ghi `study_status_history.reason`.
- Given ca đã có `study_instance_uid` (đã có ảnh), When KTV cố hủy, Then hệ thống trả `422` (RBAC-12) — không cho hủy sau khi đã có ảnh trong PACS (`03-state-machine.md` mục 2.3).
- Given KTV gọi hủy nhưng bỏ trống lý do, When gửi yêu cầu, Then trả `422` (SM-11) — `reason` bắt buộc.

#### US-KTV-09 — Đăng nhập vào hệ thống

**Là** KTV chụp, **tôi muốn** đăng nhập bằng tài khoản của mình, **để** vào đúng worklist và chỉ thấy những thao tác vai trò mình được phép. *(Mẫu áp dụng giống nhau cho cả 5 vai trò — không lặp lại ở các vai trò khác.)*

| Trường | Giá trị |
|---|---|
| Mã IN | IN-13 |
| Endpoint | `POST /auth/login` |
| Chuyển trạng thái | — |
| Quyền | Công khai (không cần đăng nhập trước) |
| Tuần dự kiến | 3 |
| Trạng thái story | Draft |

- Given tài khoản `ktv` tồn tại và đúng mật khẩu, When đăng nhập, Then nhận JWT hợp lệ, `GET /auth/me` trả đúng vai trò `TECHNICIAN`, ghi `audit_log` (`LOGIN`) — ❓ cột `action` chưa phân biệt được đăng nhập thành công/thất bại (`09-bao-mat-toan-ven-du-lieu.md` mục 2.2).
- Given sai mật khẩu, When đăng nhập, Then trả `401`, không cấp token.
- Given tài khoản bị khóa do đăng nhập sai nhiều lần liên tiếp, When đăng nhập đúng mật khẩu lần tiếp theo, Then vẫn bị từ chối cho tới khi hết thời hạn khóa — ❓ ngưỡng và thời hạn khóa chưa chốt (`09-…` mục 1.4).

#### US-KTV-10 — Xem và lọc worklist tab "Chờ"

**Là** KTV chụp, **tôi muốn** xem worklist tab Chờ, lọc theo cây máy/khoa, modality và khoảng thời gian, **để** biết ca nào cần chụp tiếp theo tại phòng máy của mình.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-07, IN-12 |
| Endpoint | `GET /studies` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 5–6 |
| Trạng thái story | Draft |

- Given có ca ở trạng thái `SCHEDULED` cho máy CT-01, When KTV lọc theo khoa → nhóm → máy CT-01 và tab "Chờ", Then danh sách chỉ hiện ca `SCHEDULED` của máy đó, đúng cột theo README mục 3.1.
- Given KTV chọn khoảng thời gian "Hôm qua", When tải worklist, Then chỉ hiện ca có `scheduled_at` trong khoảng hôm qua (00:00–23:59 giờ Việt Nam, quy đổi từ UTC — `02-erd.md` 4.7).
- Given tham số lọc không khớp ca nào, When tải worklist, Then trả danh sách rỗng kèm số đếm `0`, không lỗi.

---

### 3.2. Bác sĩ lâm sàng (`CLINICIAN`)

#### US-BSLS-01 — Tạo chỉ định CĐHA cho bệnh nhân

**Là** bác sĩ lâm sàng, **tôi muốn** tạo một chỉ định CĐHA cho bệnh nhân mình đang khám, **để** bệnh nhân được chuyển sang khoa CĐHA thực hiện.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-02 |
| Endpoint | `POST /imaging-orders` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given bệnh nhân đã tồn tại trong hệ thống, When bác sĩ lâm sàng tạo chỉ định với chẩn đoán ban đầu, khoa chỉ định, Then hệ thống tạo `imaging_order` mới, `source = MANUAL`, ghi `audit_log` (`CREATE`).
- Given thiếu `patient_id` hoặc `ordering_department_id`, When tạo chỉ định, Then trả `422`, không tạo bản ghi.

#### US-BSLS-02 — Xem thông tin lâm sàng tổng quan và xét nghiệm

**Là** bác sĩ lâm sàng, **tôi muốn** mở modal thông tin lâm sàng tab 1 (tổng quan khám) và tab 2 (xét nghiệm) của một chỉ định, **để** có đủ bối cảnh trước khi đọc kết quả CĐHA.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-11 |
| Endpoint | `GET /imaging-orders/{orderId}/clinical-snapshot` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6 |
| Trạng thái story | Draft |

- Given chỉ định đã có `clinical_snapshot` (seed giả lập HIS), When mở tab 1, Then hiển thị đủ chẩn đoán phiếu khám, sinh hiệu, quá trình bệnh lý theo đúng cấu trúc README mục 3.3.
- Given chỉ định **chưa** có `clinical_snapshot` (chưa đồng bộ HIS), When mở modal, Then tab 1/2 hiển thị trạng thái rỗng rõ ràng ("chưa có dữ liệu từ HIS"), không lỗi trắng trang.

#### US-BSLS-03 — Xem tên dịch vụ CĐHA và kết luận ở tab 3

**Là** bác sĩ lâm sàng, **tôi muốn** xem tên dịch vụ CĐHA đã chỉ định và kết luận (nếu đã có) ngay trong modal lâm sàng tab 3, **để** không phải hỏi khoa CĐHA đã đọc xong chưa.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-11 |
| Endpoint | `GET /studies/{studyId}/reports/current` (ghép với `GET /imaging-orders/{orderId}/clinical-snapshot` để lấy danh sách dịch vụ) |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6 |
| Trạng thái story | Draft |

- Given một dịch vụ CĐHA trong chỉ định đã có báo cáo `APPROVED`, When bác sĩ lâm sàng mở tab 3, Then dòng dịch vụ đó hiển thị `service_item.name` và `report.conclusion` của **bản hiện hành** (`02-erd.md` 4.10) — không lấy từ `clinical_snapshot` (`01-scope.md` mục 3.3, đã đổi so với bản trước).
- Given dịch vụ CĐHA chưa có báo cáo nào (`study` còn `SCHEDULED`/`UNREAD`), When mở tab 3, Then cột kết luận hiển thị rỗng/"chưa có kết quả", không báo lỗi và không hiện dữ liệu giả từ snapshot.

#### US-BSLS-04 — Xem ảnh CĐHA từ modal lâm sàng (cửa vào b)

**Là** bác sĩ lâm sàng, **tôi muốn** bấm "Xem ảnh" ngay tại dòng dịch vụ CĐHA trong tab 3, **để** xem lại ảnh gốc mà không cần vào worklist CĐHA.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-23 (cửa vào b), IN-19 (phần người dùng) |
| Endpoint | `GET /studies/{studyId}` (lấy `study_instance_uid` để ghép deep-link) — ❓ xem mục 6 điểm 2 (chưa có endpoint ghi riêng `VIEW_IMAGE`) |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6–7 |
| Trạng thái story | Draft |

- Given dịch vụ CĐHA đã có `study_instance_uid`, When bác sĩ lâm sàng bấm "Xem ảnh" ở tab 3, Then OHIF mở đúng study đó (deep-link theo README mục 2.7) và hệ thống ghi `audit_log` (`VIEW_IMAGE`, `02-erd.md` mục 4.11) **trước khi** trả URL.
- Given dịch vụ CĐHA **chưa** có `study_instance_uid` (chưa có ảnh), When bác sĩ lâm sàng nhìn dòng dịch vụ đó, Then nút "Xem ảnh" **ẩn hoặc disable** (❓ chọn ẩn hay disable chưa chốt), không mở viewer rỗng (`01-scope.md` IN-19 điểm 3).

---

### 3.3. Bác sĩ CĐHA — đọc (`RADIOLOGIST`)

#### US-BSCDHA-01 — Mở ca để đọc

**Là** bác sĩ CĐHA, **tôi muốn** mở một ca đang "Chưa đọc" để bắt đầu đọc, **để** giữ ca cho riêng mình trong lúc đọc (khóa mềm) và không bị người khác đọc trùng.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T3 (`UNREAD → READING`) |
| Quyền | `RADIOLOGIST` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given ca đang `UNREAD` và chưa ai mở, When bác sĩ CĐHA mở ca, Then ca chuyển `READING`, đặt `reading_user_id` = người mở, tạo khóa mềm.
- Given ca đang `READING` bởi bác sĩ A, When bác sĩ B cũng gọi T3 gần như đồng thời, Then một người thành công, người còn lại nhận `409` (SM-16, khóa mềm).

#### US-BSCDHA-02 — Xem ảnh gốc trong OHIF từ worklist (cửa vào a)

**Là** bác sĩ CĐHA, **tôi muốn** bấm "Xem ảnh" ở hàng worklist để mở OHIF đúng study, **để** xem ảnh gốc và (nếu có) overlay AI trước khi viết báo cáo.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-23 (cửa vào a), IN-19 |
| Endpoint | `GET /studies/{studyId}` — ❓ xem mục 6 điểm 2 |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6–7 |
| Trạng thái story | Draft |

- Given ca đã có `study_instance_uid` và đã nạp ảnh mẫu vào Orthanc (IN-19), When bác sĩ CĐHA bấm "Xem ảnh" từ hàng worklist, Then OHIF mở ra đúng study, hiển thị đúng ảnh đã nạp; `audit_log` ghi `VIEW_IMAGE`.
- Given OHIF/Orthanc bị truy cập trực tiếp bằng URL dán tay (không qua RIS), When ai đó mở URL đó, Then **không có dòng audit nào** được ghi — đây là rủi ro đã nêu ở `07-pacs-dicom.md` mục 6.3, lý do OHIF/Orthanc không được mở công khai (README mục 0).

#### US-BSCDHA-03 — Nhập và lưu báo cáo, chuyển "Đọc xong"

**Là** bác sĩ CĐHA, **tôi muốn** nhập 5 trường báo cáo (kỹ thuật, mô tả, chẩn đoán, kết luận, khuyến nghị) và chốt báo cáo, **để** ca chuyển sang chờ duyệt.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-10, IN-06 |
| Endpoint | `PUT /studies/{studyId}/reports/current` (lưu nháp) rồi `POST /studies/{studyId}/transitions` (chốt) |
| Chuyển trạng thái | T5 (`READING → READ`) |
| Quyền | `RADIOLOGIST` (chỉ người đang giữ ca — `reading_user_id`) |
| Tuần dự kiến | 5–6 |
| Trạng thái story | Draft |

- Given bác sĩ đang giữ ca đã điền đủ `technique` và `conclusion` (tối thiểu bắt buộc, `03-state-machine.md` mục 2.4 điểm 1), When chốt báo cáo (T5), Then báo cáo chuyển `DRAFT → FINAL`, ca chuyển `READ`, khóa mềm được nhả.
- Given `technique` hoặc `conclusion` còn trống hoặc chỉ có khoảng trắng, When bác sĩ cố chốt báo cáo, Then hệ thống trả `422` (SM-10).
- Given một bác sĩ CĐHA khác (không phải `reading_user_id`) cố lưu nháp báo cáo của ca này, When gọi `PUT .../reports/current`, Then trả `403` (SM-17) — chỉ người đang giữ ca được sửa.

#### US-BSCDHA-04 — Bỏ dở ca đang đọc

**Là** bác sĩ CĐHA, **tôi muốn** tạm dừng đọc một ca (ví dụ bị gọi đi việc khác) và trả ca về hàng chờ, **để** người khác hoặc chính mình đọc tiếp sau mà không mất báo cáo nháp.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T4 (`READING → UNREAD`) |
| Quyền | Người đang đọc (`reading_user_id`), `ADMIN` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given bác sĩ đang giữ ca ở `READING` đã lưu một phần nháp, When bỏ dở kèm lý do, Then ca về `UNREAD`, khóa mềm được nhả, **nháp báo cáo vẫn còn nguyên** khi mở lại.
- Given bác sĩ bỏ dở nhưng không nhập lý do, When gửi yêu cầu, Then trả `422` (SM-11) — `reason` bắt buộc ở T4.

#### US-BSCDHA-05 — Xem panel chi tiết bệnh nhân và các phiếu liên quan

**Là** bác sĩ CĐHA, **tôi muốn** xem panel chi tiết bệnh nhân (hành chính, chỉ định, kỹ thuật, mô tả, chẩn đoán, kết luận, khuyến nghị) và bảng các phiếu khác cùng bệnh nhân, **để** có đủ ngữ cảnh khi đọc ca hiện tại.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-08 |
| Endpoint | `GET /studies/{studyId}`, `GET /patients/{patientId}` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6 |
| Trạng thái story | Draft |

- Given bệnh nhân có 3 phiếu chỉ định khác trong quá khứ, When bác sĩ CĐHA chọn một ca ở worklist, Then panel bên phải hiển thị đủ thông tin hành chính + bảng phụ liệt kê 3 phiếu kia.
- Given bệnh nhân chỉ có đúng ca đang xem (không có phiếu nào khác), When mở panel, Then bảng phụ hiển thị rỗng, không lỗi.

#### US-BSCDHA-06 — Xem số lượng ca theo nhóm trạng thái

**Là** bác sĩ CĐHA, **tôi muốn** thấy số đếm ca theo từng trạng thái (Chưa đọc, Đang đọc…) trên tiêu đề/bộ lọc, **để** biết còn bao nhiêu ca cần đọc và ưu tiên hợp lý.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-09 |
| Endpoint | `GET /studies/status-counts` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6 |
| Trạng thái story | Draft |

- Given có 12 ca `UNREAD` và 3 ca `READING`, When bác sĩ CĐHA mở bộ lọc trạng thái, Then hiển thị đúng "Chưa đọc (12)", "Đang đọc (3)" — khớp `IDX(status, scheduled_at)` ở `02-erd.md` mục 6.
- Given không có ca nào ở trạng thái `APPROVING`, When xem bộ đếm, Then hiển thị "Đang duyệt (0)", không ẩn dòng đó.

---

### 3.4. Bác sĩ duyệt (`APPROVER`)

#### US-BSDUYET-01 — Mở ca để duyệt

**Là** bác sĩ duyệt, **tôi muốn** mở một ca đã "Đọc xong" để xem xét duyệt, **để** giữ ca cho riêng mình trong lúc duyệt.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T6 (`READ → APPROVING`) |
| Quyền | `APPROVER` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given ca đang `READ` với `reading_user_id` là bác sĩ A, When bác sĩ duyệt (không phải A) mở ca để duyệt, Then ca chuyển `APPROVING`, đặt `approving_user_id`.
- Given bác sĩ duyệt trùng chính người đã đọc ca đó (cùng một tài khoản có cả hai vai trò), When cố mở ca để duyệt, Then hệ thống từ chối (SM-14, `403`/`422` theo quyết định cuối ❓) — người duyệt phải khác người đọc.

#### US-BSDUYET-02 — Duyệt báo cáo, chuyển "Duyệt xong"

**Là** bác sĩ duyệt, **tôi muốn** duyệt một báo cáo đã đọc xong và đúng, **để** kết quả trở thành chính thức và sẵn sàng trả về HIS.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06, IN-10 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T7 (`APPROVING → APPROVED`) |
| Quyền | `APPROVER` (chính người đã mở duyệt) |
| Tuần dự kiến | 5–6 |
| Trạng thái story | Draft |

- Given bác sĩ duyệt đang giữ ca ở `APPROVING`, When duyệt, Then báo cáo chuyển `FINAL → APPROVED`, đặt `approved_by_user_id`/`approved_at`, ca chuyển `APPROVED` (tab "Đã thực hiện").
- Given một tài khoản `ADMIN` gọi T7, When gửi yêu cầu, Then trả `403` (RBAC-06, SM-09) — `ADMIN` không được đọc/duyệt báo cáo (`03-state-machine.md` mục 2.5).

#### US-BSDUYET-03 — Trả báo cáo lại để sửa

**Là** bác sĩ duyệt, **tôi muốn** trả một báo cáo chưa đạt về lại cho bác sĩ CĐHA kèm lý do, **để** báo cáo được sửa trước khi duyệt.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T8 (`APPROVING → READING`) |
| Quyền | `APPROVER` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given báo cáo đang `APPROVING` và bác sĩ duyệt thấy mô tả chưa rõ, When trả lại kèm lý do "Thiếu mô tả vùng tổn thương", Then báo cáo hiện tại chuyển `RETURNED` (giữ nguyên làm lịch sử), ca về `READING`; khi bác sĩ CĐHA chốt lại, tạo bản ghi mới `version + 1`.
- Given bác sĩ duyệt trả lại nhưng không nhập lý do, When gửi yêu cầu, Then trả `422` (SM-11).

#### US-BSDUYET-04 — Xem worklist tab "Đã thực hiện", lọc theo trạng thái

**Là** bác sĩ duyệt, **tôi muốn** lọc worklist theo trạng thái "Đang duyệt" ở tab Đã thực hiện, **để** chỉ thấy các ca đang chờ mình xử lý.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-07, IN-12 |
| Endpoint | `GET /studies` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `CLINICIAN`, `TECHNICIAN`, `RADIOLOGIST`, `APPROVER` |
| Tuần dự kiến | 6 |
| Trạng thái story | Draft |

- Given có ca ở `READ`, `APPROVING`, `APPROVED`, When bác sĩ duyệt lọc trạng thái = "Đang duyệt" ở tab Đã thực hiện, Then chỉ hiện ca `APPROVING`.
- Given bác sĩ duyệt chọn lọc "Đang duyệt" nhưng không có ca nào ở trạng thái đó, When tải lại, Then danh sách rỗng, số đếm "Đang duyệt (0)" vẫn hiển thị (liên hệ US-BSCDHA-06).

---

### 3.5. Admin

#### US-ADMIN-01 — Mở lại ca đã duyệt khi phát hiện sai

**Là** Admin, **tôi muốn** mở lại một ca đã "Duyệt xong" khi phát hiện báo cáo sai sau khi duyệt, **để** có đường đính chính có truy vết thay vì sửa chui.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T9 (`APPROVED → READING`) |
| Quyền | `ADMIN`, `APPROVER` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given ca đang `APPROVED`, When Admin mở lại kèm lý do, Then tạo bản ghi `report` mới (`version + 1`, `DRAFT`), bản `APPROVED` cũ **giữ nguyên không sửa**, ca về `READING`.
- Given một tài khoản `RADIOLOGIST` gọi T9, When gửi yêu cầu, Then trả `403` (SM-15) — chỉ `ADMIN`/`APPROVER` được mở lại ca đã duyệt.
- Given Admin mở lại nhưng không nhập lý do, When gửi yêu cầu, Then trả `422` (SM-15).

#### US-ADMIN-02 — Xem danh sách ca lệch dữ liệu HIS/RIS

**Là** Admin, **tôi muốn** xem danh sách các bản ghi lệch dữ liệu đang chờ xử lý, **để** theo dõi và phân công xử lý định kỳ (README mục 2.5).

| Trường | Giá trị |
|---|---|
| Mã IN | IN-05 |
| Endpoint | `GET /identity-conflicts` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `TECHNICIAN` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given có 5 bản ghi `identity_conflict` ở trạng thái `PENDING` và 2 bản ghi đã xử lý, When Admin mở danh sách với lọc mặc định `resolution = PENDING`, Then chỉ hiện 5 bản ghi chưa xử lý, sắp theo `detected_at`.
- Given một tài khoản `CLINICIAN` gọi API này, When gửi yêu cầu, Then trả `403` — không có trong bảng quyền mục 3 của `06-rbac-matrix.md`.

#### US-ADMIN-03 — Xử lý một ca lệch dữ liệu (vai trò Admin)

**Là** Admin, **tôi muốn** tự xử lý một ca lệch dữ liệu khi KTV chưa xử lý kịp, **để** không tồn đọng bản ghi `PENDING` quá lâu.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-05 |
| Endpoint | `POST /identity-conflicts/{conflictId}/resolution` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `TECHNICIAN` |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given một bản ghi lệch ở trường `full_name` (HIS ghép họ tên khác RIS), When Admin chọn `MANUAL_EDIT` và nhập giá trị đúng, Then `patient.full_name` được cập nhật, `identity_conflict.resolution = MANUAL_EDIT`, ghi `audit_log` (`UPDATE`, có `before_value`/`after_value`).
- Given Admin chọn `MANUAL_EDIT` nhưng để trống ô giá trị nhập, When gửi yêu cầu, Then trả `422`, không cập nhật gì (ngăn lệch dữ liệu càng sửa càng sai).

#### US-ADMIN-04 — Xem audit log theo bệnh nhân hoặc theo ca

**Là** Admin, **tôi muốn** tra `audit_log` theo một bệnh nhân hoặc một ca cụ thể, **để** trả lời được câu hỏi "ai đã xem/sửa dữ liệu này, lúc nào" khi có khiếu nại.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-15 |
| Endpoint | `GET /audit-logs` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN` |
| Tuần dự kiến | 3 |
| Trạng thái story | Draft |

- Given một bệnh nhân có 6 dòng audit log qua trọn một luồng ca chụp (T1→T2→T3→T5→T6→T7, đúng kịch bản nghiệm thu `01-scope.md` mục 6 bước 8), When Admin lọc theo `patient_id`, Then thấy đủ 6 dòng, đúng người thực hiện và thời điểm, kể cả dòng `VIEW_IMAGE` nếu có mở OHIF.
- Given một tài khoản `CLINICIAN` gọi `GET /audit-logs`, When gửi yêu cầu, Then trả `403` (RBAC-07) — chỉ `ADMIN` xem được.

#### US-ADMIN-05 — Sửa thông tin định danh bệnh nhân

**Là** Admin, **tôi muốn** sửa các trường định danh bệnh nhân (mã, họ tên, ngày sinh, giới tính, BHXH) khi phát hiện sai sau khi đã đồng bộ, **để** dữ liệu RIS đúng với thực tế.

| Trường | Giá trị |
|---|---|
| Mã IN | IN-01, IN-14 |
| Endpoint | `PATCH /patients/{patientId}` |
| Chuyển trạng thái | — |
| Quyền | `ADMIN`, `TECHNICIAN` cho trường định danh (D24, `06-rbac-matrix.md` mục 5, ❓ chưa chốt) |
| Tuần dự kiến | 4 |
| Trạng thái story | Draft |

- Given bệnh nhân có `date_of_birth` sai do nhập tay lúc đầu, When Admin sửa lại đúng ngày sinh, Then hệ thống cập nhật, ghi `audit_log` (`UPDATE`, `before_value`/`after_value` không chứa các trường nhạy cảm ngoài phạm vi, `09-…` mục 2.1).
- Given một tài khoản `CLINICIAN` cố sửa `patient_code` (trường định danh), When gọi `PATCH /patients/{patientId}`, Then bị từ chối theo quy tắc D24 (`403`, hoặc chỉ các trường định danh bị khóa — ❓ cơ chế cụ thể chưa chốt).

#### US-ADMIN-06 — Giải phóng khóa mềm khi bác sĩ bỏ ca giữa chừng

**Là** Admin, **tôi muốn** giải phóng khóa mềm của một ca khi bác sĩ đang giữ ca không còn thao tác được (ví dụ mất kết nối, nghỉ đột xuất), **để** ca không kẹt mãi ở trạng thái "Đang đọc".

| Trường | Giá trị |
|---|---|
| Mã IN | IN-06 |
| Endpoint | `POST /studies/{studyId}/transitions` |
| Chuyển trạng thái | T4 (`READING → UNREAD`, do `ADMIN` thực hiện thay người đang giữ ca) |
| Quyền | `ADMIN` |
| Tuần dự kiến | 5 |
| Trạng thái story | Draft |

- Given ca đang `READING` bởi bác sĩ A đã treo quá lâu, When Admin giải phóng khóa kèm lý do, Then ca về `UNREAD`, khóa mềm nhả, bất kỳ bác sĩ CĐHA nào (kể cả A) có thể mở lại.
- Given Admin giải phóng khóa nhưng không nhập lý do, When gửi yêu cầu, Then trả `422` (SM-11, `reason` bắt buộc ở T4).
- ❓ Thời hạn khóa mềm và điều kiện "treo quá lâu" chưa chốt (`03-state-machine.md` mục 2.7 điểm 6, `06-rbac-matrix.md` D20) — tiêu chí chấp nhận trên giả định Admin tự quyết định khi nào cần giải phóng, không có cơ chế tự động.

---

## 4. Bảng đối chiếu phủ sóng theo IN

| Mã IN | Nội dung (tóm tắt) | Story phủ | Ghi chú |
|---|---|---|---|
| IN-01 | Quản lý bệnh nhân | US-KTV-01, US-KTV-02, US-ADMIN-05 | |
| IN-02 | Quản lý chỉ định | US-BSLS-01, US-KTV-03 | |
| IN-03 | Quản lý ca chụp | US-KTV-04 | |
| IN-04 | Màn "Cập nhật ca chụp" so sánh | US-KTV-05 | |
| IN-05 | Quy tắc đối chiếu + log lệch | US-KTV-07, US-ADMIN-02, US-ADMIN-03 | |
| IN-06 | State machine 6 trạng thái + lịch sử | US-KTV-06, US-KTV-08, US-BSCDHA-01, US-BSCDHA-03, US-BSCDHA-04, US-BSDUYET-01, US-BSDUYET-02, US-BSDUYET-03, US-ADMIN-01, US-ADMIN-06 | Phủ cả 10 mã chuyển T1–T10 |
| IN-07 | Worklist 2 tab, cột, phân trang | US-KTV-10, US-BSDUYET-04 | Phân trang/sắp xếp cụ thể chưa có tiêu chí chấp nhận riêng — ❓ bổ sung khi code |
| IN-08 | Panel chi tiết + bảng phiếu | US-BSCDHA-05 | |
| IN-09 | Đếm số lượng theo nhóm trạng thái | US-BSCDHA-06 | |
| IN-10 | Báo cáo kết quả (nháp→chính thức→duyệt) | US-BSCDHA-03, US-BSDUYET-02 | |
| IN-11 | Modal lâm sàng (rút gọn) | US-BSLS-02, US-BSLS-03 | Tab 4, 5 là bảng rỗng — không cần story (không có hành vi người dùng ngoài "xem trống") |
| IN-12 | Bộ lọc (cắt gọn) | US-KTV-10, US-BSDUYET-04 | Chỉ phủ cây máy/modality/khoảng thời gian/trạng thái; lọc theo loại bệnh nhân (Nội/Ngoại trú) chưa có story riêng — ❓ bổ sung khi code |
| IN-13 | Xác thực | US-KTV-09 | Mẫu dùng chung cho 5 vai trò, không lặp lại |
| IN-14 | RBAC chặn ở API | *(không có story riêng)* | Cross-cutting — mọi story ở mục 3 có ít nhất một tiêu chí chấp nhận là ca bị từ chối quyền (`403`), đó chính là cách IN-14 được phủ |
| IN-15 | Audit log | US-ADMIN-04 | |
| IN-16 | Flyway migration, seed data | *(không có story)* | Hạ tầng dữ liệu, không có hành vi người dùng |
| IN-17 | Dockerfile, manifest k8s | *(không có story)* | Hạ tầng/DevOps, không có vai trò nào trong README mục 5 thao tác trực tiếp |
| IN-18 | Docker Compose dev | *(không có story)* | Hạ tầng/DevOps |
| IN-19 | Orthanc + OHIF trong cụm, nạp study mẫu | US-BSCDHA-02, US-BSLS-04 (phần người dùng) | Phần hạ tầng (dựng Orthanc/OHIF, chạy `storescu`) không có story — không phải hành vi của 5 vai trò nghiệp vụ |
| IN-20 | springdoc-openapi (Swagger UI) | *(không có story)* | Công cụ cho dev, không phải màn hình của vai trò nào ở README mục 5 |
| IN-21 | Wireframe AI Panel (Figma) | *(không có story)* | Deliverable thiết kế/Figma — xem `doc/11-ai-design.md` mục 5, không phải hành vi trong hệ thống code được |
| IN-22 | Chương AI trong báo cáo | *(không có story)* | Deliverable tài liệu — xem `doc/11-ai-design.md` |
| IN-23 | Nút "Xem ảnh" (hai cửa vào) | US-BSCDHA-02 (cửa vào a), US-BSLS-04 (cửa vào b) | |
| IN-24 | Kiến trúc phân tầng backend (R-05) | *(không có story)* | Ràng buộc kiến trúc áp dụng xuyên suốt mọi tính năng backend, không phải một màn hình riêng — đúng ví dụ nêu trong `01-scope.md` mục 3.1 |

---

## 5. Đối chiếu với Frontend đang có

Đọc `Frontend/Frontend/README.md` (mục "Cấu trúc", "Chưa làm") và `01-scope.md` mục 9 (bảng thay đổi phạm vi) ngày 2026-11-05.

### 5.1. Story đã có giao diện prototype (dữ liệu giả, chưa nối API, chưa kiểm tra điều kiện ở backend)

| Story | Màn hình prototype tương ứng |
|---|---|
| US-KTV-01, US-KTV-04, US-KTV-05, US-KTV-03 | `AddStudyModal`, `UpdateStudyModal` — **chỉ là giao diện tĩnh**, nút xác nhận hiện chỉ đóng modal, không gửi dữ liệu (`04-his-ris-reconciliation.md` mục 8) |
| US-KTV-07, US-ADMIN-02, US-ADMIN-03 | `UpdateFromHisModal` có giao diện nhưng **chưa có** màn "Danh sách lệch dữ liệu" hay hộp thoại xử lý theo đúng ba lựa chọn `KEEP_RIS/TAKE_HIS/MANUAL_EDIT` |
| US-KTV-06, US-BSCDHA-01, US-BSCDHA-03, US-BSCDHA-04, US-BSDUYET-01, US-BSDUYET-02, US-BSDUYET-03, US-ADMIN-01 | `ReportModal` — các nút chuyển trạng thái đã có, sinh từ `TRANSITIONS` trong `stateMachine.ts`, lọc theo quyền ở `auth.ts`. **Chưa kiểm** điều kiện bắt buộc (trường rỗng, khóa mềm, người duyệt ≠ người đọc) — `03-state-machine.md` mục 2.4 ghi rõ |
| US-KTV-09 | `LoginPage` — có giao diện, 5 tài khoản demo nằm cứng trong `data/mock.ts`, chưa gọi API thật |
| US-KTV-10, US-BSDUYET-04 | `WorklistPage`, `Sidebar` (cây máy, lọc trạng thái) — có giao diện nhưng **khoảng thời gian, loại thời gian lọc và cây máy chưa áp dụng vào lọc dữ liệu thật** (`01-scope.md` mục 9, dòng 2026-10-31) |
| US-BSLS-02, US-BSLS-03 | `ClinicalModal` 5 tab — có giao diện; **tab 3 hiện đang hiển thị dữ liệu giả từ snapshot**, cần đổi sang lấy `service_item.name` + `report.conclusion` thật theo IN-11 đã đổi (`01-scope.md` mục 3.3) |
| US-BSCDHA-05 | `PatientPanel`, `RelatedOrders` — có giao diện đầy đủ |
| US-BSCDHA-06 | Số đếm trên tiêu đề (`DANH SÁCH MÁY - (66|3.397)` kiểu cũ) — có ý tưởng giao diện, chưa nối API đếm thật |

### 5.2. Story chưa có giao diện prototype nào

| Story | Lý do |
|---|---|
| US-BSLS-04, US-BSCDHA-02 (nút "Xem ảnh" + viewer) | README `Frontend/Frontend/README.md` mục "Chưa làm" ghi rõ: "Deep-link sang OHIF Viewer (IN-19, tuần 7)" — chưa làm |
| US-ADMIN-04 (xem audit log) | `Frontend/Frontend/README.md` mục "Chưa làm": "Audit log (IN-15) — backend ghi, frontend chưa có màn xem" |
| US-ADMIN-05 (sửa định danh bệnh nhân với ràng buộc quyền theo trường) | Chưa có màn quản trị người dùng/vai trò (`Frontend/Frontend/README.md` mục "Chưa làm"); sửa bệnh nhân hiện chỉ có form chung, chưa phân biệt trường định danh theo vai trò |
| US-ADMIN-06 (giải phóng khóa mềm) | Frontend hiện **không có khóa mềm** (`03-state-machine.md` mục 2.5 "Chỗ lệch với Frontend") |
| US-KTV-08, US-ADMIN-01 (hủy ca, mở lại ca ở Admin) | `03-state-machine.md` mục 2.5: Frontend chỉ có `study.reopen` (riêng `ADMIN`), chưa có quyền hủy (`study.cancel`) cho ai |

### 5.3. Màn hình Frontend làm thừa so với IN — không viết story

Lấy từ `01-scope.md` mục 9 (bảng thay đổi phạm vi, dòng 2026-10-31). Đây là phần Frontend **có nhưng không thuộc danh mục IN**, nên **không** được viết thành user story (story chỉ viết cho hạng mục IN theo đúng yêu cầu của tài liệu này):

- Bộ lọc "Trạng thái đồng bộ HIS" và "Vi phạm BHYT" (thuộc nhóm HOÃN, `01-scope.md` mục 3.2).
- Nút "Danh sách ca chụp quá hạn" (HOÃN — "quá hạn" chưa định nghĩa).
- Nút "Duyệt toàn bộ danh sách" (H-05); nút "In danh sách", "Tải danh sách" hiện chưa có chức năng.
- Tab lâm sàng 3 và 4 hiển thị dữ liệu giả thay vì bảng rỗng + ghi chú "chờ tích hợp HIS" (riêng tab 3 đã được **đổi phạm vi** sang hiển thị dữ liệu RIS thật — xem US-BSLS-03, không còn là "thừa" sau khi đổi).
- Cột mã đoàn khám, tên đoàn khám, mã hồ sơ, mã bệnh phẩm, mã lưu trữ trong tập cột mặc định (thuộc nhóm HOÃN "bảng phụ").
- Popover "Cấu hình hệ thống" (README mục 3.5) — không thuộc danh mục IN nào.

---

## 6. Điều kiện hoàn thành

- [ ] Nhóm xác nhận 30 story ở mục 3 đúng nghiệp vụ, đổi trạng thái story từ `Draft` sang `Ready` cho những story sắp code.
- [ ] Bảng đối chiếu phủ sóng (mục 4) không còn mã IN nào thiếu lý do.
- [ ] Mục 5.3 (màn hình thừa) được nhóm xác nhận giữ hay cắt — hiện vẫn để ❓ ở `01-scope.md` mục 9.
- [ ] Khi code mỗi tính năng: cập nhật trạng thái story theo mục 2, viết test khớp đúng tiêu chí Given/When/Then.

## 7. Câu hỏi còn mở

1. US-ADMIN-06, US-BSCDHA-04: thời hạn khóa mềm và điều kiện "treo quá lâu" — `03-state-machine.md` mục 2.7 điểm 6, chưa chốt.
2. US-KTV-05, US-ADMIN-05: cơ chế từ chối khi `CLINICIAN` sửa trường định danh/`study_instance_uid` — trả `403` hay chỉ bỏ qua trường — `06-rbac-matrix.md` mục 5, chưa chốt.
3. US-BSLS-04, US-BSCDHA-02: chọn ẩn hay disable nút "Xem ảnh" khi chưa có `study_instance_uid` — `07-pacs-dicom.md` mục 6.2, việc Figma (README mục 6E).
4. US-KTV-07, US-ADMIN-03: hành vi khi xử lý lại một `identity_conflict` đã có `resolution` — chưa định nghĩa ở `04-his-ris-reconciliation.md`.
5. IN-07, IN-12: tiêu chí chấp nhận chi tiết cho phân trang/sắp xếp và lọc "Nội trú/Ngoại trú" — chưa viết riêng, bổ sung khi code (mục 2 quy ước viết tiếp).
6. US-KTV-09: cách lưu kết quả đăng nhập thất bại vào `audit_log` (`LOGIN` không phân biệt thành/bại) — `09-bao-mat-toan-ven-du-lieu.md` mục 2.2, chưa chốt.

## 8. Phát hiện mâu thuẫn và đã sửa

1. **Endpoint cho nút "Xem ảnh" (IN-23) không ghi `VIEW_IMAGE`.** `02-erd.md` mục 4.11 yêu cầu ghi `audit_log` (`action = VIEW_IMAGE`) "tại endpoint sinh deep-link, trước khi trả URL cho trình duyệt". Nhưng đối chiếu `api/openapi.yaml`, endpoint duy nhất trả `study_instance_uid` để dựng deep-link là `GET /studies/{studyId}`, và endpoint này khai `x-audit: READ` — **không phải `VIEW_IMAGE`**. Không có endpoint nào khác trong `api/openapi.yaml` được gắn `x-audit: VIEW_IMAGE`. Đây là khoảng hở giữa đặc tả ERD và API contract hiện tại, **chưa sửa `api/openapi.yaml`** trong lần này (ngoài phạm vi file được giao) — đã ghi vào US-BSCDHA-02, US-BSLS-04 (cột Endpoint có ❓) và báo cho người phụ trách `api/openapi.yaml`.
2. **Không có mâu thuẫn nào khác cần sửa trực tiếp vào các file nguồn** (`01-scope.md`, `02-erd.md`, `03-state-machine.md`, `06-rbac-matrix.md`, `api/openapi.yaml`) trong khi viết tài liệu này — các chỗ chưa chốt đều đã có ❓ sẵn ở file nguồn, chỉ tổng hợp lại ở mục 7 trên, không tự quyết thêm.
