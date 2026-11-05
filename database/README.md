# Database VR-RIS PACS

MySQL 8.4 + Flyway, chạy bằng Docker Compose. **Mọi thành viên chạy cùng một bộ migration nên có cùng cấu trúc bảng**; dữ liệu trong bảng thì mỗi người tự do thêm/xóa trên máy mình, không cần giống nhau.

Schema bám theo `doc/02-erd.md` (v0.9, bản nháp chờ nhóm xác nhận) và `doc/diagrams/02-erd.mmd`.

## Chạy lần đầu

Cần Docker Desktop (có `docker compose`). Mở Docker Desktop lên trước, rồi:

```bash
cd database
docker compose up -d
docker compose logs -f flyway     # đợi thấy: Successfully applied 7 migrations
```

Xong. Database `vr_ris` đã có đủ 14 bảng và dữ liệu giả để dev.

| | Giá trị mặc định |
|---|---|
| Host | `localhost` |
| Cổng | **3307** (không phải 3306, để khỏi đụng MySQL cài sẵn trên máy) |
| Database | `vr_ris` |
| User / mật khẩu | `ris` / `ris_dev_pw` |
| Root | `root` / `root_dev_pw` |

Muốn đổi cổng hoặc mật khẩu: copy `.env.example` thành `.env` rồi sửa (`.env` không bị commit).

Vào xem dữ liệu bằng dòng lệnh:

```bash
docker compose exec mysql mysql -uris -pris_dev_pw vr_ris
```

hoặc dùng DBeaver / MySQL Workbench / IntelliJ nối vào `localhost:3307`.

## Lệnh hay dùng

| Việc | Lệnh (trong thư mục `database/`) |
|---|---|
| Bật database | `docker compose up -d` |
| Tắt, giữ dữ liệu | `docker compose stop` |
| **Xóa sạch, tạo lại từ đầu** | `docker compose down -v && docker compose up -d` |
| Sau khi `git pull` có migration mới | `docker compose run --rm flyway` |
| Xem Flyway đã chạy những gì | `docker compose logs flyway` |

## Tài khoản demo (dữ liệu giả)

Mật khẩu của cả 5 tài khoản đều là `123456`, chỉ dùng để dev. Khớp với tài khoản trong `Frontend/README.md`.

| Tài khoản | Vai trò |
|---|---|
| `admin` | `ADMIN` |
| `bslamsang` | `CLINICIAN` |
| `ktv` | `TECHNICIAN` |
| `bscdha` | `RADIOLOGIST` |
| `bsduyet` | `APPROVER` |

Bệnh nhân mẫu mã `TEST0001`…`TEST0008`, tên có hậu tố "Giả", là **dữ liệu giả**. Tuyệt đối không nhập dữ liệu bệnh nhân thật vào database này (nguyên tắc bất biến của dự án).

## Các bảng

| Migration | Bảng |
|---|---|
| `V1__ref_data.sql` | `department`, `service_item`, `device` |
| `V2__security.sql` | `app_user`, `role`, `user_role` |
| `V3__patient_order_study.sql` | `patient`, `imaging_order`, `study` |
| `V4__workflow.sql` | `study_status_history`, `report` |
| `V5__audit_and_conflict.sql` | `audit_log`, `identity_conflict` |
| `V6__clinical_snapshot.sql` | `clinical_snapshot` |
| `V7__seed_demo_data.sql` | chỉ có dữ liệu giả, không tạo bảng |
| `V8__rbac_and_demo_extras.sql` | chỉ bổ sung dữ liệu giả còn thiếu (tài khoản demo thứ hai, `his_order_id` khớp `his-mock/orders.json`, một ca `CANCELLED`) — không tạo bảng. Xem `doc/12-seed-data-va-kich-ban-demo.md` mục 3.2 |

Ngoài 14 bảng trên, Flyway tự tạo thêm bảng `flyway_schema_history` để ghi nhớ đã chạy file nào. Đừng sửa bảng này.

Mã trạng thái ca chụp lưu dạng chữ: `SCHEDULED`, `UNREAD`, `READING`, `READ`, `APPROVING`, `APPROVED`, và `CANCELLED` (đang ❓ chờ nhóm xác nhận). Mã này khác mã tiếng Việt trong Frontend hiện tại; xem `doc/02-erd.md` mục 4.5 và 10.

## Quy tắc làm việc nhóm (quan trọng)

1. **Không sửa file migration đã có trên `main`.** Flyway ghi lại checksum của từng file; sửa file cũ sẽ làm máy của mọi người báo lỗi `checksum mismatch`.
2. **Muốn đổi cấu trúc bảng: thêm file mới**, đánh số tiếp theo và đặt tên `V8__mo_ta_ngan.sql`, `V9__...`. Tên file: chữ `V`, số, **hai gạch dưới**, mô tả không dấu.
3. **Trước khi tạo file mới, `git pull` để xem số version đã bị lấy chưa.** Hai người cùng tạo `V8` sẽ xung đột; người merge sau phải đổi sang `V9`.
4. **Không sửa cấu trúc bảng bằng tay trên máy** (ví dụ `ALTER TABLE` trong DBeaver) mà không có file migration. Máy bạn sẽ lệch với cả nhóm. Lỡ làm rồi thì `docker compose down -v && docker compose up -d`.
5. **Dữ liệu thì tự do.** Thêm bệnh nhân, ca chụp, báo cáo bao nhiêu tùy ý trên máy mình. Không đưa dữ liệu thử của cá nhân vào file migration; nếu cả nhóm cần thêm dữ liệu mẫu chung thì thêm một file `V…__seed_…` mới.
6. Gặp lỗi `checksum mismatch` hoặc `Detected failed migration`: chạy lệnh "xóa sạch, tạo lại" ở bảng trên.

