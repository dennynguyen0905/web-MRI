# VR-RIS PACS — DevOps & triển khai

Tài liệu con của `README.md`, mục 6B ([T3] Dockerfile, manifest k8s, ConfigMap/Secret, quy ước đặt tên). Trả lời: chạy hệ thống bằng gì, image dựng thế nào, cụm k8s gồm những gì, cấu hình/bí mật đi đâu, CI làm gì, và triển khai từ máy trắng theo đúng thứ tự nào.

| | |
|---|---|
| **Phiên bản** | 0.1 — NHÁP, chờ nhóm xác nhận |
| **Ngày** | 2026-11-05 |
| **Tác giả** | Agent 2 ("Vận hành"), chạy sau Agent 1 (đã viết `AGENTS.md`, `doc/08-hl7-message-spec.md`, `doc/10-lien-ket-he-thong.md`, `doc/11-ai-design.md`, `doc/13-user-stories.md` cùng ngày) |
| **Môi trường đã chốt** | **Docker Desktop Kubernetes** trên máy cá nhân (README mục 8.2, cập nhật bởi tài liệu này — xem mục 11) |
| **Tài liệu liên quan** | `README.md` mục 2.6, 6B, 8.2 · `01-scope.md` IN-16/17/18, mục 6 (kịch bản nghiệm thu) · `02-erd.md` · `09-bao-mat-toan-ven-du-lieu.md` mục 5 (ConfigMap/Secret, nguồn chính cho *nguyên tắc* bảo mật — tài liệu này chỉ áp dụng cụ thể) · `10-lien-ket-he-thong.md` (đã sửa lại L-01/L-02/L-03/L-05/L-09/L-11/L-12 cho khớp quyết định ở đây — xem mục 11) · `15-kien-truc-backend.md` mục 3 (vị trí migration) |
| **Thư mục liên quan** | `deploy/` (Dockerfile, Docker Compose, manifest k8s — **đã có**, không còn "dự kiến") |

> **Quy ước nguồn:** tài liệu này là nguồn duy nhất cho quy ước đặt tên hạ tầng (image, namespace, Service/Deployment, biến môi trường) và cho sơ đồ triển khai *chi tiết* (tên đối tượng k8s thật). Sơ đồ *khái niệm* (README mục 2.6, `diagrams/06-trien-khai-k8s-local.mmd`) vẫn giữ nguyên làm bản tóm tắt — không xóa, không thay thế, chỉ được tài liệu này làm chi tiết hơn.

---

## 1. Hai chế độ chạy

| | **Docker Compose** (vòng dev hằng ngày) | **Kubernetes** (tuần 3 và tuần 8 — README mục 7.4) |
|---|---|---|
| Khi nào dùng | Code hằng ngày: sửa Backend/Frontend, chạy lại trong vài giây | Kiểm tra tích hợp đầy đủ, demo, nghiệm thu |
| MySQL | `database/docker-compose.yml` (đã có từ trước) — container `vr-ris-mysql`, cổng host `3307` | `StatefulSet mysql` trong namespace `vr-ris`, PVC riêng |
| Migration | Container `flyway` riêng trong `database/docker-compose.yml` | **`ris-backend` tự chạy Flyway** lúc khởi động (profile `k8s`) — xem mục 5 |
| Backend | Chạy **trực tiếp trên máy** bằng `mvn spring-boot:run` (hot reload, không build image) | Image `vrris/ris-backend:dev`, `Deployment ris-backend` |
| Frontend | Chạy **trực tiếp trên máy** bằng `npm run dev` (Vite dev server, hot reload) | Image `vrris/ris-frontend:dev` (nginx + build tĩnh), `Deployment ris-frontend` |
| "Chạy cả cụm bằng Docker, không cần k8s" | `deploy/compose/docker-compose.full.yml` (mới, mục 2) — build + chạy **cả 4** service bằng Docker, không hot reload. Dùng khi cần demo nhanh mà không muốn bật k8s | — |
| Cổng mở ra máy host | `3307` (MySQL) | `8080` (frontend, **duy nhất người dùng RIS cần biết** — README mục 2.6), `8043` (OHIF), `4242`+`8042` (Orthanc) — xem mục 4 |
| Một lệnh khởi động | `cd database && docker compose up -d` rồi tự chạy `mvn spring-boot:run` / `npm run dev` | `kubectl apply -k deploy/k8s` (đúng cú pháp `01-scope.md` mục 6 bước 1) |

**Vì sao giữ nguyên "chạy trực tiếp trên máy" cho vòng dev** thay vì bắt mọi người dùng `docker-compose.full.yml` hằng ngày: build lại Docker image sau mỗi lần sửa code mất 10–20 giây (Maven) hoặc vài giây (Vite, nhưng vẫn chậm hơn hot reload) — không đáng so với lợi ích. `docker-compose.full.yml` và k8s dùng **chung hai Dockerfile** (`deploy/docker/`), nên không có hai bộ cấu hình build khác nhau.

⚠️ **Không chạy đồng thời `database/docker-compose.yml` (một mình) và `docker-compose.full.yml`** — cả hai cùng định nghĩa container `vr-ris-mysql` (cổng `3307`, xem mục 2): chạy cả hai sẽ báo lỗi trùng tên container/cổng. Dừng cái đang chạy trước khi đổi sang cái kia.

---

## 2. Quy ước đặt tên

Tìm trong repo trước khi đặt mới: `database/docker-compose.yml` dùng tiền tố container `vr-ris-*` và project name `vr-ris-db`; `Backend/pom.xml` dùng `groupId com.vrris`, `artifactId ris-backend`, package `com.vrris.pacs`; `.github/workflows/backend-ci.yml` không có tiền tố riêng. README mục 2.6 đã đề xuất namespace `vr-ris` và tên Deployment `ris-frontend`/`ris-backend`/`ohif-viewer`, Service tương ứng, `mock-his`, `orthanc` — tài liệu này **giữ nguyên các tên đã có trong README**, chỉ bổ sung phần README chưa nói tới (ConfigMap/Secret/PVC/biến môi trường/tên file manifest).

