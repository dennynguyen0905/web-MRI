# VR-RIS PACS — Seed data và kịch bản demo

Tài liệu con của `README.md`, mục 6D + 6F ([T4] Dữ liệu giả theo test case + kịch bản demo 10 bước). Trả lời: test case nào cần dữ liệu gì, dữ liệu đó đã có hay cần thêm, và kịch bản demo tuần 8 chạy theo đúng thứ tự nào với dữ liệu nào.

| | |
|---|---|
| **Phiên bản** | 0.1 — NHÁP, chờ nhóm xác nhận |
| **Ngày** | 2026-11-05 |
| **Tác giả** | Agent 2 ("Vận hành"), cùng phiên với `doc/14-devops-trien-khai.md` |
| **Tài liệu liên quan** | `01-scope.md` mục 6 (kịch bản nghiệm thu — **nguồn của luồng chính**, không chép lại) · `03-state-machine.md` (mã T1–T10, SM-01…SM-20 — **nguồn của test case chuyển trạng thái**) · `04-his-ris-reconciliation.md` (RC-01…RC-19 — **nguồn của test case đối chiếu HIS/RIS**) · `05-luong-ngoai-le.md` (4 luồng ngoại lệ) · `06-rbac-matrix.md` (RBAC-01…RBAC-14 — **nguồn của test case phân quyền**) · `07-pacs-dicom.md` mục 7, 9 (tag DICOM, bộ ảnh mẫu) · `08-hl7-message-spec.md` mục 9 (schema JSON `JsonFileHisGateway` — **nguồn của schema his-mock**) · `02-erd.md` (tên bảng/cột) |

> **Quy tắc tránh nhân bản:** tài liệu này **không viết lại** SM-xx, RC-xx, RBAC-xx — chỉ tham chiếu và, khi cần, nói rõ **dữ liệu nào trong seed** để chạy được test case đó. Mục 3.1 là lớp "demo/tích hợp" mới, không phải lớp "đơn vị/cơ chế" đã có ở ba file trên.

---

## 1. Tình trạng trước khi viết tài liệu này

`database/migrations/V7__seed_demo_data.sql` **đã có từ 2026-10-31** — không phải viết từ đầu. V7 đã seed:

- 8 khoa/nhóm, 4 máy, 6 dịch vụ (V1 gốc, không phải V7, nhưng là dữ liệu tham chiếu cho seed).
- 5 vai trò, 5 tài khoản demo (mật khẩu `123456`).
- **8 bệnh nhân giả** (`TEST0001`…`TEST0008`), cố ý đủ 4 trường hợp `dob_precision` (`DAY`/`MONTH`/`YEAR`/ngày sinh `NULL`).
- 8 chỉ định, **9 ca chụp** phủ **6/6 trạng thái chính** (`SCHEDULED`/`UNREAD`/`READING`/`READ`/`APPROVING`/`APPROVED`) kèm đủ `study_status_history`.
- 4 báo cáo (các mức `DRAFT`/`FINAL`/`APPROVED`).
- **1 `identity_conflict`** tái hiện đúng known issue README mục 2.5 (năm sinh RIS `1958` vs HIS `1952`, bệnh nhân `TEST0002`).
- 2 `clinical_snapshot` (tab 1, 2 có dữ liệu; tab 3–5 vẫn trống theo đúng `01-scope.md` mục 3.3).

**Những gì V7 CHƯA có, mà test case ở mục 3 cần** — đây là lý do có `V8__rbac_and_demo_extras.sql` (mới, mục 3.2):

1. Không có cách thử "người duyệt ≠ người đọc" hay khóa mềm giữa hai bác sĩ cùng vai trò — chỉ có 1 tài khoản `RADIOLOGIST`, 1 tài khoản `APPROVER` (`doc/06-rbac-matrix.md` mục 7 đã ghi rõ thiếu này).
2. Không có chỉ định nào có `ordering_doctor_name` khớp đúng `full_name` của tài khoản `CLINICIAN` nào — đề xuất row-level D34 không có dữ liệu để thử.
3. Không có `imaging_order.his_order_id` nào được điền — không thử được `JsonFileHisGateway.findOrder()` (interface tồn tại từ trước, bản thân file JSON mẫu **cũng chưa tồn tại trước tài liệu này** — xem mục 3.2).
4. Không có ca `CANCELLED` — trạng thái này còn ❓ ở `02-erd.md` mục 9 nhưng `03-state-machine.md` đã đặc tả T10, nên cần ít nhất một dữ liệu minh họa.

---

## 2. Tóm tắt những gì tài liệu này tạo mới

| Việc | File | Đã chạy thử? |
|---|---|---|
| Migration bổ sung (4 việc ở mục 1) | `database/migrations/V8__rbac_and_demo_extras.sql` | ✅ Chạy thật qua `docker compose run --rm flyway migrate` trên MySQL 8.4 — xem log ở mục 3.2.4 |
| Mở rộng `HisGateway.HisOrderSnapshot` (3 → 17 field, theo đề xuất `08-hl7-message-spec.md` mục 8.3) | `Backend/src/main/java/com/vrris/pacs/integration/HisGateway.java` | ✅ `mvn verify` xanh (35/35 test) sau khi sửa |
| `JsonFileHisGateway` đọc file JSON thật (trước đây luôn trả rỗng) | `Backend/src/main/java/com/vrris/pacs/integration/impl/JsonFileHisGateway.java` | ✅ Log khởi động xác nhận "nạp 7 bản ghi giả lập" |
| File JSON giả lập HIS (7 bản ghi, đúng schema + đường dẫn `08-hl7-message-spec.md` mục 9) | `Backend/src/main/resources/his-mock/his-orders.json` | ✅ Đọc thành công lúc khởi động backend (local và trong Docker) |
| 22 test case demo/tích hợp | Mục 3.1 tài liệu này | Chưa có code test tự động — đây là bảng đặc tả, chưa viết JUnit/Playwright |
| Bộ tiêu chí chọn ảnh DICOM (chưa tải được bộ thật) | Mục 3.3 | ❓ Chưa chọn bộ cụ thể |
| Kịch bản demo 10 bước | Mục 4 | Chưa diễn tập trên cụm k8s thật (Docker Desktop Kubernetes chưa bật trên máy viết tài liệu — xem `doc/14-devops-trien-khai.md` mục 8.1) |

