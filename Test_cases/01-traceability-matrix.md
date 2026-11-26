# Ma trận truy vết — yêu cầu → test → trạng thái

Mục đích: trả lời được câu "yêu cầu X đã ai kiểm chưa, bằng gì" mà không phải đọc code.

Ký hiệu trạng thái:
`✅ tự động` test tự động đã phủ · `🖐 thủ công` chỉ kiểm tay được · `⬜ chưa phủ` ·
`🚫 bị chặn` không chạy được, có lý do · `🐞 có lỗi` đã phát hiện lỗi, xem `reports/bug-report.md`

---

## 1. Ma trận 22 test case `TC-01`…`TC-22` (`doc/12` mục 3.1)

| TC | Mục tiêu | Dữ liệu | Phủ bởi | Trạng thái |
|---|---|---|---|---|
| TC-01 | Đăng nhập đủ 7 tài khoản demo | 7 `app_user`, mật khẩu demo | `e2e/01-auth.spec.ts` | ✅ tự động |
| TC-02 | Tạo bệnh nhân + chỉ định + ca chụp (KTV) | nhập tay | `e2e/04-create-study.spec.ts` | ✅ tự động |
| TC-03 | Đẩy DICOM vào Orthanc + T2 | ảnh DICOM mẫu | — | 🚫 bị chặn — `D12-01`: bộ ảnh mẫu chưa có (`doc/12` mục 3.3, 8) |
| TC-04 | Mở ca để đọc (T3), OHIF deep-link | ca từ TC-03 | phần T3 ở `e2e/03-transitions.spec.ts` | 🚫 phần OHIF bị chặn bởi `D12-01`; phần T3 ✅ tự động |
| TC-05 | Viết + chốt báo cáo (T5) | ca từ TC-04 | `e2e/03-transitions.spec.ts`, `Frontend/.../reportSteps.test.ts` | ✅ tự động |
| TC-06 | Duyệt (T6 → T7) | `bsduyet` | `e2e/03-transitions.spec.ts`, `Frontend/.../studiesApi.test.ts` | ✅ tự động |
| TC-07 | Admin xem audit log đủ 6 bước | audit ghi khi demo | — | ⬜ chưa phủ — frontend **chưa có màn xem audit log** (`doc/13` mục 5.2) |
| TC-08 | Hiển thị ca lệch dữ liệu HIS↔RIS | `identity_conflict` `patient_id=2` | — | ⬜ chưa phủ — frontend **chưa có màn "Danh sách lệch dữ liệu"** (`doc/01-scope` mục 11) |
| TC-09 | Tuổi đúng theo `dob_precision`, không bao giờ "0" | `TEST0001/0002/0003/0005/0008` | `unit/format.spec.ts` (13 case), `e2e/02-age-display.spec.ts` | ✅ tự động |
| TC-10 | Luồng chính 6 bước cho ca đã xong | `study.id=1` | `e2e/03-transitions.spec.ts` | 🖐 thủ công (phần đối chiếu `study_status_history` cần DB) |
| TC-11 | Bỏ dở ca đang đọc (T4), nháp giữ nguyên | `study.id=4` | `e2e/03-transitions.spec.ts`, `Frontend/.../studiesApi.test.ts` | ✅ tự động |
| TC-12 | Hủy chỉ định chưa có ảnh (T10) | `study.id ∈ {7,8,9}` | `e2e/03-transitions.spec.ts`, `unit/stateMachine.spec.ts` | ✅ tự động |
| TC-13 | Xử lý `identity_conflict` 3 lựa chọn | `patient_id=3` hoặc `7` | — | ⬜ chưa phủ — chưa có UI (xem TC-08) |
| TC-14 | Mất kết nối HIS khi trả kết quả | — | — | 🚫 bị chặn — tài liệu tự ghi `[HOÃN] H-03`, không chạy được trong 8 tuần |
| TC-15 | Ca chưa có UID → nút "Xem ảnh" ẩn/disable | `study.id ∈ {7,8,9}` | `e2e/05-view-image-gate.spec.ts` | ✅ tự động — **nhưng** ❓ `P-10` chưa chốt là ẩn HAY disable; test khóa hành vi code đang chạy |
| TC-16 | Đếm đủ 7 nhóm trạng thái, kể cả `CANCELLED` | `study.id=1..10` | `unit/status.spec.ts` (10 case), `e2e/06-status-counts.spec.ts` | ✅ 6/7 trạng thái — **lỗi `DATA-01`**: mock seed thiếu ca `CANCELLED`, 6 test bị skip. **nợ tài liệu `N-01`**: `doc/12` ghi TC-16 bị chặn vì frontend thiếu `CANCELLED`; điều này **đã lỗi thời** từ 2026-11-12 |
| TC-17 | Lọc theo Modality (CT/MR/CR/US) | 4 máy, 6 dịch vụ | — | ⬜ chưa phủ — cần viết `e2e/07-filters.spec.ts` |
| TC-18 | Lọc theo khoảng thời gian | mọi `scheduled_at` | — | 🖐 thủ công — rủi ro `D12-02`: seed dùng ngày cố định ⇒ "Hôm nay"/"Hôm qua" **sẽ rỗng**, không phải lỗi code |
| TC-19 | Lọc theo loại bệnh nhân (Nội/Ngoại trú) | `patient_class` | — | ⬜ chưa phủ — cần viết `e2e/07-filters.spec.ts` |
| TC-20 | `CLINICIAN` chỉ thấy chỉ định của mình (D34) | `imaging_order.id=4` | — | 🚫 bị chặn — backend chưa lọc row-level cho các `GET` còn lại; thêm `D10`: seed không khớp tên |
| TC-21 | `JsonFileHisGateway.findOrder()` 7 trường hợp | `his-orders.json` | backend, ngoài phạm vi | — ngoài phạm vi (không phải frontend) |
| TC-22 | Khóa mềm + người duyệt ≠ người đọc | `bscdha2`, `bsduyet2` | `Frontend/.../studiesApi.test.ts`, backend `RbacIntegrationTest` RBAC-09/10 | ✅ tự động — **nợ tài liệu `N-02`**: `doc/12` ghi "chưa có code chạy", **đã lỗi thời** |