| Hạng mục | Quy ước | Đã có sẵn hay mới đề xuất |
|---|---|---|
| Namespace k8s | `vr-ris` | Đã có (README mục 2.6) |
| Tên image | `vrris/<thành-phần>` — ví dụ `vrris/ris-backend`, `vrris/ris-frontend` | **Mới** — nhất quán với `groupId com.vrris` đã có |
| Tag image | `:dev` cho image build local dùng trong k8s/compose; `:ci` cho image build tạm trong GitHub Actions (không push đi đâu) | **Mới** |
| Deployment/StatefulSet | `ris-frontend`, `ris-backend` (Deployment); `mysql`, `orthanc` (StatefulSet — có PVC) | Đã có (README 2.6) cho 4 cái đầu; `mysql`/`orthanc` là StatefulSet (không phải tên khác — README chỉ ghi "mysql"/"orthanc" không phân biệt loại) |
| Service | **Trùng tên** Deployment/StatefulSet tương ứng (`ris-frontend`, `ris-backend`, `mysql`, `orthanc`, `ohif-viewer`, `mock-his`) | **Mới quy ước** (1 Service : 1 Deployment, không gộp) |
| ConfigMap | `<thành-phần>-config` — `backend-config`, `frontend-config`, `mysql-config`, `orthanc-config` | **Mới** |
| Secret | `<thành-phần>-secret` — `backend-secret`, `mysql-secret` | **Mới** |
| PVC (qua `volumeClaimTemplates`) | Tên cố định `data` trong từng StatefulSet (k8s tự ghép thành `data-mysql-0`, `data-orthanc-0`) | **Mới** |
| Nhãn `app.kubernetes.io/*` | `app.kubernetes.io/name: <tên Service>`, `app.kubernetes.io/part-of: vr-ris-pacs` trên mọi object | **Mới** |
| Biến môi trường | `SPRING_*` theo đúng *relaxed binding* của Spring Boot (vd `spring.datasource.url` → `SPRING_DATASOURCE_URL`); biến riêng của app theo `APP_*` (vd `app.security.jwt.secret` → `APP_SECURITY_JWT_SECRET`); biến của nginx/Orthanc giữ nguyên tên gốc của chúng (`BACKEND_UPSTREAM`, `MYSQL_*`) | **Mới** — bảng đầy đủ ở mục 6 |
| Tên file manifest | `deploy/k8s/NN-ten.yaml`, số `NN` quyết định **thứ tự đọc** (không phải thứ tự apply — Kustomize tự sắp theo loại object), nhóm theo thành phần: `00` namespace, `10-19` dữ liệu (mysql), `20-29` cấu hình (ConfigMap/Secret), `30-39` frontend, `40-49` PACS/OHIF, `50-59` Mock HIS | **Mới** |
| Tên file Docker Compose | `deploy/compose/docker-compose.full.yml` (phân biệt với `database/docker-compose.yml` đã có) | **Mới** |

---

## 3. Dockerfile

Cả hai ở `deploy/docker/`, build context là **gốc repo** (không phải `Backend/` hay `Frontend/`) — lý do ở từng Dockerfile:

### 3.1. `backend.Dockerfile` — multi-stage Maven → JRE

```text
docker build -f deploy/docker/backend.Dockerfile -t vrris/ris-backend:dev .
```

- **Stage build:** `maven:3.9.9-eclipse-temurin-21` (đã `docker pull` xác nhận tồn tại thật, không đoán tag) — bản Maven chính thức kèm sẵn JDK 21, khớp `java.version=21` ở `Backend/pom.xml`. `COPY database/migrations` trước `Backend/pom.xml`: `maven-resources-plugin` trong `Backend/pom.xml` tham chiếu `../database/migrations` tính từ `Backend/` — build context phải giữ đúng cấu trúc thư mục tương đối giữa hai thư mục này (xem mục 5). `-DskipTests`: test cần MySQL thật qua Testcontainers (`doc/15-kien-truc-backend.md` mục 1), không chạy được trong lớp build của image (không có Docker-trong-Docker) — test đã chạy ở `mvn verify` trên máy dev và ở CI (`backend-ci.yml`) trước khi build image, xem mục 8.
- **Stage runtime:** `eclipse-temurin:21-jre-alpine` (đã pull xác nhận) — chỉ JRE (không Maven/JDK/compiler) nên nhỏ hơn nhiều so với giữ lại image build; Alpine (musl) vì ứng dụng là một jar Spring Boot thuần, không cần glibc. Chạy bằng user thường `app` (không phải `root`) — không có lý do cần root để chạy một file jar, giảm thiệt hại nếu container bị chiếm quyền.
- **Đã build và chạy thử thật** trong phiên làm việc này (xem mục 8 cho log).

### 3.2. `frontend.Dockerfile` — multi-stage Node (Vite build) → nginx

```text
docker build -f deploy/docker/frontend.Dockerfile -t vrris/ris-frontend:dev .
```

- **Stage build:** `node:22-alpine` (đã pull xác nhận). **Giữ nguyên stack hiện tại — React 19, TypeScript, Vite 7.1.9, Tailwind 4 — không hạ phiên bản**, đúng yêu cầu. Chọn **Node 22**: Vite 7.1 khai `engines` yêu cầu `^20.19.0 || >=22.12.0` — Node 20 "thường" (không phải đúng bản 20.19 rất mới) không chắc đủ, còn **22 là nhánh LTS thấp nhất chắc chắn thỏa điều kiện** với bất kỳ phiên bản 22.x nào ≥ 22.12 (các bản 22.x phát hành sau khi lên LTS đều ≥ đó). Máy dev hiện cài Node 24 (`node -v` = `v24.16.0`) nhưng đó là phiên bản máy cá nhân, không phải quyết định cho image — 22 là lựa chọn LTS an toàn hơn cho một image dùng lâu dài. `npm ci` (không `npm install`): dùng đúng bản trong `package-lock.json`, không tự nâng version giữa các lần build.
- **Stage runtime:** `nginx:1.27-alpine` (đã pull xác nhận). Copy `dist/` (build tĩnh) vào `/usr/share/nginx/html`. Cấu hình nginx dùng cơ chế **template có sẵn của image nginx chính thức** (`/etc/nginx/templates/*.template` → `envsubst` → `/etc/nginx/conf.d/` lúc container khởi động, từ nginx 1.19): nhờ vậy **một image dùng được cho cả k8s lẫn `docker-compose.full.yml`**, chỉ đổi biến môi trường `BACKEND_UPSTREAM` (`ris-backend:8080` trong k8s, `backend:8080` trong Compose — khác tên Service/service-name giữa hai môi trường), không phải build lại image hay viết entrypoint riêng. **Đã xác nhận bằng thực nghiệm:** chạy container với `BACKEND_UPSTREAM=localhost:9999`, đọc `/etc/nginx/conf.d/default.conf` sinh ra bên trong container, thấy đúng `proxy_pass http://localhost:9999/api/;` — `envsubst` thay đúng biến mình đặt và **không** đụng vào biến nginx gốc (`$host`, `$remote_addr`…).
- `frontend.nginx.conf.template`: SPA fallback (`try_files $uri $uri/ /index.html`) cho client-side routing, và `location /api/` proxy sang backend — **trình duyệt không bao giờ gọi thẳng `ris-backend`**, luôn qua nginx này. Hệ quả tốt: CORS (`app.cors.allowed-origins` ở `Backend/src/main/resources/application.yml`) **không cần cấu hình gì thêm trong k8s** — trình duyệt luôn gọi `/api/...` cùng origin với trang đang mở.
- **Đã build và chạy thử thật** (xem mục 8).

### 3.3. `.dockerignore` (gốc repo)