---

## 3. Test case

### 3.1. Bảng test case

Cột **Nguồn**: trỏ đúng mục đã đặc tả cơ chế (không lặp lại cơ chế đó). Cột **Thuộc IN**: mã `IN-xx` ở `01-scope.md`. Dữ liệu dùng là dữ liệu giả trong `V7`/`V8` trừ khi ghi "nhập tay lúc demo".

| ID | Mục tiêu | Nguồn | Dữ liệu cần có | Kết quả mong đợi | Thuộc IN |
|---|---|---|---|---|---|
| TC-01 | Đăng nhập đủ 7 tài khoản demo (5 vai trò gốc + 2 tài khoản thứ hai) | `01-scope.md` mục 6 bước 2; `06-rbac-matrix.md` mục 2, 7 | `app_user` 7 dòng (V7 + V8): `admin`, `bslamsang`, `ktv`, `bscdha`, `bscdha2`, `bsduyet`, `bsduyet2`, mật khẩu `123456` | Đăng nhập thành công, đúng vai trò hiển thị | IN-13 |
| TC-02 | Tạo bệnh nhân + chỉ định + ca chụp mới (KTV) | `01-scope.md` mục 6 bước 3 | Nhập tay lúc demo — không dùng seed, để chứng minh form thật hoạt động | Ca mới ở tab **Chờ**, trạng thái `SCHEDULED`, có `accession_number` tự sinh | IN-01, IN-02, IN-03 |
| TC-03 | Đẩy ảnh DICOM mẫu vào Orthanc, cập nhật tay `study_instance_uid` + chuyển `SCHEDULED → UNREAD` (T2) | `01-scope.md` mục 6 bước 4; `07-pacs-dicom.md` mục 2.1, 5.1 | Ca tạo ở TC-02 + bộ ảnh DICOM mẫu (mục 3.3, **chưa có**) | `storescu` chạy không lỗi; ca chuyển `UNREAD`; nút "Xem ảnh" xuất hiện | IN-19 (tuần 7) |
| TC-04 | Bác sĩ CĐHA mở ca để đọc (T3), OHIF mở đúng study qua deep-link | `01-scope.md` mục 6 bước 5; `07-pacs-dicom.md` mục 6.1; `03-state-machine.md` SM-01 bước đầu | Ca từ TC-03, `bscdha` | Trạng thái `READING`; OHIF mở đúng `study_instance_uid`; `audit_log` có `VIEW_IMAGE` | IN-23 |
| TC-05 | Viết và lưu báo cáo, chốt `READING → READ` (T5) | `01-scope.md` mục 6 bước 6; SM-01 | Ca từ TC-04 | Trạng thái `READ`; `report.status = FINAL` | IN-10 |
| TC-06 | Bác sĩ duyệt mở ca (T6) rồi duyệt (T7), ca sang tab **Đã thực hiện** | `01-scope.md` mục 6 bước 7; SM-01 | Ca từ TC-05, `bsduyet` | Trạng thái `APPROVED`; `report.status = APPROVED` | IN-06, IN-07 |
| TC-07 | Admin xem audit log, thấy đủ 6 bước TC-02…TC-06 kèm người & thời điểm | `01-scope.md` mục 6 bước 8; `02-erd.md` mục 4.8, 4.11; `09-bao-mat-toan-ven-du-lieu.md` mục 2 | `audit_log` ghi trong lúc demo (không phải seed tĩnh) | Đủ dòng `CREATE`/`STATUS_CHANGE`×5/`VIEW_IMAGE`, đúng `actor_username` | IN-15 |
| TC-08 | Hiển thị một ca lệch dữ liệu HIS ↔ RIS đã tồn tại sẵn (năm sinh) | `01-scope.md` mục 6 bước 9; `04-his-ris-reconciliation.md` RC-02 | `identity_conflict` id tương ứng `patient_id=2` (`TEST0002`) từ V7 | Thấy dòng `PENDING`, `field_name=date_of_birth`, `ris_value=1958`≠`his_value=1952` | IN-05 |
| TC-09 | Tuổi hiển thị đúng cho mọi độ chính xác ngày sinh, không có ca "Tuổi 0" | `01-scope.md` mục 6 bước 10; `02-erd.md` mục 4.1, 4.2 | `TEST0001` (DAY), `TEST0003` (MONTH), `TEST0002`/`TEST0008` (YEAR), `TEST0005` (NULL) | `TEST0001` tuổi chính xác; `TEST0003`/`TEST0002`/`TEST0008` tuổi **xấp xỉ** (`~`); `TEST0005` hiển thị "—", **không bao giờ "0"** | README mục 2.5 |
| TC-10 | Luồng chính đủ 6 bước cho một ca **đã xong từ trước** (không demo sống, dùng để đối chiếu dữ liệu) | `03-state-machine.md` SM-01 | `study.id=1` (V7, đã `APPROVED`) | 6 dòng `study_status_history`, khớp người thực hiện (`performed_by`=KTV, `reading_user`=bscdha, `approving_user`=bsduyet) | IN-06 |
| TC-11 | Chụp lại: bỏ dở ca đang đọc (T4 `READING → UNREAD`), mở lại sau | `05-luong-ngoai-le.md` mục 1 | `study.id=4` (V7, đang `READING`) | Trạng thái về `UNREAD`; `reading_user_id` về `NULL`; báo cáo nháp (nếu có) giữ nguyên | — |
| TC-12 | Hủy chỉ định chưa có ảnh (T10) | `05-luong-ngoai-le.md` mục 2; `03-state-machine.md` T10 | Một trong `study.id ∈ {7, 8, 9}` (V7, `SCHEDULED`, `study_instance_uid IS NULL`) — **hoặc** đối chiếu với `study.id=10` (V8) đã `CANCELLED` sẵn nếu không muốn thực hiện sống | Trạng thái `CANCELLED`; bắt buộc `reason` | — |
| TC-13 | Sai thông tin bệnh nhân: xử lý một `identity_conflict` (`KEEP_RIS`/`TAKE_HIS`/`MANUAL_EDIT`) | `05-luong-ngoai-le.md` mục 3; `04-his-ris-reconciliation.md` RC-01, RC-12 | `identity_conflict` cho `patient_id=3` (`TEST0003`, lệch họ tên, V8) hoặc `patient_id=7` (`TEST0007`, lệch giới tính, V8) | Một trong ba lựa chọn cập nhật đúng `resolution`, ghi `audit_log` | — |
| TC-14 | Mất kết nối HIS khi trả kết quả — **[HOÃN] H-03, chỉ mô tả hành vi mong đợi, không chạy được trong 8 tuần** | `05-luong-ngoai-le.md` mục 4 | Không áp dụng (chưa có Mock HIS thật) | Không có kết quả để kiểm — ghi vào báo cáo đồ án như giới hạn đã biết | H-03 |
| TC-15 | Ca chưa có `study_instance_uid` → nút "Xem ảnh" ẩn/disable, không mở viewer rỗng | `01-scope.md` IN-23; `07-pacs-dicom.md` mục 6.2 (P-10) | `study.id ∈ {7, 8, 9}` (V7, `SCHEDULED`) | Nút ẩn hoặc disable (❓ P-10 chưa chốt cái nào) | IN-23 |
| TC-16 | Mỗi trạng thái (kể cả `CANCELLED` đề xuất) xuất hiện ít nhất một lần trong worklist, bộ đếm theo nhóm đúng | README mục 3.1, 3.2; `02-erd.md` mục 9 | `study.id=7,8,9` (`SCHEDULED`), `5` (`UNREAD`), `6` (`UNREAD`), `4` (`READING`), `2` (`READ`), `3` (`APPROVING`), `1` (`APPROVED`), `10` (`CANCELLED`, V8) | Đếm đúng 7 nhóm trạng thái (nếu Frontend đã thêm `CANCELLED` — hiện **chưa** có trong `Frontend/src/lib/types.ts`, ghi nợ kỹ thuật) | IN-07, IN-09, IN-12 |
| TC-17 | Lọc theo Modality (CT/MR/CR/US) | README mục 3.2; `01-scope.md` IN-12 | 4 máy, 6 dịch vụ (V1) trải đều 4 modality trong seed `study` | Mỗi modality lọc ra đúng số ca tương ứng | IN-12 |
| TC-18 | Lọc theo khoảng thời gian (Hôm nay/Hôm qua/−3/−7/−30/tùy chọn) | README mục 3.2; `01-scope.md` IN-12 | Mọi `study.scheduled_at` trong seed | ⚠️ **Rủi ro đã biết** (mục 6) — xem cảnh báo dưới | IN-12 |
| TC-19 | Lọc theo loại bệnh nhân (Nội trú/Ngoại trú) | README mục 3.2; IN-12 | `imaging_order.patient_class` seed có cả `INPATIENT` (đơn 2, 3) và `OUTPATIENT` (còn lại) | Lọc đúng theo từng loại | IN-12 |
| TC-20 | (Dữ liệu sẵn sàng, **chưa có code chạy**) `CLINICIAN` chỉ thấy chỉ định do mình chỉ định — đề xuất D34 | `06-rbac-matrix.md` mục 5, 7 | `imaging_order.id=4` có `ordering_doctor_name = 'BS. Lâm Sàng (demo)'` khớp `app_user.full_name` của `bslamsang` (V8) | ❓ Chưa kiểm được — backend chưa lọc theo quy tắc này. Dữ liệu đã đúng, chờ code | — |
| TC-21 | `JsonFileHisGateway.findOrder()` trả đúng snapshot cho 7 trường hợp đối chiếu khác nhau | `08-hl7-message-spec.md` mục 9; `04-his-ris-reconciliation.md` RC-01, RC-02, RC-03/RC-07, RC-12 | `his-mock/his-orders.json` (mục 3.2.3) | Khớp hoàn toàn (`HISO...0007`, `...0014`); lệch ngày sinh (`...0008`); lệch họ tên (`...0011`); lệch giới tính (`...0013`); bệnh nhân mới (`...0009`); trùng BHXH khác người (`...0012`) | — |
| TC-22 | (Dữ liệu sẵn sàng, **chưa có code chạy**) Khóa mềm + người duyệt ≠ người đọc, dùng tài khoản thứ hai | `06-rbac-matrix.md` mục 7, RBAC-09/10 | `bscdha2` (`RADIOLOGIST`), `bsduyet2` (`APPROVER`) từ V8 | ❓ Chưa kiểm được — backend chưa có khóa mềm/kiểm tra người duyệt≠người đọc. Tài khoản đã sẵn sàng | — |