**Tổng kết TC (22):** ✅ tự động 10 · 🖐 thủ công 2 · ⬜ chưa phủ 5 · 🚫 bị chặn 4 · ngoài phạm vi 1

---

## 2. Ma trận 15 test case RBAC (`doc/06` mục 8)

Backend đã phủ **1-1** trong `Backend/src/test/java/com/vrris/pacs/rest/RbacIntegrationTest.java` (20 test).
Cột dưới đây theo dõi phần **tầng giao diện** — thứ backend không kiểm được.

| Mã | Tình huống | Mã mong đợi | Backend | Frontend (UI) |
|---|---|---|---|---|
| RBAC-01 | Chưa đăng nhập gọi `GET /studies` | 401 | ✅ | ✅ `e2e/01-auth.spec.ts` (chuyển về màn đăng nhập) |
| RBAC-02 | Token hết hạn / sai chữ ký | 401 | ✅ | ⬜ chưa phủ |
| RBAC-03 | `RADIOLOGIST` tạo bệnh nhân | 403 | ✅ | ✅ `e2e/08-rbac-ui.spec.ts` (nút tạo bị ẩn) |
| RBAC-04 | `APPROVER` lưu báo cáo | 403 | ✅ | ✅ `e2e/08-rbac-ui.spec.ts` |
| RBAC-05 | `ADMIN` lưu báo cáo | 403 | ✅ | ✅ `e2e/08-rbac-ui.spec.ts` |
| RBAC-06 | `ADMIN` chuyển T7 (duyệt) | 403 | ✅ | ✅ `e2e/08-rbac-ui.spec.ts` (nút T7 ẩn với ADMIN) |
| RBAC-07 | `CLINICIAN` xem audit log | 403 | ✅ | ⬜ chưa có màn audit log |
| RBAC-08 | `ADMIN` xem audit log | 200 | ✅ | ⬜ chưa có màn audit log |
| RBAC-09 | `RADIOLOGIST` thứ hai lưu báo cáo ca người khác | 403 | ✅ | ✅ `Frontend/.../studiesApi.test.ts` (`REPORT_NOT_HOLDER`) |
| RBAC-10 | Người vừa đọc vừa là `APPROVER` tự duyệt | 403/422 | ✅ | ✅ `Frontend/.../studiesApi.test.ts` (`NOT_APPROVING_HOLDER`) |
| RBAC-11 | `RADIOLOGIST` xử lý identity-conflict | 403 | ✅ | ⬜ chưa có UI |
| RBAC-12 | `TECHNICIAN` hủy ca đã có UID | 422 | ✅ | ✅ `e2e/03-transitions.spec.ts` + `studiesApi.test.ts` |
| RBAC-13 | `CLINICIAN` đặt `study_instance_uid` | ❓ chưa chốt | ✅ | ✅ `Frontend/.../studiesApi.test.ts` + `updatePayloads.test.ts` (khóa trường) |
| RBAC-14 | Mỗi lần 403 ghi thêm `audit_log` | — | ✅ | — không kiểm được từ UI |
| RBAC-15 | Xuất CSV: không token → 401, có quyền → 200 | 401/200 | ✅ | ⬜ chưa phủ |