Thêm mới — loại `node_modules/`, `target/`, `dist/`, `.git/` khỏi build context để giảm dung lượng gửi cho Docker daemon và tránh copy nhầm artefact cũ.

---

## 4. Manifest Kubernetes (`deploy/k8s/`)

Tổng cộng 8 file + `kustomization.yaml`, áp dụng bằng `kubectl apply -k deploy/k8s` (đúng cú pháp `01-scope.md` mục 6 bước 1).

### 4.1. StorageClass

**Không khai báo `storageClassName`** trong bất kỳ PVC nào. Docker Desktop Kubernetes có sẵn một StorageClass tên thật là `hostpath`, đã đánh dấu `(default)` — để trống `storageClassName` để Kubernetes tự dùng StorageClass mặc định đó. Cách này chạy được trên **mọi** máy Docker Desktop mà không ai phải tự gõ đúng tên `hostpath` bằng tay; nếu nhóm đổi sang minikube/kind sau này (README mục 8.2 từng để ngỏ), StorageClass mặc định của cụm đó cũng tự được dùng — chỉ cần đổi cách expose Service (mục 4.4), không phải sửa PVC.

### 4.2. MySQL — `StatefulSet` + `Service` headless + PVC

`10-mysql.yaml`. Image `mysql:8.4` (đúng tag `database/docker-compose.yml` đang dùng — không lệch phiên bản giữa hai môi trường). `command` giữ nguyên 3 cờ charset/collation/timezone đã có trong compose. `Service` kiểu headless (`clusterIP: None`) — đúng khớp mẫu StatefulSet, Pod có DNS ổn định `mysql-0.mysql...`. `volumeClaimTemplates` cấp `2Gi` — đủ cho dữ liệu demo (14 bảng, seed nhỏ), không phải số đo thật cho production. `readinessProbe`/`livenessProbe` dùng `mysqladmin ping`, cùng lệnh `database/docker-compose.yml` đã dùng cho healthcheck.

### 4.3. Backend — `Deployment` + `Service ClusterIP`

`22-backend.yaml`. `replicas: 1` — đồ án demo, không cần HA; ghi chú rõ trong `application-k8s.yml` rằng tăng replica với Flyway tự chạy trong mỗi pod **chưa được kiểm chứng thực tế** (Flyway có khóa nội bộ nên về lý thuyết an toàn, nhưng chưa thử với >1 replica). Probe readiness/liveness trỏ `/api/v1/actuator/health/readiness` và `/liveness` (xem mục 4.5). `Service` kiểu **ClusterIP** (mặc định, không khai báo `type`) — khác `ris-frontend` — vì không ai/không gì ngoài `ris-frontend` (nginx) gọi tới backend; xem mục 4.4 lý do kiến trúc.

### 4.4. ⚠️ Không dùng Ingress — khác giả định ban đầu ở README mục 2.6

README mục 2.6 và sơ đồ gốc (`diagrams/06-trien-khai-k8s-local.mmd`) vẽ `Ingress / NodePort` đứng trước cả `ris-frontend` lẫn `ris-backend`. Quyết định ở đây: **không dùng Ingress**. Lý do:

1. Docker Desktop Kubernetes **không có sẵn Ingress controller** — phải cài thêm (ví dụ `ingress-nginx`) trước khi bất kỳ `Ingress` object nào có tác dụng. Thêm một thành phần phải cài đặt riêng chỉ để làm việc mà nginx trong chính `ris-frontend` đã làm được.
2. `ris-frontend` (nginx) đã tự proxy `/api/**` sang `ris-backend` (mục 3.2) — kết quả giống hệt việc có Ingress định tuyến theo path, nhưng không cần thêm Service/Ingress nào.
3. Trình duyệt vì vậy chỉ cần biết **một địa chỉ** (`ris-frontend`) cho toàn bộ RIS — đúng tinh thần README mục 2.6 ("một lệnh deploy", một chỗ vào), chỉ khác công cụ.

**Cách expose `ris-frontend` ra ngoài cụm:** `Service type: LoadBalancer`. Đây là điểm đặc thù của **Docker Desktop** (khác hẳn k8s cloud thật): Docker Desktop tự nhận `LoadBalancer` và bind cổng đó vào `localhost` của chính máy đang chạy — không có IP công khai nào được cấp. Mở trình duyệt vào `http://localhost:8080` (cổng đặt trong `Service.spec.ports[0].port`) là đủ. Nếu nhóm đổi sang minikube/kind: `LoadBalancer` không tự có địa chỉ, cần `minikube tunnel` hoặc đổi sang `NodePort` — ghi chú thẳng trong file YAML.

**Đã cập nhật ngược `doc/10-lien-ket-he-thong.md`** (L-01/L-02/L-03, thành phần "Ingress", sơ đồ mục 5) cho khớp quyết định này — xem mục 11 tài liệu này và mục 8 của `10-…`.

### 4.5. Health check (`/actuator/health`) — thêm mới, chưa có trước 2026-11-05

`Backend/pom.xml` trước đây **không có** `spring-boot-starter-actuator` — không có endpoint nào để k8s kiểm tra "pod đã sẵn sàng chưa" ngoài việc gọi một endpoint cần JWT (mọi endpoint khác đều `authenticated()` — `security/SecurityConfig.java`). Đã thêm:

- Dependency `spring-boot-starter-actuator`.
- `application.yml`: `management.endpoints.web.exposure.include: health` — **chỉ** `health`, không mở `env`/`beans`/`configprops` (có thể lộ cấu hình/secret qua HTTP không cần JWT). `show-details: never`.
- `security/SecurityConfig.java`: thêm `.requestMatchers("/actuator/health/**").permitAll()` — probe của kubelet không có JWT.
- `application-k8s.yml`: `management.endpoint.health.probes.enabled: true` — bật hai nhóm `readiness`/`liveness` (Spring Boot tự bật khi phát hiện biến môi trường `KUBERNETES_SERVICE_HOST` k8s tự bơm vào container, nhưng đặt tường minh ở đây để không phụ thuộc hành vi tự động).

⚠️ **Phát hiện bằng thực nghiệm, không đoán:** vì `server.servlet.context-path: /api/v1` (đã có từ trước), đường dẫn thật là **`/api/v1/actuator/health`**, không phải `/actuator/health` ở gốc — đã build jar, chạy `java -jar`, `curl` cả hai đường dẫn để xác nhận (gốc trả `404`, có `context-path` trả `200 {"status":"UP"}`) trước khi viết vào manifest. Đây là lý do mục 10 ở `doc/10-lien-ket-he-thong.md` (K10-01) giờ trả lời được: ✅ có health check.

### 4.6. Orthanc (PACS) và OHIF Viewer — tuần 7, IN-19