### 3.2. Seed data — mô tả

#### 3.2.1. Bảng dữ liệu (V7 có gì, V8 thêm gì)

| Bảng | V7 (đã có từ 2026-10-31) | V8 (mới, tài liệu này) | TC liên quan |
|---|---|---|---|
| `department`, `service_item`, `device` | 8 + 6 + 4 dòng (thuộc `V1__ref_data.sql`, không phải V7 — ghi lại để đủ bối cảnh) | Không đổi | TC-17 |
| `role`, `app_user`, `user_role` | 5 vai trò, 5 tài khoản | **+2 tài khoản** (`bscdha2` `RADIOLOGIST`, `bsduyet2` `APPROVER`) | TC-01, TC-20, TC-22 |
| `patient` | 8 bệnh nhân (`TEST0001`…`TEST0008`), đủ 4 trường hợp `dob_precision` | Không đổi (TC-21 dùng `TEST0009`/`TEST0010` — **không** có dòng `patient` tương ứng, vì đó là "bệnh nhân chưa có trong RIS", đúng bản chất test case) | TC-09, TC-21 |
| `imaging_order` | 8 chỉ định | **`ordering_doctor_name`** của đơn `id=4` đổi khớp `bslamsang`; **`his_order_id`** điền cho 5 đơn (`id=1,2,3,7,8`) | TC-20, TC-21 |
| `study` | 9 ca, phủ 6 trạng thái chính | **+1 ca `CANCELLED`** (`id=10`, thuộc đơn `id=8`) | TC-12, TC-16 |
| `study_status_history` | Đủ lịch sử cho 9 ca | +2 dòng cho ca `id=10` | TC-12 |
| `report` | 4 báo cáo | Không đổi | TC-05, TC-10 |
| `identity_conflict` | 1 dòng (`TEST0002`, lệch ngày sinh) | **+2 dòng** (`TEST0003` lệch họ tên, `TEST0007` lệch giới tính) | TC-08, TC-13 |
| `clinical_snapshot` | 2 dòng (đơn `id=1,2`) | Không đổi | — |