---

## 3. Ma trận 9 bước chuyển trạng thái T2–T10

| Mã | Chuyển | Vai trò được phép | Bắt lý do | Unit | E2E |
|---|---|---|---|---|---|
| T2 | `SCHEDULED → UNREAD` | ADMIN, TECHNICIAN | không | ✅ `stateMachine.spec.ts` | ✅ |
| T3 | `UNREAD → READING` | RADIOLOGIST | không | ✅ | ✅ |
| T4 | `READING → UNREAD` (bỏ dở) | người đang đọc, ADMIN | **có** (SM-11) | ✅ | ✅ |
| T5 | `READING → READ` | người đang đọc | không | ✅ | ✅ |
| T6 | `READ → APPROVING` | APPROVER | không | ✅ | ✅ |
| T7 | `APPROVING → APPROVED` | người đã mở duyệt | không | ✅ | ✅ |
| T8 | `APPROVING → READING` (trả lại) | APPROVER | **có** (SM-11) | ✅ | ✅ |
| T9 | `APPROVED → READING` (mở lại) | ADMIN, APPROVER ❓ | **có** (SM-15) | ✅ | ✅ |
| T10 | `SCHEDULED → CANCELLED` (hủy) | ADMIN, TECHNICIAN | **có** (SM-11) | ✅ | ✅ |

⚠️ **Lệch tài liệu `N-03`:** `doc/06` mục 4 cho **T9** cả `ADMIN` ✔ và `APPROVER` ✔, nhưng mục 6
ghi `study.reopen` là *đề xuất* (❓ D16) và **frontend hiện chỉ cho ADMIN**. Cần nhóm chốt.

---

## 4. Ma trận 31 user story (`doc/13` mục 3)