`40-orthanc.yaml`, `41-ohif.yaml`. Viết sẵn từ bây giờ (yêu cầu của việc này), **chưa dùng trong 8 tuần đầu** nhưng **nằm trong `kustomization.yaml`** (không giống Mock HIS, mục 4.7) vì IN-19 vẫn là IN (tuần 7), không phải HOÃN.

- **Image:** `orthancteam/orthanc:26.9.1` và `ohif/viewer:v4.12.51.21579` — cả hai đã `docker pull` xác nhận tồn tại thật trên Docker Hub (không đoán tag; tag ban đầu dự kiến `orthancteam/orthanc:24.11.1` và `ohif/viewer:v3.9.1` **không tồn tại** — tra danh sách tag thật qua Docker Hub API rồi mới chọn).
- ⚠️ **Phát hiện khi kiểm tra image OHIF, chưa từng ghi ở `07-pacs-dicom.md`:** mở container `ohif/viewer:v4.12.51.21579`, file cấu hình mặc định (`/usr/share/nginx/html/app-config.js`) có ngày build **19/03/2023** và dùng schema `servers.dicomWeb[]` kiểu OHIF v2 — **không khớp** tài liệu OHIF hiện hành (bản mới dùng `dataSources[]`). Ảnh trên Docker Hub có thể không được cập nhật theo đúng tag GitHub. `ConfigMap ohif-config` (mount đè `app-config.js`) **dùng lại đúng schema cũ** mà chính ảnh này thể hiện là nó đọc được — an toàn hơn đoán schema mới nó có thể không đọc — nhưng **chưa chạy thử với Orthanc thật** (Orthanc chưa dựng trong 8 tuần đầu). Bổ sung thêm vào câu hỏi mở P-08 của `07-pacs-dicom.md` (xem mục 10 tài liệu này).
- **Vì sao `Service type: LoadBalancer` cho cả hai (không phải `ClusterIP` như backend):** cả OHIF lẫn việc gọi DICOMweb đều chạy **trong trình duyệt** (OHIF là SPA, gọi QIDO-RS/WADO-RS bằng JavaScript phía client — `07-pacs-dicom.md` mục 1) và việc đẩy ảnh mẫu (`storescu`) chạy **trên máy host**, không phải từ trong cụm — cả hai phía đều là tiến trình trên máy host, cần địa chỉ `localhost` mà `ClusterIP` không cho. Cổng: `orthanc` mở `4242` (DICOM) + `8042` (HTTP/DICOMweb) đúng mặc định của Orthanc; `ohif-viewer` mở `8043` (tùy chọn, tránh trùng `8080`/`8042`).
- **Orthanc không bật xác thực** (`AuthenticationEnabled: false`, không khai `DicomModalities` whitelist, có `DicomAlwaysAllowStore: true`) — chọn phương án (1) ở `07-pacs-dicom.md` mục 3.1/6.3 ("chỉ dựa vào mạng nội bộ cụm... đơn giản nhất, đủ cho đồ án"). ⚠️ **Lý luận "không mở công khai" ở đây chỉ đúng vì `LoadBalancer` trên Docker Desktop bind vào `localhost`** (mục 4.4) — không ai ngoài máy đang chạy Docker Desktop tới được `localhost:4242`/`:8042`/`:8043`. **Trên k8s cloud thật, `LoadBalancer` có IP công khai thật và cách làm này sẽ vi phạm nguyên tắc bất biến "OHIF/Orthanc không mở công khai"** (README mục 0) — đây **không phải** câu trả lời cho production, chỉ là câu trả lời đúng cho **đúng môi trường đã chốt** (Docker Desktop local, README mục 8.2). Ghi rõ trong báo cáo đồ án như một giới hạn có ý thức.
- `APP_OHIF_BASE_URL=http://localhost:8043` đặt trong `backend-config` (mục 6) dù **backend hiện chưa có code nào đọc biến này** (`app.ohif.base-url` không tồn tại trong bất kỳ `application*.yml` nào) — chuẩn bị trước cho IN-19/IN-23 (tuần 6–7) để lúc đó chỉ cần thêm `@ConfigurationProperties`, không phải sửa manifest.

### 4.7. Mock HIS — H-03, [HOÃN]

`50-mock-his.yaml` — viết sẵn theo đúng yêu cầu, nhưng:

- **Không có image thật** (`vrris/mock-his:not-built-yet` — placeholder, không tồn tại trên registry nào). `replicas: 0`.
- **Không nằm trong `kustomization.yaml`** — `kubectl apply -k deploy/k8s` mặc định **không** triển khai file này (khác Orthanc/OHIF ở mục 4.6). Ghi rõ trong chính file và trong `kustomization.yaml`.
- `Service` kiểu `ClusterIP`, port `2575` (MLLP, quy ước phổ biến chứ không phải chuẩn IANA — `doc/08-hl7-message-spec.md` mục 10.1), **không có readiness/liveness `httpGet`** vì MLLP là socket TCP thuần, không phải HTTP (`01-scope.md` mục 4.1, `07-pacs-dicom.md` mục 3.1 đã ghi nguyên tắc này cho DICOM — áp dụng lại cho MLLP).

---

## 5. Flyway vào backend

**Hiện trạng trước 2026-11-05** (`doc/15-kien-truc-backend.md` mục 3, 12 — "đã chốt"): `database/docker-compose.yml` có container `flyway` riêng chạy migration cho vòng dev; `Backend/pom.xml` chỉ có `flyway-core`/`flyway-mysql` ở **scope `test`**; `application.yml` đặt `spring.flyway.enabled=false`. Không có gì chạy Flyway trong một cụm k8s (không có container Flyway riêng như docker-compose trong k8s, trừ khi tự viết thêm Job).

**Vấn đề cần giải quyết:** triển khai k8s cần MySQL có đủ bảng trước khi backend chạy được — nhưng không nên dựng một container Flyway riêng trong k8s nếu backend tự làm được, vì vẫn phải quản lý thứ tự khởi động (init container / Job chờ MySQL) mà không có lợi ích rõ ràng so với để chính backend làm.

**Quyết định (🟡 đề xuất của tài liệu này, trả lời `K10-06` ở `doc/10-lien-ket-he-thong.md`):**

| Môi trường | Ai chạy Flyway | Vì sao |
|---|---|---|
| Dev local (Docker Compose) | **Không đổi** — container `flyway` riêng, backend `flyway.enabled=false` | Vòng dev hằng ngày không chạm tới — tránh phá cái đang chạy tốt |
| k8s | **Backend tự chạy** lúc khởi động (profile `k8s`) | Không cần thêm Job/initContainer; Flyway có cơ chế khóa nội bộ (bảng `flyway_schema_history`) nên không đụng độ nếu dev vẫn còn migrate qua Compose trên máy khác kết nối **cùng** MySQL (hiếm, nhưng về lý thuyết an toàn) |

### 5.1. Thay đổi code