Tổng sau V8: **9 tài khoản**, **8 bệnh nhân RIS** (+2 "bệnh nhân HIS" chỉ tồn tại trong `his-orders.json`, chưa vào RIS), **8 chỉ định**, **10 ca chụp** (7 trạng thái), **3 `identity_conflict`**.

#### 3.2.2. Vì sao không thêm tiền tố "TEST" riêng

Đã hỏi lại cách đặt mã trong lúc làm — **nhóm không yêu cầu tiền tố riêng** (khác với giả định ban đầu của người giao việc). Mã bệnh nhân mới (`TEST0009`, `TEST0010` — chỉ xuất hiện trong `his-orders.json`, không trong `patient`) tiếp tục đúng dãy số đã có ở V7 (`TEST0001`…`TEST0008`), không đặt tiền tố khác.

#### 3.2.3. File JSON cho `JsonFileHisGateway`

`Backend/src/main/resources/his-mock/his-orders.json` — đúng đường dẫn và schema đề xuất ở `08-hl7-message-spec.md` mục 9 (17 field, theo đề xuất mục 8.3 của file đó — đã áp dụng vào code, xem mục 2). **7 bản ghi**, 3 bản ghi đầu do Agent 1 đặc tả sẵn trong `08-…`, 4 bản ghi sau do tài liệu này thêm để phủ các trường hợp đối chiếu còn thiếu:

| `hisOrderId` | `patientCode` | Trường hợp minh họa | Khớp `imaging_order.his_order_id` |
|---|---|---|---|
| `HISO2026110500007` | `TEST0001` | Khớp hoàn toàn | `id=1` |
| `HISO2026110500008` | `TEST0002` | Lệch ngày sinh (`1952` HIS vs `1958` RIS) — **chính known issue README 2.5** | `id=2` |
| `HISO2026110500009` | `TEST0009` | Bệnh nhân mới (case B, `04-…` mục 1.1) — không có trong `patient` | *(không gắn — đúng bản chất)* |
| `HISO2026110500011` | `TEST0003` | Lệch họ tên (kiểu RC-01) | `id=3` |
| `HISO2026110500012` | `TEST0010` | Trùng BHXH với `TEST0004` nhưng là người khác (case C, kiểu RC-07) | *(không gắn — đúng bản chất)* |
| `HISO2026110500013` | `TEST0007` | Lệch giới tính (kiểu RC-12) | `id=8` |
| `HISO2026110500014` | `TEST0006` | Khớp hoàn toàn (ca thứ hai, dịch vụ khác) | `id=7` |

⚠️ **Giới hạn đã biết:** `HisOrderSnapshot` không có `his_patient_id` (field dùng để phát hiện case D ở `04-his-ris-reconciliation.md` mục 1.1 — "trùng mã khác người") — không dựng được test case cho trường hợp đó qua gateway này. Cũng không dựng được RC-16/RC-17 (nhận lại message trùng `ORC-2`) vì gateway là tra cứu điểm (`findOrder(id)`), không phải luồng nhận message — các RC-xx thuộc nhóm "sau khi đã khớp khóa" (RC-09…RC-11, RC-13…RC-15, RC-19) cũng không cần đi qua gateway, chỉ cần dữ liệu `patient`/`identity_conflict` đã có.

