# Test_cases — Bộ kiểm thử chốt cuối trước deploy

Thư mục này do người phụ trách **test + frontend** sở hữu. Nó tách khỏi `Frontend/` và `Backend/`
để việc kiểm thử không phụ thuộc vào ai đang sửa code sản phẩm.

## Chạy thế nào

```bash
cd Test_cases
npm install            # lần đầu

npm run test:unit      # unit/component test (Vitest) — nhanh, không cần server
npm run test:e2e       # E2E (Playwright) — tự bật dev server của Frontend
npm test               # cả hai
npm run report         # mở báo cáo HTML của Playwright
```

### Không cần Docker, không cần backend

E2E chạy trên **mock mode** của Frontend (`VITE_API_MODE` không đặt → mặc định `mock`, xem
`Frontend/src/shared/api/client.ts`). Toàn bộ luồng nghiệp vụ, mã lỗi 403/409/422 và dữ liệu
seed V7/V8 đều được `Frontend/src/shared/api/mock/mockClient.ts` mô phỏng.

Điều này quan trọng: container CI và máy dev thường **không có Docker daemon**, nên 18/33 file
test integration của backend (dùng Testcontainers MySQL) không chạy được. E2E ở đây vẫn chạy.

Muốn kiểm thật vào backend:

```bash
VITE_API_MODE=live VITE_API_BASE_URL=http://localhost:8080/api/v1 npm run test:e2e
```

## Cấu trúc

| Đường dẫn | Nội dung | Git |
|---|---|---|
| `00-test-plan.md` | Kế hoạch test, phạm vi, tiêu chí vào/ra của cổng deploy | commit |
| `01-traceability-matrix.md` | Ma trận truy vết: TC-xx / US-xxx / RBAC-xx → test nào phủ → trạng thái | commit |
| `02-uat-checklist.md` | Kịch bản test thủ công kiểu người dùng, in ra tick tay được | commit |
| `unit/` | Unit + component test (Vitest), import mã nguồn thật qua alias `@` | commit |
| `e2e/` | E2E Playwright trên mock mode | commit |
| `fixtures/` | Tài khoản seed, hàm đăng nhập, hàm chụp ảnh | commit |
| `reports/` | **Báo cáo lỗi + ảnh chụp + kết quả chạy** | **gitignore** |

### ⚠️ Về `reports/`

Thư mục này **không được git theo dõi** (theo yêu cầu: tài liệu lỗi không vào repo chung).
Hệ quả: **nội dung trong đó sẽ mất khi container/máy bị dọn.** Đã từng mất một báo cáo QA đầy
đủ vì lý do này. Hãy tải `reports/bug-report.md` và `reports/screenshots/` về máy sau mỗi phiên
làm việc.

## Quy ước viết test

- Mỗi test **phải trích mã truy vết** trong tên hoặc comment: `TC-09`, `US-KTV-08`, `RBAC-12`,
  `SM-11`, `T10`. Không có mã thì không biết test đang bảo vệ yêu cầu nào.
- Unit test import mã nguồn thật bằng alias `@` (trỏ `../Frontend/src`), giống hệt cách code sản
  phẩm import. Nhờ vậy copy test qua lại giữa `Frontend/src` và `Test_cases/unit` không phải sửa
  import.
- Phát hiện lỗi thì ghi vào `reports/bug-report.md` kèm ảnh, **đừng** để test đỏ vĩnh viễn: test
  đỏ thường trực làm cả bộ mất giá trị gác cổng. Test khóa **hành vi đúng mong đợi** chỉ được
  thêm sau khi lỗi đã sửa.
