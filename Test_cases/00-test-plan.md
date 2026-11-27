# Kế hoạch kiểm thử chốt cuối trước deploy — web-MRI (RIS/PACS)

| | |
|---|---|
| Phiên bản | 1.0 |
| Ngày | 2026-11-26 |
| Nhánh kiểm thử | `Khanh` (đã đồng bộ `main` tại `d9ee3f7`) |
| Phạm vi sở hữu | Người phụ trách frontend + test |

## 1. Vì sao có tài liệu này

Trưởng nhóm phân việc "test" nhưng không nói rõ phạm vi. Tài liệu này chốt cách hiểu, dựa trên
chính tiêu chuẩn nhóm đã viết ra:

`doc/13-user-stories.md` mục 1 định nghĩa một story là `Done` khi **"đã code và có test khớp
tiêu chí chấp nhận"**. Hiện trạng: **31/31 story còn ở `Draft`** (duy nhất `US-KTV-11` được
đánh `Done` ngày 2026-11-07). Tầng UI của frontend phủ **0%** test.

⇒ Theo đúng tiêu chuẩn nhóm tự đặt, gần như toàn bộ story frontend **chưa đủ điều kiện gọi là
xong**. Đó là khoảng trống mà bộ kiểm thử này lấp.

## 2. Hai lớp kiểm thử, không phải hai lựa chọn

"Test kiểu người dùng" và "viết test case" là **hai lớp bổ sung nhau**, bắt loại lỗi khác nhau:

| Lớp | Công cụ | Bắt được gì | Không bắt được gì |
|---|---|---|---|
| Tự động | Vitest (`unit/`), Playwright (`e2e/`) | Hồi quy — thứ hôm qua đúng hôm nay sai | Lỗi nghiệp vụ chưa ai nghĩ tới |
| Thủ công | `02-uat-checklist.md` | Lỗi nghiệp vụ, nghịch lý dữ liệu, trải nghiệm vô lý | Hồi quy (không ai bấm lại 500 lần) |

**Bằng chứng cần cả hai:** 192 test backend + 80 test frontend đều xanh, nhưng lỗi **L1** (tạo
được bệnh nhân sinh năm 2030 → API trả 500 khi đọc lại) chỉ lộ ra khi có người ngồi điền form.
Lỗi này còn sống sót qua cả một đợt review code.

## 3. Phạm vi

### Trong phạm vi

- Toàn bộ tầng UI frontend: worklist 2 tab, bộ lọc, panel bệnh nhân, 9 bước chuyển trạng thái
  T2–T10, các modal tạo/sửa, form báo cáo, màn đăng nhập.
- Ma trận quyền **ở tầng giao diện**: 5 vai trò × 7 trạng thái (đặc tả: `doc/06b-ui-permission-spec.md` mục 2).
- 22 test case `TC-01`…`TC-22` (`doc/12` mục 3.1).
- 15 test case `RBAC-01`…`RBAC-15` (`doc/06` mục 8).
- Tiêu chí chấp nhận Given/When/Then của 31 user story (`doc/13` mục 3).
- Kịch bản UAT 10 bước (`doc/12` mục 4).

### Ngoài phạm vi (có lý do)

| Hạng mục | Lý do |
|---|---|
| Test integration backend | Đã phủ tốt: 33 file, ~311 test, `RbacIntegrationTest` map 1-1 với RBAC-01…15. Không làm lại. |
| `TC-14` | Tài liệu tự ghi `[HOÃN] H-03` — chưa có Mock HIS thật, không chạy được trong 8 tuần. |
| `TC-03`, `TC-04` | Phụ thuộc bộ ảnh DICOM mẫu mà `doc/12` mục 3.3 ghi **chưa có**. |
| `TC-20`, `TC-22` | `doc/12` ghi "chưa có code chạy" (row-level D34, khóa mềm). Nhưng backend **nay đã có** `RbacIntegrationTest` RBAC-09/10 — xem ghi chú lệch tài liệu ở `01-traceability-matrix.md`. |
| `TC-21` | Gateway `JsonFileHisGateway` thuộc backend, không phải frontend. |
| Hiệu năng, tải, bảo mật xâm nhập | Không được phân công. |

## 4. Môi trường

| | |
|---|---|
| Node | 22.23.5 (thỏa engine Vite 7.1) |
| Vitest | 2.1.8 — môi trường `jsdom` |
| Playwright | 1.63.0 — Chromium `chromium-1243` tại `PLAYWRIGHT_BROWSERS_PATH` |
| Chế độ API | `mock` (mặc định) — không cần backend/MySQL/Docker |
| Tài khoản | 7 tài khoản seed V7/V8, mật khẩu demo nằm công khai trong `mock/seed.ts` và hiện trên màn đăng nhập |

**Giới hạn đã biết của môi trường:** Docker daemon không dùng được ⇒ `mvn verify` sẽ đỏ vì
thiếu Testcontainers, **không phải vì code sai**. Chỉ ~15 file unit backend chạy được bằng
`mvn test -Dtest=...`.

## 5. Tiêu chí vào / ra của cổng deploy

### Tiêu chí vào (đủ điều kiện bắt đầu chốt)

- [x] Nhánh kiểm thử đồng bộ `main`.
- [x] `npm run build` của Frontend xanh.
- [x] 80/80 test frontend sẵn có xanh.
- [ ] Backend `mvn verify` xanh **trên môi trường CÓ Docker** (không kiểm được ở đây).

### Tiêu chí ra (được phép deploy)

- [ ] `npm run test:unit` và `npm run test:e2e` trong `Test_cases/` đều xanh.
- [ ] Không còn lỗi **Nặng** nào mở trong `reports/bug-report.md`.
- [ ] Mọi lỗi **Trung bình** còn mở đều có người nhận và ngày hẹn sửa.
- [ ] `02-uat-checklist.md` chạy hết, mọi bước tick hoặc có lý do bỏ qua.
- [ ] CI frontend đã gác `npm run test` (hiện **CHƯA** — xem lỗi `D-01`).

## 6. Phân loại độ nặng

| Mức | Nghĩa | Chặn deploy? |
|---|---|---|
| **Nặng** | Mất/sai dữ liệu bệnh nhân, lỗi 500, vượt quyền, sai trạng thái ca chụp | Có |
| **Trung bình** | Nghiệp vụ sai nhưng có đường tránh, thông báo lỗi sai/thiếu | Trưởng nhóm quyết |
| **Nhẹ** | Hiển thị, chính tả, khả năng truy cập | Không |
| **Nợ tài liệu** | Tài liệu lệch so với code đang chạy | Không, nhưng phải sửa tài liệu |

## 7. Rủi ro đã biết, không phải lỗi code

| Mã | Rủi ro | Nguồn |
|---|---|---|
| `D12-01` | Bộ ảnh DICOM mẫu chưa chọn, chưa tải → chặn TC-03/04/15 và bước 3–4 của UAT | `doc/12` mục 3.3, 8 |
| `D12-02` | Mọi `scheduled_at` trong seed là ngày cố định 29–31/10/2026 và 05/11/2026 ⇒ bộ lọc **"Hôm nay"/"Hôm qua" sẽ ra rỗng** khi demo | `doc/12` mục 6 |
| `D10` | Seed `ordering_doctor_name` không khớp `app_user.full_name` ⇒ `bslamsang` không gọi được `clinical-snapshot` (row-level D34) | `doc/06` mục 5 |
| — | Docker daemon không có ⇒ không chạy được integration backend | Môi trường |