⚠️ **Thay đổi kỹ thuật kèm theo (đã làm, không chỉ tài liệu):** `JsonFileHisGateway` trước đây luôn trả `Optional.empty()` (chưa đọc file nào). Đã sửa để đọc thật `his-orders.json` bằng một `ObjectMapper` **riêng** (có đăng ký `JavaTimeModule` cho `LocalDate`/`LocalDateTime`), không dùng `ObjectMapper` mặc định của Spring — vì bean đó cấu hình `property-naming-strategy: SNAKE_CASE` cho JSON của OpenAPI (`application.yml`), còn `his-orders.json` dùng camelCase đúng như ví dụ ở `08-…` mục 9.2. Dùng chung sẽ đọc ra toàn `null`. Đã log xác nhận "nạp 7 bản ghi" lúc khởi động (local và trong Docker image).

#### 3.2.4. Chạy thử migration

```text
$ cd database && docker compose run --rm flyway info
...
| Versioned | 8       | rbac and demo extras | SQL  |                     | Pending | No       |

$ docker compose run --rm flyway migrate
...
Migrating schema `vr_ris` to version "8 - rbac and demo extras"
Successfully applied 1 migration to schema `vr_ris`, now at version v8
```

✅ **Đã chạy thật** trên MySQL 8.4 (container `vr-ris-mysql` đang dùng cho vòng dev) trong phiên làm việc này — không phải suy luận. Cũng đã chạy lại từ đầu (`docker compose down -v && up -d`) để xác nhận toàn bộ `V1`…`V8` áp được liên tục trên schema rỗng — khớp đúng kịch bản k8s sẽ gặp (`doc/14-devops-trien-khai.md` mục 5.3).

---

## 3.3. Ảnh DICOM — đang chờ, chưa chọn bộ cụ thể

Nhóm có bộ ảnh riêng nhưng **chưa tải về được** tại thời điểm viết tài liệu này — không đoán, không chọn bộ thay. Dưới đây là tiêu chí để nhóm tự chọn, và việc phải làm **sau khi** có ảnh.

### 3.3.1. Tiêu chí chọn bộ ảnh

| Tiêu chí | Yêu cầu |
|---|---|
| Ẩn danh | Đã ẩn danh sẵn (ví dụ bộ công khai từ TCIA — The Cancer Imaging Archive, hoặc tương đương) — nguyên tắc bất biến README mục 0, R-03 |
| Modality | ❓ **Chưa được nhóm xác nhận.** Đề xuất của `01-scope.md` mục 8 (câu 9) là **CR + CT** — CR nhẹ, mở nhanh để kiểm chứng đường ảnh chạy được; CT có nhiều lát để kiểm chứng viewer cuộn được series lớn |
| Số study tối thiểu | 1–2 study (đủ cho IN-19 — không cần nhiều hơn) |
| Dung lượng | Đủ nhỏ để tải qua mạng thường và đẩy vào Orthanc trong vài phút — một study CT vài trăm lát có thể vài trăm MB, nên ưu tiên study CT **ít lát** nếu có lựa chọn |
| Giấy phép dùng lại | Phải cho phép dùng lại trong mục đích giáo dục/phi thương mại (hầu hết bộ TCIA ở dạng CC BY hoặc tương đương — kiểm tra giấy phép cụ thể của từng collection trước khi dùng) |
| Mở được trong Orthanc/OHIF | Cú pháp DICOM chuẩn (transfer syntax phổ biến, ví dụ Explicit VR Little Endian) — tránh bộ dùng transfer syntax nén lạ cần plugin transcode riêng |

### 3.3.2. Tag DICOM phải sửa trước khi nạp

Danh sách đầy đủ lý do + mapping: **`07-pacs-dicom.md` mục 7** (không chép lại). Tóm tắt các tag **phải đổi** để khớp bệnh nhân giả trong RIS trước khi `storescu`:

| Tag | Tên | Đổi thành (ví dụ, khớp `TEST0001`) |
|---|---|---|
| `(0010,0020)` | `PatientID` | `TEST0001` |
| `(0010,0010)` | `PatientName` | `Nguyen^Van^Gia A` (xem ❓ quy tắc tách ở `07-pacs-dicom.md` mục 4.1 — chưa chốt, dùng tạm dạng "để cả vào thành phần đầu") |
| `(0010,0030)` | `PatientBirthDate` | `19580315` (nếu dùng `TEST0001`, `dob_precision=DAY`) — ❓ xem `07-pacs-dicom.md` mục 4.2 cho ca `dob_precision` khác `DAY` |
| `(0010,0040)` | `PatientSex` | `M` |
| `(0008,0050)` | `AccessionNumber` | **Phải khớp đúng** `study.accession_number` của ca đã tạo trong RIS (ví dụ `2026102900002`) — đây là khóa nối, sai tag này thì nút "Xem ảnh" vẫn mở được (dựa vào `study_instance_uid` nhập tay) nhưng mất tính minh họa "khóa nối ba chiều" |

### 3.3.3. Lệnh mẫu sửa tag (chưa chạy thử — chưa có file ảnh để thử)

❓ **Chưa kiểm chứng** — chưa có bộ ảnh thật để chạy lệnh này. Công cụ sửa tag DICOM phổ biến là `dcmodify` (bộ **DCMTK**, khác `dcm4che` — `dcm4che` ở `07-pacs-dicom.md` chỉ dùng cho thao tác mạng `storescu`/`findscu`/`echoscu`; sửa tag trong file là một việc khác, cần tool khác):