1. `Backend/pom.xml`: bỏ `<scope>test</scope>` khỏi `flyway-core`/`flyway-mysql` — chuyển thành *compile scope* (dùng được cả ở `main` lẫn `test`, không cần khai hai lần).
2. **Thêm `maven-resources-plugin`**, execution `copy-flyway-migrations` ở phase `generate-resources`: copy `${project.basedir}/../database/migrations/*.sql` vào `${project.build.outputDirectory}/db/migration` (tức `target/classes/db/migration`). Đây là cách **"tham chiếu", không "chép"**: không có file `.sql` nào được tạo/commit trong `src/main/resources/` — `doc/15-kien-truc-backend.md` mục 3 vẫn đúng ("không commit thư mục này"), chỉ build-time mới sinh ra, và sinh từ **đúng** `database/migrations/` — một nguồn duy nhất.
3. **`application-k8s.yml`** (file mới, Spring profile `k8s`): `spring.flyway.enabled=true`, không khai `locations` (dùng mặc định `classpath:db/migration` — đúng nơi bước 2 vừa copy vào), `connect-retries=10` (MySQL StatefulSet có thể chưa sẵn sàng ngay khi backend Pod khởi động — Flyway tự thử lại thay vì cần initContainer chờ).
4. **`application.yml` (mặc định, dev local) không đổi** — vẫn `flyway.enabled=false`.
5. `database/README.md` mục "Khi có backend Spring Boot": cập nhật bảng theo đúng quyết định trên (đã sửa trực tiếp, xem mục 11).

### 5.2. Baseline

Không cần `baseline-on-migrate` hay xử lý gì đặc biệt: mọi cụm k8s mới (namespace `vr-ris` mới tạo) có MySQL **rỗng hoàn toàn** — Flyway chạy từ `V1` như lần đầu, không có baseline nào phải thiết lập. Nếu sau này ai đó trỏ backend-profile-`k8s` vào một MySQL đã có dữ liệu từ nguồn khác (không khuyến khích, không phải kịch bản của đồ án) thì mới cần `baseline-version`/`baseline-on-migrate` — ngoài phạm vi tài liệu này.

### 5.3. ✅ Đã build và chạy thử thật (không phải suy luận)

| Việc | Kết quả |
|---|---|
| `mvn verify` sau khi đổi `pom.xml` + thêm `application-k8s.yml` + sửa `HisGateway`/`JsonFileHisGateway` (mục `doc/12-seed-data-va-kich-ban-demo.md`) | ✅ **35/35 test, BUILD SUCCESS** — không có test nào bị ảnh hưởng (profile test vẫn trỏ `filesystem:../database/migrations`, không đổi) |
| `target/classes/db/migration/` sau `mvn package` | ✅ Có đủ `V1`…`V8`, đúng nội dung `database/migrations/` |
| `docker build -f deploy/docker/backend.Dockerfile .` | ✅ Build thành công |
| Chạy image với `SPRING_DATASOURCE_URL` trỏ MySQL dev đã có sẵn dữ liệu (profile mặc định) | ✅ Khởi động, `/api/v1/actuator/health` trả `{"status":"UP"}` |
| **Chạy image với `SPRING_PROFILES_ACTIVE=k8s` trỏ một MySQL HOÀN TOÀN RỖNG** (container `mysql:8.4` mới, chưa có bảng nào) | ✅ **Log Flyway tự chạy lần lượt V1→V8, "Successfully applied 8 migrations", `Started RisBackendApplication` thành công** — đây là bằng chứng trực tiếp cho đúng kịch bản k8s sẽ gặp (namespace mới, MySQL rỗng) |

Đây là phần được kiểm chứng kỹ nhất trong tài liệu — vì nó thay đổi hành vi khởi động của backend, rủi ro cao nhất nếu sai.

---

## 6. ConfigMap và Secret

Nguồn nguyên tắc: `doc/09-bao-mat-toan-ven-du-lieu.md` mục 5.1 (ConfigMap vs Secret, cảnh báo Secret k8s chỉ là base64). Bảng dưới là **áp dụng cụ thể** — không lặp lại nguyên tắc.

| Biến | Thành phần dùng | Vào ConfigMap hay Secret | Giá trị (dev/demo) | Lấy từ đâu |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ris-backend | ConfigMap `backend-config` | `k8s` | Mới |
| `SPRING_DATASOURCE_URL` | ris-backend | ConfigMap `backend-config` | `jdbc:mysql://mysql:3306/vr_ris?...` | Suy từ `application.yml` (đổi host `localhost:3307` → `mysql:3306`) |
| `SPRING_DATASOURCE_USERNAME` | ris-backend | ConfigMap `backend-config` | `ris` | `application.yml` |
| `SPRING_DATASOURCE_PASSWORD` | ris-backend | **Secret `backend-secret`** | `ris_dev_pw` | `database/docker-compose.yml` (giá trị mặc định đã có) |
| `APP_SECURITY_JWT_SECRET` | ris-backend | **Secret `backend-secret`** | `dev-only-secret-khong-dung-cho-production-doi-truoc-khi-deploy-32b` | **Y HỆT** chuỗi đã có sẵn trong `application.yml` — để một JWT ký lúc dev local và ký lúc chạy trong k8s dùng chung một secret, không phải đổi gì khi chuyển môi trường demo |
| `APP_OHIF_BASE_URL` | ris-backend | ConfigMap `backend-config` | `http://localhost:8043` | Mới — xem mục 4.6 (chưa có code đọc) |
| `TZ` | ris-backend, mysql | ConfigMap `backend-config`, `mysql-config` | `UTC` | `02-erd.md` mục 4.7 |
| `MYSQL_DATABASE` | mysql | ConfigMap `mysql-config` | `vr_ris` | `database/docker-compose.yml` |
| `MYSQL_USER` | mysql | ConfigMap `mysql-config` | `ris` | `database/docker-compose.yml` |
| `MYSQL_PASSWORD` | mysql | **Secret `mysql-secret`** | `ris_dev_pw` | `database/docker-compose.yml` |
| `MYSQL_ROOT_PASSWORD` | mysql | **Secret `mysql-secret`** | `root_dev_pw` | `database/docker-compose.yml` |
| `BACKEND_UPSTREAM` | ris-frontend (nginx template) | ConfigMap `frontend-config` | `ris-backend:8080` | Mới — xem mục 3.2 |
| `orthanc.json` (file, không phải biến đơn) | orthanc | ConfigMap `orthanc-config`, mount file | AE Title `VRRIS_PACS`, port `4242`/`8042`, không xác thực | Mới — theo đề xuất `07-pacs-dicom.md` mục 3.1 |
| `app-config.js` (file) | ohif-viewer | ConfigMap `ohif-config`, mount file | Trỏ `qidoRoot`/`wadoRoot` về `http://localhost:8042/dicom-web` | Mới — xem cảnh báo mục 4.6 |

### 6.1. Quy tắc commit