| Story | Mã chuyển | Mã lỗi trong tiêu chí | Test phủ | Trạng thái story |
|---|---|---|---|---|
| US-KTV-01 Tạo bệnh nhân | — | 201, 409 | `e2e/04-create-study.spec.ts` | 🐞 `L1` — chấp ngày sinh tương lai |
| US-KTV-02 Tìm bệnh nhân tránh trùng | — | *(không có ca lỗi)* | `e2e/04-create-study.spec.ts` | ✅ tự động |
| US-KTV-03 Sửa chỉ định | — | 403 | `Frontend/.../updatePayloads.test.ts` | ✅ tự động |
| US-KTV-04 Thêm ca chụp (T1) | T1 | 422 | `e2e/04-create-study.spec.ts` | ✅ tự động |
| US-KTV-05 Cập nhật ca, bảng so sánh | — | 403 ❓ chưa chốt | `Frontend/.../updatePayloads.test.ts` | ✅ một phần — payload đã phủ, khóa trường theo vai trò chưa |
| US-KTV-06 Đánh dấu đã có ảnh (T2) | T2 | 422 (SM-12), 403 (SM-08) | `e2e/03-transitions.spec.ts` | ✅ tự động |
| US-KTV-07 Xử lý lệch HIS/RIS | — | 403 (RBAC-11) | — | ⬜ chưa có UI |
| US-KTV-08 Hủy ca chưa chụp (T10) | T10 | 422 ×2 (RBAC-12, SM-11) | `e2e/03-transitions.spec.ts` | ✅ tự động |
| US-KTV-09 Đăng nhập | — | 401 | `e2e/01-auth.spec.ts` | ✅ tự động |
| US-KTV-10 Worklist tab "Chờ" + lọc | — | *(không có ca lỗi)* | `e2e/06-status-counts.spec.ts` | ✅ một phần — bộ đếm đã phủ, bộ lọc chưa |
| US-KTV-11 Xuất CSV + in danh sách | — | 200, 401, 400 | `Frontend` chưa có test; backend `ExportServiceUnitTest` | ⬜ phần UI chưa phủ — **story duy nhất đã `Done`** |
| US-BSLS-01 Tạo chỉ định | — | 422 | `e2e/04-create-study.spec.ts` | ✅ tự động |
| US-BSLS-02 Xem thông tin lâm sàng | — | *(không có)* | — | ⬜ chưa phủ (`ClinicalModal`) |
| US-BSLS-03 Tên dịch vụ + kết luận tab 3 | — | *(không có)* | — | ⬜ chưa phủ |
| US-BSLS-04 Xem ảnh từ modal lâm sàng | — | *(không có)* | `e2e/05-view-image-gate.spec.ts` (phần gating) | 🚫 phần OHIF bị chặn `D12-01` |
| US-BSCDHA-01 Mở ca để đọc (T3) | T3 | 409 (SM-16) | `e2e/03-transitions.spec.ts`, `studiesApi.test.ts` | ✅ tự động |
| US-BSCDHA-02 Xem ảnh từ worklist | — | *(không có)* | `e2e/05-view-image-gate.spec.ts` | 🚫 `D12-01` |
| US-BSCDHA-03 Nhập + chốt báo cáo (T5) | T5 | 422 (SM-10), 403 (SM-17) | `reportSteps.test.ts`, `e2e/03-transitions.spec.ts` | ✅ tự động |
| US-BSCDHA-04 Bỏ dở ca (T4) | T4 | 422 (SM-11) | `e2e/03-transitions.spec.ts` | ✅ tự động |
| US-BSCDHA-05 Panel chi tiết + phiếu liên quan | — | *(không có)* | `e2e/08-rbac-ui.spec.ts` (phần panel) | ✅ tự động |
| US-BSCDHA-06 Số lượng ca theo nhóm | — | *(không có)* | `e2e/06-status-counts.spec.ts` | ✅ tự động |
| US-BSDUYET-01 Mở ca để duyệt (T6) | T6 | 403/422 ❓ (SM-14) | `e2e/03-transitions.spec.ts`, `studiesApi.test.ts` | ✅ tự động |
| US-BSDUYET-02 Duyệt (T7) | T7 | 403 (RBAC-06, SM-09) | `e2e/03-transitions.spec.ts`, `e2e/08-rbac-ui.spec.ts` | ✅ tự động |
| US-BSDUYET-03 Trả lại để sửa (T8) | T8 | 422 (SM-11) | `e2e/03-transitions.spec.ts` | ✅ tự động |
| US-BSDUYET-04 Tab "Đã thực hiện" + lọc | — | *(không có)* | `e2e/06-status-counts.spec.ts` | ✅ một phần — bộ đếm đã phủ, bộ lọc chưa |
| US-ADMIN-01 Mở lại ca đã duyệt (T9) | T9 | 403 (SM-15), 422 | `e2e/03-transitions.spec.ts` | ✅ tự động |
| US-ADMIN-02 Danh sách lệch HIS/RIS | — | 403 | — | ⬜ chưa có UI |
| US-ADMIN-03 Xử lý lệch (Admin) | — | 422 | — | ⬜ chưa có UI |
| US-ADMIN-04 Xem audit log | — | 403 (RBAC-07) | — | ⬜ chưa có màn audit log |
| US-ADMIN-05 Sửa định danh bệnh nhân | — | 403 ❓ (D24) | `Frontend/.../updatePayloads.test.ts` | ✅ một phần — payload đã phủ, UI chưa |
| US-ADMIN-06 Giải phóng khóa mềm | T4 | 422 (SM-11) | `e2e/08-rbac-ui.spec.ts` (nhánh ADMIN `study.release`) | ✅ tự động |