```bash
# Vi du: sua 4 tag cho mot file DICOM truoc khi day vao Orthanc (cu phap DCMTK dcmodify).
dcmodify \
  -i "PatientID=TEST0001" \
  -i "PatientName=Nguyen^Van^Gia A" \
  -i "PatientBirthDate=19580315" \
  -i "PatientSex=M" \
  -i "AccessionNumber=2026102900002" \
  duong/dan/den/file.dcm
```

Sau khi sửa tag, dùng đúng lệnh `storescu` đã có ở `07-pacs-dicom.md` mục 5.1 để đẩy vào Orthanc (không chép lại ở đây).

### 3.3.4. Chỗ đặt ảnh

**Không commit vào git** (ảnh DICOM nặng, không thuộc mã nguồn). Đề xuất: thư mục riêng ngoài repo (ví dụ `~/vr-ris-dicom-samples/`), hoặc nếu cần chia sẻ trong nhóm thì dùng Google Drive/tương đương — **không đưa link hay ảnh thật vào bất kỳ file nào trong repo**.

### 3.3.5. ❓ Câu hỏi còn mở (bắt buộc chốt trước tuần 7)

Modality demo cuối cùng (CR + CT, hay khác) — đề xuất ở `01-scope.md` mục 8 câu 9 **vẫn chưa được nhóm xác nhận** tính đến ngày viết tài liệu này. Hạn chốt: muộn nhất tuần 4–5 (đúng như `01-scope.md` đã ghi) — tài liệu này không đẩy hạn, chỉ nhắc lại.

---

## 4. Kịch bản demo 10 bước

Viết lại từ đầu theo đúng yêu cầu (không chép `01-scope.md` mục 6), **cùng luồng chính, thêm chi tiết thao tác/dữ liệu/lệnh** — không mâu thuẫn với file đó.

| # | Vai trò đăng nhập | Thao tác | Dữ liệu dùng | Lệnh chạy kèm | Kết quả mong đợi trên màn hình | Trạng thái sau | TC |
|---|---|---|---|---|---|---|---|
| 1 | — | Triển khai cụm | — | `kubectl apply -k deploy/k8s` (`doc/14-devops-trien-khai.md` mục 8.2) | Mọi pod `Running`/`Ready`; mở `http://localhost:8080` thấy trang đăng nhập | — | — |
| 2 | `ktv` | Đăng nhập, tạo bệnh nhân + chỉ định + ca chụp mới | Nhập tay (ví dụ bệnh nhân "Demo Giả K", dịch vụ `CT-SN`, máy `CT-01`) | — | Ca mới ở tab **Chờ**, trạng thái "Chờ thực hiện", có mã phiếu tự sinh | `SCHEDULED` | TC-02 |
| 3 | `ktv` (thao tác trên máy host) | Đẩy ảnh DICOM mẫu vào Orthanc; nhập tay `study_instance_uid` vào ca; chuyển sang "Chưa đọc" | Bộ ảnh mẫu (mục 3.3 — **chưa có**) | `storescu -c VRRIS_PACS@localhost:4242 --bind SIM_CT01 <thư-mục-study>` (`07-pacs-dicom.md` mục 5.1) | Không có lỗi kết nối DICOM; ca chuyển tab **Đã thực hiện**, trạng thái "Chưa đọc", nút "Xem ảnh" xuất hiện | `UNREAD` | TC-03, TC-15 |
| 4 | `bscdha` | Đăng nhập, mở ca vừa tạo | Ca từ bước 3 | — | Trạng thái "Đang đọc"; bấm "Xem ảnh" mở đúng OHIF, đúng study | `READING` | TC-04 |
| 5 | `bscdha` | Nhập kỹ thuật + kết luận, lưu báo cáo | — | — | Trạng thái "Đọc xong" | `READ` | TC-05 |
| 6 | `bsduyet` | Đăng nhập, mở ca để duyệt, duyệt | Ca từ bước 5 | — | Trạng thái "Duyệt xong"; ca chuyển hẳn sang tab **Đã thực hiện** (không còn ở nhóm "đang xử lý") | `APPROVED` | TC-06 |
| 7 | `admin` | Mở màn audit log | — | — | Thấy đủ dòng ghi lại các bước 2, 3 (nếu RIS ghi lúc nhập `study_instance_uid`), 4 (`VIEW_IMAGE`), 5, 6 | — | TC-07 |
| 8 | `admin` (hoặc `ktv`) | Mở "Danh sách lệch dữ liệu" (nếu đã có màn này — ❓ xem cảnh báo dưới), xem ca `TEST0002` | `identity_conflict` có sẵn từ V7 | — | Thấy dòng `PENDING`, năm sinh RIS `1958` ≠ HIS `1952`, **không** tự động ghi đè | — | TC-08 |
| 9 | Bất kỳ (xem worklist) | Kiểm tra cột Tuổi cho `TEST0001`, `TEST0002`, `TEST0003`, `TEST0005` | 4 bệnh nhân seed, đủ 4 kiểu `dob_precision`/NULL | — | Tuổi chính xác / tuổi xấp xỉ (`~`) / "—" — **không ca nào ra "0"** | — | TC-09 |
| 10 | `ktv` | Hủy một ca chưa có ảnh | `study.id=7` (hoặc 8, 9 — seed sẵn, `SCHEDULED`, chưa có ảnh) | — | Ca chuyển "Đã hủy" (đề xuất `CANCELLED` — ❓ Frontend/README hiện chưa có trạng thái này, xem mục 6); yêu cầu nhập lý do | `CANCELLED` | TC-12 |