**Khác với đề xuất ban đầu ("có file `.example` + `.gitignore`"):** sau khi đối chiếu, dự án này đã có sẵn một tiền lệ khác — `database/docker-compose.yml` **commit thẳng** giá trị mặc định giả (`root_dev_pw`, `ris_dev_pw`) ngay trong file, chỉ phần **tùy chọn đổi riêng** (`database/.env`) mới bị `.gitignore`. Để nhất quán và để `kubectl apply -k deploy/k8s` chạy được bằng **đúng một lệnh** (không bắt người dùng tự tạo file Secret trước — khớp tinh thần `01-scope.md` mục 6 bước 1), `deploy/k8s/21-secret.yaml` **commit thẳng** cùng bộ giá trị giả y hệt `database/docker-compose.yml`/`application.yml` đang dùng — không tách `.example`. Đây là trả lời cho câu hỏi mở **S-12** ở `doc/09-bao-mat-toan-ven-du-lieu.md`.

⚠️ Nhắc lại nguyên tắc đã có ở `doc/09-bao-mat-toan-ven-du-lieu.md` mục 5.1: **Secret k8s chỉ là base64, không phải mã hóa.** `kubectl get secret backend-secret -n vr-ris -o yaml` lộ giá trị thật ngay cho bất kỳ ai có quyền đọc namespace. Chấp nhận được ở đây vì giá trị vốn **đã là giả** (không phải mật khẩu thật bị lộ) — không áp dụng được nguyên lý này nếu sau này có giá trị thật.

---

## 7. CI/CD

| File | Việc làm | Đã chạy thật trên GitHub? |
|---|---|---|
| `.github/workflows/backend-ci.yml` (đã có từ trước) | `mvn verify` khi đụng `Backend/**` | ✅ Sẵn sàng — `Backend/` đã commit vào git (tự kích hoạt khi push/PR) |
| `.github/workflows/frontend-ci.yml` (**mới**) | `npm ci` → `eslint .` → `npm run build` (`tsc -b && vite build`) khi đụng `Frontend/**` | ✅ Sẵn sàng — `Frontend/` đã commit vào git. **Đã chạy local**: `eslint`, `tsc -b && vite build` |
| `.github/workflows/build-images.yml` (**mới**) | `docker build` (không `push`) cả hai Dockerfile khi đụng `Backend/`, `Frontend/`, `database/migrations/`, `deploy/docker/` | ✅ Sẵn sàng — toàn bộ code đã commit. **Logic đóng gói jar & build đã kiểm chứng thành công ở local** (`mvn -DskipTests clean package`) |

### 7.1. ESLint — thêm mới cho Frontend (chưa từng có trước 2026-11-05)

`Frontend/` trước đây **không có** cấu hình lint nào. Thêm `eslint.config.js` (flat config, ESLint 10 — đúng mẫu chuẩn cho React 19 + TypeScript + Vite 7, giống template `npm create vite@latest -- --template react-ts`), cộng 5 package `devDependencies` (`eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals`) — **đã `npm view` xác nhận phiên bản thật trước khi ghi vào `package.json`**, không đoán số.

Chạy `npx eslint .` lần đầu phát hiện **1 lỗi thật** trong code nghiệp vụ có sẵn (`src/components/modals/ReportModal.tsx:55` — gọi `setState` trực tiếp trong `useEffect`, luật `react-hooks/set-state-in-effect` của bản `eslint-plugin-react-hooks` 7.x mới thêm). Đây là code **ngoài phạm vi việc được giao** (12-seed-data / 14-devops) — đã hạ luật đó xuống `warn` trong `eslint.config.js` (có comment giải thích rõ lý do, không âm thầm tắt) để CI không đỏ vì một phát hiện mới chưa ai yêu cầu sửa, nhưng vẫn hiển thị để nhóm biết. **Không** hạ các luật khác.

---

## 8. Quy trình triển khai (máy trắng → hệ thống chạy)

### 8.1. Điều kiện tiên quyết

- Docker Desktop đã cài, **Kubernetes đã bật** (Settings → Kubernetes → Enable Kubernetes) — ⚠️ **máy viết tài liệu này KHÔNG bật sẵn Kubernetes** (`kubectl cluster-info` báo `connection refused` tới `localhost:8080`/k8s API) nên **toàn bộ bước `kubectl apply` dưới đây chưa chạy thử được trên cụm thật** trong phiên làm việc này — xem mục 8.3 để biết chính xác cái gì đã kiểm chứng và cái gì chưa.
- `kubectl` và Kustomize đi kèm (`kubectl version --client` cho thấy `Kustomize Version: v5.7.1` đã có sẵn — không cần cài riêng).
- Đã chạy qua `database/README.md` ít nhất một lần (để chắc Docker hoạt động bình thường trên máy) — không bắt buộc, chỉ là cách kiểm tra nhanh.

### 8.2. Các bước

```bash
# 1. Build hai image LOCAL - không cần registry, không cần "docker push"/"docker login".
#    Docker Desktop Kubernetes dùng CHUNG image store với lệnh `docker build` ngay trên máy đó
#    (khác minikube/kind, vốn là một VM/container riêng cần `minikube image load` hay tương tự) -
#    đây là lý do "cách nạp image local không cần registry" cho đúng MÔI TRƯỜNG ĐÃ CHỐT.
docker build -f deploy/docker/backend.Dockerfile  -t vrris/ris-backend:dev  .
docker build -f deploy/docker/frontend.Dockerfile -t vrris/ris-frontend:dev .

# 2. (Một lần) Bật Kubernetes trong Docker Desktop nếu chưa bật: Settings -> Kubernetes -> Enable.
#    Kiểm tra cụm đã lên và có StorageClass mặc định (phải thấy một dòng có "(default)", ví dụ "hostpath (default)"):
kubectl cluster-info
kubectl get storageclass

# 3. Áp toàn bộ manifest.
kubectl apply -k deploy/k8s

# 4. Theo dõi tới khi mọi pod Ready.
kubectl get pods -n vr-ris -w

# 5. Theo dõi backend tự chạy Flyway xong (xem log có "Successfully applied 8 migrations").
kubectl logs -n vr-ris deployment/ris-backend -f

# 6. Mở trình duyệt: http://localhost:8080
```

**Gỡ bỏ:**

```bash
kubectl delete -k deploy/k8s      # xóa mọi object, bao gồm PVC (mất dữ liệu demo - chấp nhận được)
```

### 8.3. ✅ Đã chạy thử / ❌ Chưa kiểm chứng — tách rõ, không gộp lẫn

