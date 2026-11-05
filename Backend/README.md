# Backend VR-RIS PACS

Spring Boot 3 (Java 21, Maven). Scope hiện tại: **3 API tra cứu khoa / máy / dịch vụ** (nhóm
endpoint `reference-data` trong `doc/api/openapi.yaml`). Chưa làm các nhóm endpoint khác
(patients, imaging-orders, studies, …) và chưa nối JWT/RBAC (IN-13, IN-14) — xem mục "Chưa làm" dưới đây.

## Chạy

1. Bật database trước (xem `database/README.md`):

   ```bash
   cd database
   docker compose up -d
   ```

2. Chạy backend:

   ```bash
   cd Backend
   mvn spring-boot:run
   ```

Mặc định đọc `src/main/resources/application.yml`: nối MySQL ở `localhost:3307/vr_ris`
(user `ris` / `ris_dev_pw`, khớp `database/README.md`), serve ở
`http://localhost:8080/api/v1` (khớp `doc/api/openapi.yaml`).

Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`.

## API đã làm

| Endpoint | Mô tả | Nguồn đặc tả |
|---|---|---|
| `GET /departments` | Cây khoa → nhóm (`parent_id` để client dựng cây) | `doc/api/openapi.yaml` operation `listDepartments` |
| `GET /devices?department_id=&modality=` | Danh sách máy chụp, lọc tuỳ chọn | operation `listDevices` |
| `GET /service-items?modality=` | Danh mục dịch vụ CĐHA, lọc tuỳ chọn | operation `listServiceItems` |

Response JSON dùng `snake_case` đúng tên cột ERD (`doc/02-erd.md`), chỉ trả đúng field có trong
schema OpenAPI (`Department`, `Device`, `ServiceItem`) — entity có thêm vài cột nội bộ
(`is_active`, `station_name`) không nằm trong contract nên không trả ra.

Chỉ trả hàng có `is_active = true`.

## Chưa làm (có chủ đích, ngoài phạm vi yêu cầu này)

- **Xác thực/JWT (IN-13) và RBAC (IN-14).** `doc/06-rbac-matrix.md` và `openapi.yaml` ghi cả 3
  endpoint trên yêu cầu đăng nhập (không `public`), nhưng chưa có hạ tầng Spring Security trong
  repo để nối vào — controller hiện **chưa chặn** truy cập. Cần bổ sung khi làm IN-13/IN-14,
  không tự suy diễn cơ chế xác thực ở đây.
- Audit log (IN-15): 3 endpoint này không có `x-audit` trong OpenAPI vì là dữ liệu danh mục/seed,
  không phải dữ liệu bệnh nhân (`doc/02-erd.md` mục 2) — không ghi `audit_log`.
- Mọi endpoint khác (`patients`, `imaging-orders`, `studies`, `reports`, `identity-conflicts`,
  `audit-logs`, `auth`) chưa viết.
- Flyway: migration chạy ở `database/docker-compose.yml` (xem `database/README.md`), backend đặt
  `spring.flyway.enabled=false` và `ddl-auto=none` để không tạo/sửa schema trùng.