⚠️ **Bước 8 cảnh báo:** `01-scope.md` mục 11 đã ghi "Frontend chưa có… màn danh sách lệch dữ liệu HIS ↔ RIS" — nếu đến tuần 8 màn này vẫn chưa code, bước 8 phải đổi sang "xem trực tiếp trong DB qua DBeaver" làm phương án thay thế khi demo (không phải lỗi của kịch bản, lỗi của tiến độ code — ghi vào nợ kỹ thuật nếu xảy ra).

### 4.1. Thời lượng ước tính

| Nhóm bước | Thời gian |
|---|---|
| 1 (triển khai, chờ pod Ready) | 2–5 phút (tùy máy, lần đầu kéo image Orthanc/OHIF chậm hơn các lần sau) |
| 2–3 (tạo ca, đẩy ảnh) | 3–5 phút |
| 4–6 (đọc, duyệt) | 5–8 phút (phụ thuộc người demo gõ báo cáo nhanh hay chậm) |
| 7–10 (audit, đối chiếu, tuổi, hủy) | 5 phút |
| **Tổng** | **~20 phút** — đủ cho một buổi nghiệm thu nội bộ, còn dư thời gian hỏi-đáp |

### 4.2. Điều kiện chuẩn bị trước demo

- Cụm k8s đã `kubectl apply -k deploy/k8s` **trước** buổi demo ít nhất 10 phút (để image Orthanc/OHIF kéo xong nếu lần đầu chạy trên máy đó).
- Bộ ảnh DICOM mẫu đã tải, đã sửa tag (mục 3.3), đã kiểm thử `storescu`/`echoscu` một lần **ngoài buổi demo** — README mục 7.4 đã ghi rủi ro "ảnh DICOM mẫu không tương thích Orthanc/OHIF", cách giảm là thử trước tuần 4–5, không phải chờ tới lúc demo.
- Máy demo đã đóng các tiến trình chiếm cổng `8080`/`4242`/`8042`/`8043`/`3307` (`doc/14-devops-trien-khai.md` mục 8.4).

### 4.3. Cách reset để demo lại từ đầu

```bash
kubectl delete -k deploy/k8s     # xóa toàn bộ, kể cả PVC (mất ca vừa tạo ở bước 2 — chủ ý)
kubectl apply -k deploy/k8s      # dựng lại từ seed V1-V8 sạch
```

Ca tạo ở bước 2 **không phải seed** (nhập tay) nên sau khi reset, bước 2 lại tạo đúng một ca mới — demo lặp lại được nhiều lần không cần sửa gì thêm.

### 4.4. Phương án dự phòng cho hai rủi ro (README mục 7.4)

| Rủi ro | Phương án dự phòng |
|---|---|
| **Ảnh DICOM mẫu không tương thích Orthanc/OHIF** | Có sẵn 1–2 ảnh chụp màn hình OHIF đã mở thành công (chụp lúc diễn tập trước) để chiếu thay nếu `storescu` lỗi ngay tại chỗ — bước 3–4 chuyển thành "đã làm trước, đây là kết quả" + giải thích miệng. Không dừng demo để debug DICOM trước người chấm |
| **Kubernetes trục trặc** (`kubectl apply` lỗi, pod không lên) | Phương án hai lớp: (a) `docker compose -f deploy/compose/docker-compose.full.yml up -d` (chạy cùng image, không cần k8s — `doc/14-devops-trien-khai.md` mục 1); (b) nếu cả Docker cũng trục trặc, chạy trực tiếp `mvn spring-boot:run` (Backend) + `npm run dev` (Frontend) + `database/docker-compose.yml` (chỉ MySQL) — mất phần Orthanc/OHIF (bỏ qua bước 3–4, demo từ bước 2 rồi nhảy thẳng sang nhập báo cáo tay cho một ca seed sẵn đã có ảnh "giả định") |

### 4.5. Bước nào làm tay vì phần tự động là H-01/H-02

**Nói thật, không giấu** (đúng yêu cầu của người giao việc):

- **Bước 3** (nhập tay `study_instance_uid` + tự chuyển "Chưa đọc") **là thao tác tay thay cho H-02** (PACS tự báo RIS khi nhận đủ ảnh) — README mục 2.3 đã ghi rõ đây là giả định tạm, không phải luồng đích.
- **Không có bước nào** mô phỏng H-01 (RIS đẩy Modality Worklist cho máy chụp) — máy giả lập (`storescu`) không lấy thông tin bệnh nhân từ RIS qua MWL, mà dùng đúng tag đã sửa tay trước (mục 3.3.3). Đây là lý do `AccessionNumber` trong ảnh phải được gõ **khớp tay** với RIS, không phải máy tự lấy.
- **Không có bước nào** mô phỏng H-03 (gửi/nhận HL7 thật qua MLLP) — TC-14 (mục 3.1) đã ghi đây là giới hạn đã biết, không chạy được trong 8 tuần.

---

## 5. Sơ đồ

Không cần sơ đồ mới cho tài liệu này (không có luồng mới ngoài những gì `03-state-machine.md`, `04-his-ris-reconciliation.md`, `05-luong-ngoai-le.md` đã vẽ) — kịch bản demo ở mục 4 là một **bảng các bước**, không phải một luồng cần biểu đồ riêng.

---

## 6. Câu hỏi còn mở