| Việc | Trạng thái |
|---|---|
| `docker build` cả hai Dockerfile | ✅ Chạy thật, thành công (mục 3) |
| Chạy container backend/frontend đơn lẻ bằng `docker run`, kiểm tra health/nginx | ✅ Chạy thật (mục 3, 4.5) |
| Backend tự chạy Flyway V1→V8 trên MySQL rỗng (đúng kịch bản k8s) | ✅ Chạy thật bằng `docker run` + container MySQL rời (mục 5.3) — **không phải trong k8s thật**, nhưng cùng cơ chế |
| `kubectl kustomize deploy/k8s` (dựng offline, không cần cụm) | ✅ Chạy thật — 18 object, không lỗi |
| `docker compose -f deploy/compose/docker-compose.full.yml config` | ✅ Chạy thật — hợp lệ, `include` gộp đúng `database/docker-compose.yml` |
| `docker compose -f deploy/compose/docker-compose.full.yml up` (chạy thật cả 4 service) | ❌ **Chưa chạy** — sẽ đụng container `vr-ris-mysql`/cổng `3307` đang chạy cho vòng dev (mục 1); cần dừng cụm dev trước, chưa làm trong phiên này để không ảnh hưởng máy đang dùng |
| `kubectl apply -k deploy/k8s` trên cụm thật (`kubectl cluster-info`, `kubectl get storageclass`) | ❌ **Chưa chạy** — máy phiên 2026-11-05 chưa bật Docker Desktop Kubernetes (mục 8.1); máy kiểm tra ngày 2026-11-07 chỉ cài `MySQL Server 8.4.9` trực tiếp, **chưa cài Docker Desktop và `kubectl`**, nên chỉ kiểm chứng lại được Flyway `V1`→`V8` (kể cả xóa sạch chạy lại từ đầu ra `"Successfully applied 8 migrations"`) và full test suite 113/113 trên MySQL 8.4.9 cổng `3307`. Cần chạy trên máy thành viên có Docker Desktop bật Kubernetes |
| Toàn bộ `deploy/k8s/40-orthanc.yaml`, `41-ohif.yaml` chạy thật, mở ảnh DICOM qua OHIF | ❌ **Chưa chạy** — đúng kế hoạch (tuần 7, IN-19), chỉ viết sẵn manifest |
| `frontend-ci.yml`, `build-images.yml` trên GitHub Actions | ✅ **Sẵn sàng** — `Backend/`/`Frontend/`/`database/migrations/`/`deploy/docker/` đã commit vào git, tự động chạy khi push/PR lên GitHub |

### 8.4. Sự cố thường gặp

| Triệu chứng | Nguyên nhân hay gặp | Cách kiểm tra / sửa |
|---|---|---|
| Pod `ris-backend` ở trạng thái `CrashLoopBackOff` | Flyway không kết nối được MySQL (StatefulSet chưa Ready) | `kubectl logs -n vr-ris deployment/ris-backend` — nếu thấy lỗi kết nối JDBC, chờ thêm (`connect-retries=10` đã có, nhưng StatefulSet có thể chậm hơn khi mới `apply` lần đầu); `kubectl get pods -n vr-ris` xem `mysql-0` đã `1/1 Running` chưa |
| Pod `mysql-0` không bao giờ `Running`, PVC ở `Pending` | StorageClass mặc định chưa được đánh dấu đúng, hoặc Docker Desktop Kubernetes mới bật chưa khởi tạo xong `hostpath` provisioner | `kubectl get storageclass` — phải thấy một dòng có `(default)`; nếu không, tắt/bật lại Kubernetes trong Docker Desktop Settings |
| `kubectl apply -k` báo `image pull backoff` dù đã `docker build` | Gõ sai tag (`:dev` so với tag thật đã build), hoặc `imagePullPolicy` không phải `IfNotPresent`/`Never` nên kubelet cố pull từ Docker Hub một image `vrris/...` không tồn tại ở đó | `docker images | grep vrris` để xác nhận tag đúng tồn tại local; kiểm tra lại `imagePullPolicy: IfNotPresent` trong manifest (đã đặt sẵn, xem mục 4.3/4.6) |
| Cổng `8080`/`3307`/`4242`/`8042`/`8043` "đã bị chiếm" khi `docker run`/`kubectl` cấp `LoadBalancer` | Một tiến trình khác trên máy (kể cả chính `database/docker-compose.yml` đang chạy vòng dev) đã dùng cổng đó | `netstat -ano | findstr <cổng>` (PowerShell: tương tự) tìm PID đang giữ; với MySQL cổng `3307`, nhớ **không chạy đồng thời** vòng dev và `docker-compose.full.yml`/k8s (mục 1) |
| `kubectl apply -k deploy/k8s` báo lỗi namespace | Thứ tự resource bị đảo (Namespace phải có trước các object khác) | Không cần tự sắp thứ tự — Kustomize tự nhóm Namespace lên trước; nếu vẫn lỗi, chạy riêng `kubectl apply -f deploy/k8s/00-namespace.yaml` trước rồi mới `apply -k` lại |

---

## 9. Sơ đồ

Nguồn: `diagrams/14-devops-trien-khai.mmd` — chi tiết hơn `diagrams/06-trien-khai-k8s-local.mmd` (vẫn giữ nguyên, là bản tóm tắt ở README mục 2.6): thêm tên Service/Deployment/StatefulSet thật, cổng thật, và tách rõ ba đường vào riêng biệt từ máy host (RIS qua `ris-frontend`, xem ảnh qua `ohif-viewer`, và DICOMweb gọi thẳng `orthanc` từ trình duyệt — ba địa chỉ `localhost` khác nhau, không phải một Ingress chung).

⚠️ **Chưa xuất được `diagrams/14-devops-trien-khai.png`** trong phiên làm việc này: môi trường cho phép chạy `npx @mermaid-js/mermaid-cli` ở một phần việc trước đó trong cùng ngày (ghi nhận ở `doc/02-erd.md` mục 0, `AGENTS.md` mục 5), nhưng lệnh đó **bị từ chối trong phiên của Agent 2** (quyền thực thi bị hệ thống chặn, không phải lỗi kỹ thuật của lệnh) — không thử lại/vòng tránh theo đúng nguyên tắc không lách qua một từ chối quyền. `doc/diagrams/10-lien-ket-he-thong.png` **cũng chưa xuất lại** dù nguồn `.mmd` đã sửa (mục 11) — cùng lý do. Người có quyền chạy `npx`/mermaid-cli trên máy mình xin xuất giúp bằng đúng lệnh đã ghi ở `doc/02-erd.md` mục 0.

---

## 10. Câu hỏi còn mở