## Ghi chú thiết kế

- **Charset/collation:** `utf8mb4` / `utf8mb4_0900_ai_ci`, theo ERD. `ai_ci` là không phân biệt dấu và hoa thường: tìm "nguyen" sẽ ra "Nguyễn". Cột `patient_code` cũng không phân biệt hoa thường (`TEST0001` và `test0001` là cùng một mã).
- **Múi giờ:** mọi `DATETIME` lưu UTC (ERD 4.7); server được đặt `+00:00`. Quy đổi sang giờ Việt Nam ở tầng hiển thị.
- **Không có cột tuổi.** Tuổi luôn tính từ `date_of_birth` + `dob_precision` (ERD 4.1, 4.2). Dữ liệu mẫu cố ý có đủ 4 trường hợp: đủ ngày, chỉ tháng, chỉ năm, không có ngày sinh.
- **`study_status_history` và `audit_log` là append-only**, nhưng hiện chỉ backend tự giữ quy ước (chỉ `INSERT`). Chưa có trigger hay phân quyền ở mức database ❓.
- **Chưa thêm** `UNIQUE(his_order_id)`, cờ "đã sửa tay", chỗ giữ chỉ định chờ xử lý conflict: các điểm này mới nằm ở dạng đề xuất trong `doc/04-his-ris-reconciliation.md` mục 10, chờ nhóm xác nhận ERD.

## Khi có backend Spring Boot

Backend nối vào `jdbc:mysql://localhost:3307/vr_ris` bằng user `ris`. **Cập nhật 2026-11-05 — câu hỏi "chạy Flyway ở đâu" không còn là một lựa chọn duy nhất, mà theo môi trường** (đề xuất ở `doc/14-devops-trien-khai.md` mục 5, 🟡 chờ nhóm xác nhận):

| Môi trường | Ai chạy Flyway | Cấu hình backend |
|---|---|---|
| Dev local (vòng hằng ngày) | **Docker Compose này** (`database/docker-compose.yml`, như từ trước tới nay) | `spring.flyway.enabled=false` (`Backend/src/main/resources/application.yml`, không đổi) |
| Cụm Kubernetes (local, tuần 3 và 8) | **Backend tự chạy** lúc khởi động — không có container flyway riêng như docker-compose trong k8s | Profile `k8s` (`Backend/src/main/resources/application-k8s.yml`): `spring.flyway.enabled=true`, đọc `classpath:db/migration` |

Dù chạy ở đâu, cả hai đều dùng **đúng nội dung** `database/migrations/` (nguồn duy nhất): Docker Compose đọc trực tiếp qua volume; backend ở k8s đọc bản mà `maven-resources-plugin` (`Backend/pom.xml`) copy từ `database/migrations/` vào `target/classes/db/migration` lúc build — không có file `.sql` nào được chép tay/commit trùng. Cả hai cũng dùng chung bảng `flyway_schema_history` (trong cùng schema `vr_ris`) nên không bị lệch version. Test của backend (`Backend/src/test/resources/application.yml`) vẫn là cách thứ ba, riêng: tự chạy Flyway nhưng trỏ `filesystem:../database/migrations`, chỉ tới `V6` (không seed `V7`/`V8`) — không đổi.

## Trạng thái kiểm tra (2026-11-05, cập nhật từ 2026-10-31)

- Cả 8 file (V1–V8) đã chạy đúng thứ tự trên **MySQL 8.4 thật** qua `docker compose run --rm flyway migrate` — **đã chạy thật trong phiên làm việc này**, không còn là "chưa kiểm chứng" như ghi chú 2026-10-31 dưới đây: `docker compose run --rm flyway info` báo đúng `Pending` cho V8 trước khi chạy, và `Successfully applied 1 migration to schema vr_ris, now at version v8` sau khi chạy `migrate`.
- `V1`–`V7` tạo đủ 14 bảng, 23 khóa ngoại; khóa duy nhất và khóa ngoại chặn đúng dữ liệu sai; dữ liệu mẫu nhất quán với state machine. `V8` chỉ `INSERT`/`UPDATE` (không `CREATE TABLE`), xem mục "Các bảng" ở trên.
- `docker-compose.yml` đã được `docker compose config` đọc hợp lệ; tag `mysql:8.4` và `flyway/flyway:11` có trên Docker Hub.
- *(Ghi chú 2026-10-31, nay đã lỗi thời — giữ lại để biết lịch sử)*: ~~Chưa chạy được container Flyway thật (lúc viết, Docker Desktop trên máy tác giả chưa bật)~~.