| # | Câu hỏi | Mục | Chặn việc gì |
|---|---|---|---|
| D12-01 | Modality demo cuối cùng (CR+CT hay khác) — kế thừa từ `01-scope.md` mục 8 câu 9, **vẫn chưa chốt** | 3.3.5 | Chọn + tải bộ ảnh DICOM mẫu, muộn nhất tuần 4–5 |
| D12-02 | ⚠️ **Rủi ro mới phát hiện khi viết tài liệu này:** mọi `scheduled_at`/`ordered_at`/`detected_at` trong `V7`/`V8` là ngày cố định (29–31/10/2026, 05/11/2026). Demo thật diễn ra tuần 8 (~đầu/giữa tháng 12/2026 theo lịch README mục 7.1) — bộ lọc "Hôm nay"/"Hôm qua" (TC-18) **sẽ ra rỗng** vì không có `study` nào có `scheduled_at` đúng ngày demo. Hai hướng: (a) viết thêm một migration `V9` ngay trước buổi demo để seed vài ca có ngày = ngày demo thật (không làm trong tài liệu này vì chưa biết ngày demo chính xác); (b) chấp nhận bộ lọc "Hôm nay" trống trong demo, chỉ minh họa "−30 ngày"/"Tất cả". **Đề xuất (a)**, nhưng cần nhóm xác nhận ngày demo trước khi viết `V9` | 3.1 (TC-18), 4 | Demo tuần 8 — phải xử lý **trước** buổi demo, không phải việc của tài liệu này vì chưa biết ngày chính xác |
| D12-03 | "Danh sách lệch dữ liệu" (bước 8 mục 4) — Frontend có kịp code trước tuần 8 không | 4 | Kịch bản demo — đã có phương án thay thế (xem cảnh báo dưới bảng mục 4) |
| D12-04 | Có cần thêm migration `V9` để minh họa `CLINICIAN` xem được đúng chỉ định của mình (TC-20) **sau khi** backend code xong đề xuất D34, hay seed hiện tại (V8) đã đủ | 3.2.1 | Không chặn 8 tuần — chỉ chặn việc *chứng minh* D34 chạy được, không chặn việc code D34 |
| D12-05 | Giữ nguyên đề xuất "không tiền tố TEST riêng" (mục 3.2.2) hay nhóm muốn đổi | 3.2.2 | Không chặn — chỉ là quy ước đặt mã |

---

## 7. Phát hiện mâu thuẫn và đã sửa

1. **`doc/08-hl7-message-spec.md` mục 8.2/8.3 mô tả `HisOrderSnapshot` chỉ 3 field và `JsonFileHisGateway` "chưa đọc file JSON nào"** — đây không phải mâu thuẫn mà là trạng thái *trước khi* tài liệu này thực thi đúng việc được giao ở đó ("Agent 2 sinh file JSON thật"). Đã cập nhật trực tiếp `doc/08-hl7-message-spec.md` (mục 8.2 thêm khung ghi chú, mục 12/14 đánh dấu hoàn thành) để không ai đọc nhầm là bị bỏ sót — xem mục 11 của file đó.
2. **Không có mâu thuẫn nào tìm thấy** giữa dữ liệu `V7`/`V8` và `03-state-machine.md`/`04-his-ris-reconciliation.md`/`06-rbac-matrix.md` — mọi bản ghi mới ở `V8` đều dùng đúng mã trạng thái, mã vai trò, và cấu trúc cột đã chốt ở các file đó, không đề xuất giá trị mới nào ngoài việc dùng lại.
3. **Phát hiện (không phải mâu thuẫn, là rủi ro mới):** xem **D12-02** ở mục 6 — ngày trong seed sẽ "cũ" vào lúc demo thật diễn ra. Ghi lại thành câu hỏi mở vì không tự quyết được ngày demo chính xác.
4. **`01-scope.md` mục 11 đã ghi Frontend thiếu "màn danh sách lệch dữ liệu HIS ↔ RIS"** — không phải mâu thuẫn, là xác nhận lại rằng bước 8 trong kịch bản demo (mục 4) cần phương án dự phòng, đã thêm cảnh báo ngay dưới bảng.

---

## 8. Điều kiện hoàn thành

Nối với `01-scope.md` mục 6 (kịch bản nghiệm thu) và README mục 7.2 (tuần 8: *"Demo end-to-end từ một lệnh deploy, dữ liệu giả"*):

- [x] Migration `V8` viết xong, **chạy thật thành công** trên MySQL 8.4 (mục 3.2.4).
- [x] File `his-orders.json` sinh xong theo đúng schema `doc/08-hl7-message-spec.md` mục 9, **đọc thành công thật** qua `JsonFileHisGateway` đã sửa code.
- [x] 22 test case demo/tích hợp đặc tả xong (mục 3.1), tham chiếu đúng SM-xx/RC-xx/RBAC-xx, không chép lại.
- [x] Kịch bản demo 10 bước viết xong, đúng luồng chính, không mâu thuẫn `01-scope.md` mục 6.
- [ ] Bộ ảnh DICOM mẫu — **chưa chọn, chưa tải** (mục 3.3.5, D12-01) — chặn TC-03, TC-04, TC-15, và bước 3–4 của kịch bản demo.
- [ ] Diễn tập kịch bản demo **trên cụm k8s thật** ít nhất một lần trước tuần 8 — chưa làm (máy viết tài liệu này chưa bật Docker Desktop Kubernetes, xem `doc/14-devops-trien-khai.md` mục 8.1).
- [ ] Quyết định D12-02 (seed ngày demo thật) trước buổi demo.
- [ ] Code các đề xuất chưa chạy được (`row-level D34` cho TC-20, khóa mềm/người duyệt≠người đọc cho TC-22, màn "danh sách lệch dữ liệu" cho bước 8) — thuộc phạm vi code backend/frontend, không phải tài liệu này.