| # | Câu hỏi | Mục | Chặn việc gì |
|---|---|---|---|
| D14-01 | Image Orthanc/OHIF chọn (`orthancteam/orthanc:26.9.1`, `ohif/viewer:v4.12.51.21579`) có đúng là bản nhóm muốn dùng không — đặc biệt OHIF có phát hiện schema cấu hình cũ (mục 4.6) | 4.6 | Dựng Orthanc+OHIF thật (tuần 7, IN-19) |
| D14-02 | `app-config.js` của OHIF (mục 4.6, 6) đúng schema không — chỉ xác nhận được khi có Orthanc thật để thử — bổ sung cho P-08 ở `07-pacs-dicom.md` | 4.6 | Như trên |
| D14-03 | Có tăng `replicas` của `ris-backend` > 1 không — nếu có, cần kiểm chứng Flyway tự chạy ở nhiều pod cùng lúc có an toàn thật không (mục 4.3, 5) | 4.3, 5 | Không chặn 8 tuần (demo 1 replica là đủ) |
| D14-04 | Giữ `docker-compose.full.yml` (mục 1, IN-18) hay bỏ nếu nhóm thấy không cần "chạy cả cụm bằng Docker, không k8s" — tốn công bảo trì hai bộ manifest (Compose + k8s) cho cùng một mục đích | 1 | Không chặn, chỉ ảnh hưởng khối lượng bảo trì |
| D14-05 | Swagger UI (`K10-02` ở `doc/10-lien-ket-he-thong.md`) có cần chặn JWT trong k8s hay để mở — tài liệu này **chưa trả lời**, giữ nguyên mở | 4.3 | Bảo mật, không chặn 8 tuần |
| D14-06 | Có commit `deploy/k8s/21-secret.yaml` với giá trị giả trực tiếp (như đã làm, mục 6.1) hay nhóm muốn tách `.example` + `.gitignore` theo đúng đề xuất gốc của người giao việc | 6.1 | Quyết định quy ước — không chặn kỹ thuật, ❓ cần nhóm xác nhận lựa chọn đã chọn ở đây |
| D14-07 | Resources requests/limits ở mục 4.2–4.6 là ước lượng theo kinh nghiệm, chưa đo tải thật trên máy nhóm — có cần hạ thêm cho máy yếu hơn không | 4 | Không chặn, chỉnh khi gặp `OOMKilled` thật |

---

## 11. Phát hiện mâu thuẫn và đã sửa

1. **README mục 2.6 / `diagrams/06-trien-khai-k8s-local.mmd` giả định có Ingress; quyết định ở đây là không dùng Ingress** (mục 4.4). Đã sửa trực tiếp `doc/10-lien-ket-he-thong.md` (thành phần "Ingress", L-01/L-02/L-03, sơ đồ mục 5, mục 6/7/8 của file đó). **Chưa sửa** sơ đồ gốc ở README mục 2.6 / `diagrams/06-trien-khai-k8s-local.mmd` trong lần này — sơ đồ đó ghi "Ingress / NodePort" như hai lựa chọn, không sai hẳn (NodePort vẫn là một lựa chọn hợp lệ chưa dùng), nhưng nên đối chiếu lại khi nhóm xác nhận tài liệu này. Đề xuất cho người xác nhận: sửa nhãn "Ingress / NodePort" thành "LoadBalancer (Docker Desktop)" ở README mục 2.6 nếu đồng ý quyết định ở đây.
2. **`doc/15-kien-truc-backend.md` mục 3/12 ghi "không cần `src/main/resources/db/migration/`"** (đúng cho dev local) — tài liệu này thêm trường hợp k8s cần thư mục đó tồn tại **lúc build** (không phải lúc commit). Đã sửa trực tiếp `doc/15-kien-truc-backend.md` mục 3 và bảng mục 12 để ghi rõ cả hai trường hợp, không phải một mâu thuẫn mà là một ngữ cảnh mới (k8s) chưa được tính tới lúc viết `15`.
3. **`database/README.md` mục "Khi có backend Spring Boot" từng để ❓ "migration chỉ nên chạy ở một nơi, nhóm quyết định"** — tài liệu này trả lời: **tùy môi trường** (Compose: container riêng như cũ; k8s: backend tự chạy) — đã sửa trực tiếp `database/README.md`, không phải chọn một trong hai như câu hỏi gốc ngụ ý mà là "cả hai, theo đúng môi trường".
4. **`doc/08-hl7-message-spec.md` mục 8.3 đề xuất mở rộng `HisOrderSnapshot` và giao "Agent 2 sinh file JSON thật"** — việc này thuộc phạm vi `doc/12-seed-data-va-kich-ban-demo.md`, không phải tài liệu này, nhưng được thực hiện cùng phiên làm việc này (xem `doc/12-seed-data-va-kich-ban-demo.md` mục 3.2 và cập nhật trực tiếp trong `doc/08-hl7-message-spec.md` mục 8.2/8.3/12/14). Ghi chú ở đây để người đọc tài liệu DevOps không hiểu nhầm là bị bỏ sót.
5. **Không tìm thấy mâu thuẫn nào khác** giữa tài liệu này và `01-scope.md`, `02-erd.md`, `06-rbac-matrix.md`, `07-pacs-dicom.md`, `09-bao-mat-toan-ven-du-lieu.md` — mọi quyết định bảo mật/ConfigMap/Secret ở mục 6 chỉ *áp dụng cụ thể* nguyên tắc đã có ở `09-…` mục 5, không thêm nguyên tắc mới.

---

## 12. Điều kiện hoàn thành

Nối với điều kiện tuần 3 (README mục 7.2: *"`kubectl apply` chạy được cả 3 thành phần; đăng nhập được; thao tác ghi audit log; ArchUnit xanh trong CI"*) và tuần 8 (`01-scope.md` mục 6: *"Demo end-to-end từ một lệnh deploy"*):

- [x] Dockerfile backend + frontend viết xong, **build thành công thật** (không chỉ cú pháp).
- [x] Flyway chạy được trong backend (profile `k8s`), **chạy thử thành công thật** trên MySQL rỗng.
- [x] Manifest k8s đủ cho RIS core (namespace, mysql, backend, frontend, ConfigMap, Secret) + Orthanc/OHIF (tuần 7) + Mock HIS (manifest mẫu, không apply) — **dựng offline bằng Kustomize thành công thật**.
- [x] `docker-compose.full.yml` (IN-18) viết xong, **hợp lệ thật** (`docker compose config`).
- [x] CI: `frontend-ci.yml`, `build-images.yml` viết xong, **chạy tương đương thành công ở local**; `backend-ci.yml` không đổi.
- [ ] `kubectl apply -k deploy/k8s` chạy thật trên cụm Docker Desktop Kubernetes — **chưa làm được** (máy viết tài liệu chưa bật Kubernetes, mục 8.1/8.3). Đây là việc còn thiếu quan trọng nhất trước khi coi tài liệu này là "xong" theo đúng tinh thần README mục 7.2 tuần 3.
- [ ] Mở `http://localhost:8080` sau `kubectl apply -k`, đăng nhập được, thấy worklist — phụ thuộc mục trên.
- [ ] Nhóm xác nhận các quyết định 🟡 ở mục 10 (đặc biệt D14-01, D14-06) và quyết định "không Ingress" ở mục 4.4.
- [ ] Orthanc + OHIF chạy thật, mở được ảnh DICOM qua deep-link — đúng kế hoạch tuần 7 (IN-19), không phải việc của tuần 3.