**Tổng kết story:** ✅ 20 · ⬜ 8 · 🚫 2 · 🐞 1

### ⚠️ Lệch quy ước của chính `doc/13` (`N-04`)

`doc/13` mục 1 yêu cầu **mọi** story có ít nhất một tiêu chí là ca lỗi/từ chối quyền, và mục 4
dòng IN-14 khẳng định điều đó đã đạt. Thực tế **9/31 story không có tiêu chí 403/409/422 nào**:
`US-KTV-02`, `US-KTV-10`, `US-BSLS-02`, `US-BSLS-03`, `US-BSLS-04`, `US-BSCDHA-02`,
`US-BSCDHA-05`, `US-BSCDHA-06`, `US-BSDUYET-04`.

Ngoài ra `doc/13` mục 6 ghi "**30 story**" trong khi mục 3 có **31** — lệch đúng 1, khớp việc
`US-KTV-11` được thêm sau (2026-11-07).

---

## 5. Nợ tài liệu phát hiện được

| Mã | Tài liệu | Nội dung lỗi thời / lệch |
|---|---|---|
| `N-01` | `doc/12` TC-16, bước 10 | Ghi frontend thiếu `CANCELLED` — đã có từ 2026-11-12 tại `shared/lib/types.ts` |
| `N-02` | `doc/12` TC-22 | Ghi "backend chưa có khóa mềm / người duyệt ≠ người đọc" — đã có `RbacIntegrationTest` RBAC-09/10 |
| `N-03` | `doc/06` mục 4 vs mục 6 | T9: bảng cho ADMIN + APPROVER, mục 6 ghi là đề xuất ❓D16, frontend chỉ ADMIN |
| `N-04` | `doc/13` mục 1, 4, 6 | 9/31 story thiếu tiêu chí ca lỗi; đếm "30 story" trong khi có 31 |
| `N-05` | `doc/13` mục 5.1 | Ghi `AddStudyModal`/`UpdateStudyModal` "chỉ là giao diện tĩnh, nút xác nhận chỉ đóng modal" — đã nối API thật từ 2026-11-12 |
| `N-06` | `doc/13` mục 5.1 | Ghi `LoginPage` "5 tài khoản demo nằm cứng trong `data/mock.ts`, chưa gọi API thật" — nay có 7 tài khoản và gọi `/auth/login` thật |
| `N-07` | `doc/12` mục 3.2.1 | Đếm tài khoản không nhất quán: TC-01 ghi 7, tổng sau V8 ghi 9 |
